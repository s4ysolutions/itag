package s4y.itag;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.TelephonyManager;

import java.io.IOException;

import static android.Manifest.permission.READ_PHONE_STATE;
import static android.content.Context.AUDIO_SERVICE;

public class MediaPlayerUtils implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {
    private final MediaPlayer mPlayer;
    private int mVolumeLevel = -1;

    private MediaPlayerUtils() {
        mPlayer = new MediaPlayer();
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnCompletionListener(this);
    }

    private static MediaPlayerUtils instance;

    public static MediaPlayerUtils getInstance() {
        if (instance == null) {
            instance = new MediaPlayerUtils();
        }
        return instance;
    }

    @Override
    public void onPrepared(MediaPlayer player) {
        player.start();
    }


    @Override
    public void onCompletion(MediaPlayer mp) {
        stopFindingPhones();
    }

    private void stop() {
        mPlayer.stop();
        mPlayer.reset();
    }

    private final Handler vibrateHandler = new Handler(Looper.getMainLooper());


    public void startFindPhone(Context context) {
        AssetFileDescriptor afd = null;
        stopSound(context);
        try {
            afd = context.getAssets().openFd("alarm.mp3");

            AudioManager am = (AudioManager) context.getSystemService(AUDIO_SERVICE);
            if (am != null) {
                mVolumeLevel = am.getStreamVolume(AudioManager.STREAM_ALARM);
                am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
            }

            mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mPlayer.stop();
            mPlayer.reset();
            mPlayer.reset();
            mPlayer.setLooping(false);
            mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mPlayer.prepareAsync();
        } catch (IOException e) {
            ITagApplication.handleError(e, true);
        } finally {
            if (afd != null) {
                try {
                    afd.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private final Runnable vibration = new Runnable() {
        @Override
        public void run() {
            Vibrator v = (Vibrator) ITagApplication.context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null) {
                return;
            }
            // Vibrate for 500 milliseconds
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                //deprecated in API 26
                v.vibrate(500);
            }
            vibrateHandler.postDelayed(this, 1000);
        }
    };

    public boolean isSound() {
        return mPlayer.isPlaying();
    }

    public void startSoundConnectedDisconnected(Context context, Boolean disconnected) {
        stopSound(context);
        // check if we have permission get call state

        if (context.checkSelfPermission(READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            // return if we are in a call
            TelephonyManager manager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
            if(manager.getCallState() != TelephonyManager.CALL_STATE_IDLE){
                return;
            }
        }

        AudioManager am = (AudioManager) context.getSystemService(AUDIO_SERVICE);

        if (am == null)
            return;
        if (am.getMode() != AudioManager.MODE_NORMAL) {
            ITagApplication.faDisconnectDuringCall();
            return;
        }

        AssetFileDescriptor afd = null;
        try {
            afd = context.getAssets().openFd("lost.mp3"); // TODO: different sound on connect and disconnect
            MediaPlayerUtils.getInstance().stop();
            mVolumeLevel = am.getStreamVolume(AudioManager.STREAM_ALARM);
            am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
            // TODO: make duration of alarm configurable
            mPlayer.stop();
            mPlayer.reset();
            mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mPlayer.reset();
            mPlayer.setLooping(true);
            mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mPlayer.prepareAsync();
            // mPlayer.start();
        } catch (IOException e) {
            ITagApplication.handleError(e, true);
        } finally {
            if (afd != null) {
                try {
                    afd.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void stopSound(Context context) {
        if (MediaPlayerUtils.getInstance().isSound()) {
            stopFindingPhones();
            MediaPlayerUtils.getInstance().stop();
            if (mVolumeLevel >= 0) {
                AudioManager am = (AudioManager) context.getSystemService(AUDIO_SERVICE);
                if (am != null) {
                    am.setStreamVolume(AudioManager.STREAM_ALARM, mVolumeLevel, 0);
                    mVolumeLevel = -1;
                }
            }
        }
        stopVibrate();
    }

    private void stopFindingPhones() {
    }

    private void stopVibrate() {
        vibrateHandler.removeCallbacks(vibration);

    }

    public void startVibrate() {
        stopVibrate();
        vibrateHandler.post(vibration);
    }
}
