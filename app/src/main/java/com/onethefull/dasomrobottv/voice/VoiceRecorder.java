package com.onethefull.dasomrobottv.voice;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.util.Log;

import androidx.core.app.ActivityCompat;

/**
 * Continuously records audio and notifies the {@link Callback} when voice (or any
 * sound) is heard.
 * <p>
 * <p>The recorded audio format is always {@link AudioFormat#ENCODING_PCM_16BIT} and
 * {@link AudioFormat#CHANNEL_IN_MONO}. This class will automatically pick the right sample rate
 * for the device. Use {@link #getSampleRate()} to get the selected value.</p>
 */
public class VoiceRecorder {

    private static final int[] SAMPLE_RATE_CANDIDATES = new int[]{16000, 11025, 22050, 44100};
    //    private static int N = AudioRecord.getMinBufferSize(
//            16000,
//            AudioFormat.CHANNEL_IN_MONO,
//            AudioFormat.ENCODING_PCM_16BIT
//    )
    public static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    public static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    public static int CURRENT_SAMPLE_RATE = 16000;
    //    public static int BUFFER_SIZE = 2048;
    public static int BUFFER_SIZE = 4096;

    private static final int AMPLITUDE_THRESHOLD = 1500;
    private static final int AMPLITUDE_THRESHOLD_GENIE = 1675;
    private static final int SPEECH_TIMEOUT_MILLIS = 2000;
    private static final int MAX_SPEECH_LENGTH_MILLIS = 10 * 1000;


    public static abstract class Callback {

        /**
         * Called when the recorder starts hearing voice.
         */
        public void onVoiceStart() {
        }

        /**
         * Called when the recorder is hearing voice.
         *
         * @param data The audio data in {@link AudioFormat#ENCODING_PCM_16BIT}.
         * @param size The size of the actual data in {@code data}.
         */
        public void onVoice(byte[] data, int size) {
        }

        /**
         * Called when the recorder stops hearing voice.
         */
        public void onVoiceEnd() {
        }
    }

    private final Callback mCallback;

    private AudioRecord mAudioRecord;

    private Thread mThread;

    private byte[] mBuffer;

    private final Object mLock = new Object();

    /**
     * The timestamp of the last time that voice is heard.
     */
    private long mLastVoiceHeardMillis = Long.MAX_VALUE;

    /**
     * The timestamp when the current voice is started.
     */
    private long mVoiceStartedMillis;

    public VoiceRecorder(Callback callback) {
        mCallback = callback;
    }

    /**
     * Starts recording audio.
     * <p>
     * <p>The caller is responsible for calling {@link #stop()} later.</p>
     */
    public void start() {
        // Stop recording if it is currently ongoing.
        stop();
        // Try to create a new recording session.
        mAudioRecord = createAudioRecord();

        if (NoiseSuppressor.isAvailable()) {
            NoiseSuppressor.create(mAudioRecord.getAudioSessionId()).setEnabled(true);
        } else {
            Log.e("VoiceRecorder", "Can not use Noise Suppressor");
        }

        if (AcousticEchoCanceler.isAvailable()) {
            AcousticEchoCanceler.create(mAudioRecord.getAudioSessionId()).setEnabled(true);
        } else {
            Log.e("VoiceRecorder", "Can not use AcousticEchoCanceler");
        }

        if (AutomaticGainControl.isAvailable()) {
            AutomaticGainControl.create(mAudioRecord.getAudioSessionId()).setEnabled(true);
        } else {
            Log.e("VoiceRecorder", "Can not use AutomaticGainControl");
        }

        if (mAudioRecord == null) {
            Log.e("VoiceRecorder", "Cannot instantiate VoiceRecorder");
            throw new RuntimeException("Cannot instantiate VoiceRecorder");
        }
        // Start recording.
        mAudioRecord.startRecording();
        // Start processing the captured audio.
        isPause = false;
        isFirstPauseEnd = false;
        mThread = new Thread(new ProcessVoice());
        mThread.start();
    }

