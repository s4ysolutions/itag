package s4y.itag;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;

import java.io.IOException;
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

    public void startSoundDisconnected(Context context) {
        stopSound(context);
        AudioManager am = (AudioManager) context.getSystemService(AUDIO_SERVICE);

        if (am == null)
            return;
        if(am.getMode()!=AudioManager.MODE_NORMAL) {
            ITagApplication.faDisconnectDuringCall();
            return;
        }

        AssetFileDescriptor afd = null;
        try {
            afd = context.getAssets().openFd("lost.mp3");

            if (am != null) {
                mVolumeLevel = am.getStreamVolume(AudioManager.STREAM_ALARM);
                am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
            }

            mPlayer.stop();
            mPlayer.reset();
            mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mPlayer.reset();
            mPlayer.setLooping(true);
            mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mPlayer.prepareAsync();
            //           mPlayer.start();
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

    public void stopSound(Context context) {
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

    public boolean isSound(String addr) {
        return mPlayer.isPlaying();
    }

    public boolean isSound() {
        return mPlayer.isPlaying();
    }

    private void stopFindingPhones(){
    };
}
