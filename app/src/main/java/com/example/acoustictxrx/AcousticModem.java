package com.example.acoustictxrx;

import android.Manifest;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class AcousticModem {

    // --- FINAL ARCHITECTURE PARAMETERS ---
    public static final int SAMPLE_RATE = 44100;
    private static final double FREQ0 = 2000.0; // Frequency for bit '0'
    private static final double FREQ1 = 3000.0; // Frequency for bit '1'
    private static final double PREAMBLE_FREQ = 4000.0; // A distinct frequency for synchronization
    private static final double END_PREAMBLE_FREQ = 5000.0; // A new frequency to signal the end of the message
    private static final int SYMBOL_MS = 40; // Symbol duration for reliability
    private static final double AMP = 2.0; // Maximum amplitude
    // --- END OF PARAMETERS ---

    private static final int SAMPLES_PER_SYMBOL = (SAMPLE_RATE * SYMBOL_MS) / 1000;
    private static final int PREAMBLE_MS = 250;
    private static final int END_PREAMBLE_MS = 100; // Duration for the new end preamble
    private static final int SILENCE_MS = 50;

    private static final float[] idealSin0;
    private static final float[] idealCos0;
    private static final float[] idealSin1;
    private static final float[] idealCos1;

    private static double phase = 0.0;

    // Static initializer block to create the quadrature waves
    static {
        idealSin0 = new float[SAMPLES_PER_SYMBOL];
        idealCos0 = new float[SAMPLES_PER_SYMBOL];
        idealSin1 = new float[SAMPLES_PER_SYMBOL];
        idealCos1 = new float[SAMPLES_PER_SYMBOL];
        for (int i = 0; i < SAMPLES_PER_SYMBOL; i++) {
            idealSin0[i] = (float) Math.sin(2.0 * Math.PI * FREQ0 * i / SAMPLE_RATE);
            idealCos0[i] = (float) Math.cos(2.0 * Math.PI * FREQ0 * i / SAMPLE_RATE);
            idealSin1[i] = (float) Math.sin(2.0 * Math.PI * FREQ1 * i / SAMPLE_RATE);
            idealCos1[i] = (float) Math.cos(2.0 * Math.PI * FREQ1 * i / SAMPLE_RATE);
        }
    }

    // Private constructor to prevent instantiation
    private AcousticModem() {}

    public static short[] buildFramePcmFromText(String text) {
        phase = 0.0;
        byte[] payload = text.getBytes(StandardCharsets.ISO_8859_1);
        byte[] lengthBytes = new byte[]{ (byte)((payload.length >> 8) & 0xFF), (byte)(payload.length & 0xFF) };
        int crc = crc16Ccitt(concatenateByteArrays(lengthBytes, payload));
        byte[] crcBytes = new byte[]{ (byte)((crc >> 8) & 0xFF), (byte)(crc & 0xFF) };
        List<Integer> bits = bytesToBits(concatenateByteArrays(concatenateByteArrays(lengthBytes, payload), crcBytes));
        return composePcm(bits);
    }

    public static void playPcm(short[] pcm) {
        if (pcm.length == 0) return;
        AudioTrack track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build())
                .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(pcm.length * 2)
                .build();
        try {
            track.play();
            track.write(pcm, 0, pcm.length);
            long durationMs = (long) ((double) pcm.length / SAMPLE_RATE * 1000);
            Thread.sleep(durationMs + 200); // Replaces Kotlin's delay()
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            track.stop();
            track.release();
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public static String receiveOneMessageBlocking() {
        return receiveOneMessageBlocking(30);
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @Nullable
    public static String receiveOneMessageBlocking(int maxSeconds) {
        final String TAG = "AcousticModemDebug";
        Log.d(TAG, "--- Starting new reception attempt ---");
        int minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf * 4);
        long deadlineNs = System.nanoTime() + maxSeconds * 1_000_000_000L;

        recorder.startRecording();
        try {
            Log.d(TAG, "1. Listening for start preamble tone...");
            if (!findPreamble(recorder, deadlineNs)) {
                Log.e(TAG, "FAIL: Start preamble not found. Timed out.");
                return null;
            }
            Log.d(TAG, "SUCCESS: Start preamble found!");

            Log.d(TAG, "2. Recording message data until end preamble is detected...");
            short[] messagePcm = recordUntilEndPreamble(recorder, deadlineNs);
            if (messagePcm == null) {
                Log.e(TAG, "FAIL: Timed out waiting for end preamble.");
                return null;
            }
            Log.d(TAG, "SUCCESS: End preamble found. Recorded " + messagePcm.length + " samples.");

            Log.d(TAG, "3. Demodulating recorded audio...");
            List<Integer> allBits = demodulatePcmToBits(messagePcm);
            if (allBits == null || allBits.size() < 32) {
                Log.e(TAG, "FAIL: Not enough data bits decoded from audio. Got " + (allBits == null ? 0 : allBits.size()) + " bits.");
                return null;
            }
            Log.d(TAG, "-> Decoded " + allBits.size() + " total bits.");

            Log.d(TAG, "4. Decoding message length (16 bits)...");
            List<Integer> lengthBits = allBits.subList(0, 16);
            int length = ((bitsToByte(lengthBits.subList(0, 8)) & 0xFF) << 8) | (bitsToByte(lengthBits.subList(8, 16)) & 0xFF);
            Log.d(TAG, "-> Decoded Length: " + length + " bytes");

            if (length <= 0 || length > 8192) {
                Log.e(TAG, "FAIL: Decoded length (" + length + ") is invalid.");
                return null;
            }

            Log.d(TAG, "5. Decoding payload and CRC...");
            int payloadBitsCount = length * 8;
            int crcBitsCount = 16;
            int requiredBits = 16 + payloadBitsCount + crcBitsCount;
            if (allBits.size() < requiredBits) {
                Log.e(TAG, "FAIL: Not enough bits for payload and CRC. Expected " + requiredBits + ", got " + allBits.size());
                return null;
            }

            List<Integer> payloadBits = allBits.subList(16, 16 + payloadBitsCount);
            List<Integer> crcBits = allBits.subList(16 + payloadBitsCount, 16 + payloadBitsCount + crcBitsCount);
            byte[] payloadBytes = bitsToBytes(payloadBits);
            int crcRx = ((bitsToByte(crcBits.subList(0, 8)) & 0xFF) << 8) | (bitsToByte(crcBits.subList(8, 16)) & 0xFF);

            byte[] lengthAsBytes = new byte[]{ (byte)((length >> 8) & 0xFF), (byte)(length & 0xFF) };
            int crcCalc = crc16Ccitt(concatenateByteArrays(lengthAsBytes, payloadBytes));
            Log.d(TAG, "-> Received CRC: " + crcRx + " vs Calculated CRC: " + crcCalc);

            if (crcCalc != crcRx) {
                Log.e(TAG, "FAIL: CRC MISMATCH! Data is corrupted.");
                return null;
            }
            Log.d(TAG, "SUCCESS: CRC Matched!");

            return new String(payloadBytes, StandardCharsets.ISO_8859_1);

        } finally {
            Log.d(TAG, "--- Stopping reception attempt ---");
            try { recorder.stop(); } catch (Exception ignored) {}
            recorder.release();
        }
    }

    private static short[] recordUntilEndPreamble(AudioRecord recorder, long deadlineNs) {
        final String TAG = "AcousticModemDebug";
        List<Short> messageAudio = new ArrayList<>();
        int bufferSize = msToSamples(END_PREAMBLE_MS * 0.5);
        short[] buffer = new short[bufferSize];
        double endPreambleThreshold = 2E12;

        while (System.nanoTime() < deadlineNs) {
            int read = recorder.read(buffer, 0, buffer.length);
            if (read <= 0) continue;

            for (int i = 0; i < read; i++) {
                messageAudio.add(buffer[i]);
            }

            double power = goertzelPower(buffer, 0, read, END_PREAMBLE_FREQ);
            if (power > endPreambleThreshold) {
                Log.d(TAG, "End preamble detected. Finalizing recording.");
                return toShortArray(messageAudio);
            }
        }

        Log.w(TAG, "Timed out waiting for end preamble.");
        if (messageAudio.isEmpty()) return null;
        return toShortArray(messageAudio);
    }

    private static List<Integer> demodulatePcmToBits(short[] pcm) {
        final String TAG = "AcousticModemDebug";
        if (pcm.length < SAMPLES_PER_SYMBOL) return null;

        int searchDurationSamples = Math.min(pcm.length - SAMPLES_PER_SYMBOL, msToSamples(SYMBOL_MS));
        if (searchDurationSamples <= 0) return null;

        int bestOffset = 0;
        float maxEnergy = -1.0f;
        float[] floatWindow = new float[SAMPLES_PER_SYMBOL];

        for (int offset = 0; offset < searchDurationSamples; offset += 4) {
            for (int i = 0; i < SAMPLES_PER_SYMBOL; i++) {
                floatWindow[i] = pcm[offset + i] / 32767.0f;
            }

            float corrSin0 = correlate(floatWindow, idealSin0);
            float corrCos0 = correlate(floatWindow, idealCos0);
            float corrSin1 = correlate(floatWindow, idealSin1);
            float corrCos1 = correlate(floatWindow, idealCos1);

            float energy0 = (float) Math.sqrt(corrSin0 * corrSin0 + corrCos0 * corrCos0);
            float energy1 = (float) Math.sqrt(corrSin1 * corrSin1 + corrCos1 * corrCos1);

            float currentMaxEnergy = Math.max(energy0, energy1);
            if (currentMaxEnergy > maxEnergy) {
                maxEnergy = currentMaxEnergy;
                bestOffset = offset;
            }
        }
        Log.d(TAG, "Sync search complete. Locked on to best starting offset at sample " + bestOffset);

        List<Integer> bits = new ArrayList<>();
        int currentOffset = bestOffset;
        while (currentOffset + SAMPLES_PER_SYMBOL <= pcm.length) {
            for (int i = 0; i < SAMPLES_PER_SYMBOL; i++) {
                floatWindow[i] = pcm[currentOffset + i] / 32767.0f;
            }
            float corrSin0 = correlate(floatWindow, idealSin0);
            float corrCos0 = correlate(floatWindow, idealCos0);
            float corrSin1 = correlate(floatWindow, idealSin1);
            float corrCos1 = correlate(floatWindow, idealCos1);

            float energy0 = (float) Math.sqrt(corrSin0 * corrSin0 + corrCos0 * corrCos0);
            float energy1 = (float) Math.sqrt(corrSin1 * corrSin1 + corrCos1 * corrCos1);

            bits.add(energy1 > energy0 ? 1 : 0);
            currentOffset += SAMPLES_PER_SYMBOL;
        }
        return bits;
    }

    private static boolean findPreamble(AudioRecord recorder, long deadlineNs) {
        int windowSize = msToSamples(PREAMBLE_MS * 0.5);
        short[] shortBuf = new short[windowSize];
        double preambleThreshold = 2E12;

        while (System.nanoTime() < deadlineNs) {
            int read = recorder.read(shortBuf, 0, windowSize);
            if (read != windowSize) continue;
            double power = goertzelPower(shortBuf, 0, windowSize, PREAMBLE_FREQ);
            if (power > preambleThreshold) {
                int samplesToDiscard = msToSamples(PREAMBLE_MS * 0.5 + SILENCE_MS);
                discardSamples(recorder, samplesToDiscard);
                return true;
            }
        }
        return false;
    }

    private static double goertzelPower(short[] buf, int offset, int n, double freq) {
        int k = (int) (0.5 + (n * freq) / SAMPLE_RATE);
        double w = (2.0 * Math.PI * k) / n;
        double cosine = Math.cos(w);
        double coeff = 2.0 * cosine;
        double s0;
        double s1 = 0.0;
        double s2 = 0.0;
        for (int i = 0; i < n; i++) {
            s0 = coeff * s1 - s2 + buf[offset + i];
            s2 = s1;
            s1 = s0;
        }
        return s1 * s1 + s2 * s2 - coeff * s1 * s2;
    }

    private static float correlate(float[] a, float[] b) {
        float sum = 0.0f;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    private static float[] bitsToFskPcm(List<Integer> bits) {
        float[] samples = new float[bits.size() * SAMPLES_PER_SYMBOL];
        double twoPi = 2.0 * Math.PI;
        int currentSample = 0;
        for (int b : bits) {
            double f = (b == 0) ? FREQ0 : FREQ1;
            for (int i = 0; i < SAMPLES_PER_SYMBOL; i++) {
                samples[currentSample++] = (float) (Math.sin(phase + twoPi * f * i / SAMPLE_RATE) * AMP);
            }
            phase += twoPi * f * SAMPLES_PER_SYMBOL / SAMPLE_RATE;
        }
        return samples;
    }

    private static float[] tone(double freq, int ms) {
        int nSamples = msToSamples(ms);
        float[] out = new float[nSamples];
        if (freq <= 0.0) return out;
        double twoPi = 2.0 * Math.PI;
        for (int i = 0; i < nSamples; i++) {
            out[i] = (float) (Math.sin(phase + twoPi * freq * i / SAMPLE_RATE) * AMP);
        }
        phase += twoPi * freq * nSamples / SAMPLE_RATE;
        return out;
    }

    private static short[] composePcm(List<Integer> bits) {
        float[] preambleTone = tone(PREAMBLE_FREQ, PREAMBLE_MS);
        float[] silence = tone(0.0, SILENCE_MS);
        float[] dataPcm = bitsToFskPcm(bits);
        float[] endPreambleTone = tone(END_PREAMBLE_FREQ, END_PREAMBLE_MS);

        float[] all = concatenateFloatArrays(preambleTone, silence, dataPcm, endPreambleTone);
        short[] out = new short[all.length];
        for (int i = 0; i < all.length; i++) {
            out[i] = (short) Math.max(-32767, Math.min(32767, (int) (all[i] * 32767.0f)));
        }
        return out;
    }

    private static int msToSamples(double ms) {
        return (int) (SAMPLE_RATE * ms / 1000.0);
    }

    private static List<Integer> bytesToBits(byte[] bytes) {
        List<Integer> bits = new ArrayList<>(bytes.length * 8);
        for (byte b : bytes) {
            for (int i = 7; i >= 0; i--) {
                bits.add((b >> i) & 1);
            }
        }
        return bits;
    }

    private static byte[] bitsToBytes(List<Integer> bits) {
        byte[] out = new byte[bits.size() / 8];
        for (int i = 0; i < out.length; i++) {
            out[i] = bitsToByte(bits.subList(i * 8, i * 8 + 8));
        }
        return out;
    }

    private static byte bitsToByte(List<Integer> bits) {
        int v = 0;
        for (int i = 0; i < 8; i++) {
            v = (v << 1) | (bits.get(i) & 1);
        }
        return (byte) v;
    }

    private static int crc16Ccitt(byte[] data) {
        return crc16Ccitt(data, 0xFFFF, 0x1021);
    }
    private static int crc16Ccitt(byte[] data, int init, int poly) {
        int crc = init;
        for (byte b : data) {
            int x = (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                boolean bit = ((crc ^ x) & 0x8000) != 0;
                crc = (crc << 1) & 0xFFFF;
                if (bit) crc ^= poly;
                x = (x << 1) & 0xFFFF;
            }
        }
        return crc & 0xFFFF;
    }

    private static void discardSamples(AudioRecord rec, int samples) {
        if (samples <= 0) return;
        short[] tmp = new short[Math.min(samples, 4096)];
        int left = samples;
        while (left > 0) {
            int n = rec.read(tmp, 0, Math.min(tmp.length, left));
            if (n <= 0) break;
            left -= n;
        }
    }

    // --- Helper Methods ---
    private static byte[] concatenateByteArrays(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private static float[] concatenateFloatArrays(float[]... arrays) {
        int totalLength = 0;
        for (float[] arr : arrays) {
            totalLength += arr.length;
        }
        float[] result = new float[totalLength];
        int currentPos = 0;
        for (float[] arr : arrays) {
            System.arraycopy(arr, 0, result, currentPos, arr.length);
            currentPos += arr.length;
        }
        return result;
    }

    private static short[] toShortArray(List<Short> list) {
        short[] array = new short[list.size()];
        for(int i = 0; i < list.size(); i++) array[i] = list.get(i);
        return array;
    }
}