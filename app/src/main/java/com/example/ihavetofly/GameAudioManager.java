package com.example.ihavetofly;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

public class GameAudioManager {

    private static GameAudioManager instance;

    private MediaPlayer backgroundPlayer;
    private SoundPool soundPool;
    private int shootSoundId;

    private boolean musicMuted = false;
    private boolean allMuted = false;
    private boolean isReady = false;

    private GameAudioManager(Context context) {
        initSoundPool(context);
        initBackgroundMusic(context);
    }

    public static GameAudioManager getInstance(Context context) {
        if (instance == null) {
            instance = new GameAudioManager(context.getApplicationContext());
        }
        return instance;
    }

    private void initSoundPool(Context context) {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(attrs)
                .build();

        shootSoundId = soundPool.load(context, R.raw.shoot, 1);
    }

    private void initBackgroundMusic(Context context) {
        backgroundPlayer = MediaPlayer.create(context, R.raw.background_sound);
        if (backgroundPlayer != null) {
            backgroundPlayer.setLooping(true);
            backgroundPlayer.setVolume(0.7f, 0.7f); // Giảm volume một chút

            // Set listener để biết khi nào ready
            backgroundPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    isReady = true;
                    // Auto-start background music khi ready
                    if (!musicMuted && !allMuted) {
                        mp.start();
                    }
                }
            });

            backgroundPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    // Log error nếu cần
                    return false;
                }
            });
        }
    }

    public void playShootSound() {
        if (!allMuted && soundPool != null) {
            soundPool.play(shootSoundId, 1f, 1f, 1, 0, 1f);
        }
    }

    public void prepare() {
        // Đảm bảo background music được prepare và start
        if (backgroundPlayer != null && !backgroundPlayer.isPlaying() && isReady) {
            playBackground();
        }
    }

    public void playBackground() {
        if (!musicMuted && !allMuted && backgroundPlayer != null && isReady && !backgroundPlayer.isPlaying()) {
            try {
                backgroundPlayer.start();
            } catch (IllegalStateException e) {
                // Handle case where MediaPlayer is in wrong state
                e.printStackTrace();
            }
        }
    }

    public void pauseBackground() {
        if (backgroundPlayer != null && backgroundPlayer.isPlaying()) {
            try {
                backgroundPlayer.pause();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    public void setMusicMuted(boolean muted) {
        musicMuted = muted;
        if (muted || allMuted) {
            pauseBackground();
        } else {
            playBackground();
        }
    }

    public void setAllMuted(boolean muted) {
        allMuted = muted;
        if (muted) {
            pauseBackground();
        } else if (!musicMuted) {
            playBackground();
        }
    }

    public boolean isMusicMuted() { return musicMuted; }
    public boolean isAllMuted() { return allMuted; }
    public boolean isReady() { return isReady; }

    public void release() {
        if (backgroundPlayer != null) {
            try {
                if (backgroundPlayer.isPlaying()) {
                    backgroundPlayer.stop();
                }
                backgroundPlayer.release();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            backgroundPlayer = null;
        }
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        isReady = false;
        instance = null;
    }
}