    /**
     * Stops recording audio.
     */
    public void stop() {
        dismiss();
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
        mBuffer = null;
    }

    /**
     * Dismisses the currently ongoing utterance.
     */
    public void dismiss() {
        if (mLastVoiceHeardMillis != Long.MAX_VALUE) {
            mLastVoiceHeardMillis = Long.MAX_VALUE;
            mCallback.onVoiceEnd();
        }
    }

    /**
     * Retrieves the sample rate currently used to record audio.
     *
     * @return The sample rate of recorded audio.
     */
    public int getSampleRate() {
        if (mAudioRecord != null) {
            return mAudioRecord.getSampleRate();
        }
        return 0;
    }

    /**
     * Creates a new {@link AudioRecord}.
     *
     * @return A newly created {@link AudioRecord}, or null if it cannot be created (missing
     * permissions?).
     */
    private AudioRecord createAudioRecord() {
        for (int sampleRate : SAMPLE_RATE_CANDIDATES) {

            final int sizeInBytes = AudioRecord.getMinBufferSize(sampleRate, CHANNEL, ENCODING);
            if (sizeInBytes == AudioRecord.ERROR_BAD_VALUE) {
                continue;
            }
            final AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    sampleRate, CHANNEL, ENCODING, sizeInBytes);
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                mBuffer = new byte[sizeInBytes];
                CURRENT_SAMPLE_RATE = sampleRate;
                BUFFER_SIZE = AudioRecord.getMinBufferSize(sampleRate, CHANNEL, ENCODING);
                return audioRecord;
            } else {
                audioRecord.release();
            }
        }
        return null;
    }

    /**
     * Continuously processes the captured audio and notifies {@link #mCallback} of corresponding
     * events.
     */
    private class ProcessVoice implements Runnable {

        @Override
        public void run() {
            while (true) {
                synchronized (mLock) {
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    if (!isPause) {
                        try {
                            final int size = mAudioRecord.read(mBuffer, 0, mBuffer.length);
                            final long now = System.currentTimeMillis();
                            if (isHearingVoice(mBuffer, size)) {
                                if (mLastVoiceHeardMillis == Long.MAX_VALUE) {
                                    mVoiceStartedMillis = now;
                                    mCallback.onVoiceStart();
                                }
                                mCallback.onVoice(mBuffer, size);
                                mLastVoiceHeardMillis = now;
                                if (now - mVoiceStartedMillis > MAX_SPEECH_LENGTH_MILLIS) {
                                    end();
                                }
                            } else if (mLastVoiceHeardMillis != Long.MAX_VALUE) {
                                mCallback.onVoice(mBuffer, size);
                                if (now - mLastVoiceHeardMillis > SPEECH_TIMEOUT_MILLIS) {
                                    end();
                                }
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    } else {
                        if (isFirstPauseEnd) {
                            isFirstPauseEnd = false;
                            end();
                        }
                    }
                }
            }
        }

        private void end() {
            mLastVoiceHeardMillis = Long.MAX_VALUE;
            mCallback.onVoiceEnd();
        }

        private boolean isHearingVoice(byte[] buffer, int size) {
            for (int i = 0; i < size - 1; i += 2) {
                // The buffer has LINEAR16 in little endian.
                int s = buffer[i + 1];
                if (s < 0) s *= -1;
                s <<= 8;
                s += Math.abs(buffer[i]);

                if (s > AMPLITUDE_THRESHOLD_GENIE) {
//                    DWLog.INSTANCE.d("[VOICE_RECORDER] ==> isHearingVoice : " + s);
                    return true;
                }
            }
            return false;
        }
    }

    public void pasue() {
        isPause = true;
        isFirstPauseEnd = true;
    }

    public void resume() {
        isPause = false;
        isFirstPauseEnd = false;
    }

    private volatile static boolean isPause = false;
    private volatile static boolean isFirstPauseEnd = false;

}
