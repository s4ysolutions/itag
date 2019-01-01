package solutions.s4y.itag.ble;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import solutions.s4y.itag.ITagApplication;

import static android.content.Context.AUDIO_SERVICE;

public class MediaPlayerUtils implements MediaPlayer.OnPreparedListener {
    private final MediaPlayer mPlayer = new MediaPlayer();
    private int mVolumeLevel = -1;

    @NonNull
    private
    Set<String> mSoundingITags = new HashSet<>(4);


    private MediaPlayerUtils() {
        mPlayer.setOnPreparedListener(this);
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

    private void stop() {
        mPlayer.stop();
        mPlayer.reset();
    }

    public void startSoundDisconnected(Context context, String addr) {
        stopSound(context);
        AssetFileDescriptor afd = null;
        try {
            afd = context.getAssets().openFd("lost.mp3");

            AudioManager am = (AudioManager) context.getSystemService(AUDIO_SERVICE);
            if (am != null) {
                mVolumeLevel = am.getStreamVolume(AudioManager.STREAM_ALARM);
                am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
            }

            mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mPlayer.reset();
            mPlayer.setLooping(true);
            mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mPlayer.prepareAsync();
            //           mPlayer.start();
            mSoundingITags.add(addr);
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


    void startFindPhone(Context context, String addr) {
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
            mPlayer.reset();
            mPlayer.setLooping(false);
            mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mPlayer.prepareAsync();
            mSoundingITags.add(addr);
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
        mSoundingITags.clear();
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
        return mPlayer.isPlaying() && mSoundingITags.contains(addr);
    }

    public boolean isSound() {
        return mPlayer.isPlaying();
    }

}
