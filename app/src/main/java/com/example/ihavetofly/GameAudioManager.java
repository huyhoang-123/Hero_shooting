package com.example.ihavetofly;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;

public class GameAudioManager {
    private static GameAudioManager instance;
    private MediaPlayer backgroundMusic;
    private SoundPool soundPool;
    private int shootSound;
    private Context context;
    private SharedPreferences prefs;
    private boolean isMuted = false;
    private boolean isMusicMuted = false; // Riêng cho nhạc nền
    private float musicVolume = 0.5f;
    private float effectVolume = 0.7f;

    private GameAudioManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE);
        this.isMuted = prefs.getBoolean("isMute", false); // Sound effects
        this.isMusicMuted = prefs.getBoolean("isMusicMute", false); // Background music
        initializeAudio();
    }

    public static GameAudioManager getInstance(Context context) {
        if (instance == null) {
            instance = new GameAudioManager(context);
        }
        return instance;
    }

    private void initializeAudio() {
        // Initialize background music
        try {
            backgroundMusic = MediaPlayer.create(context, R.raw.background_sound);
            if (backgroundMusic != null) {
                backgroundMusic.setLooping(true);
                backgroundMusic.setVolume(isMuted ? 0 : musicVolume, isMuted ? 0 : musicVolume);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize SoundPool for sound effects
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(5)
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            soundPool = new SoundPool(5, android.media.AudioManager.STREAM_MUSIC, 0);
        }

        // Load shoot sound
        shootSound = soundPool.load(context, R.raw.shoot, 1);
    }

    public void startBackgroundMusic() {
        if (backgroundMusic != null && !isMusicMuted) {
            try {
                if (!backgroundMusic.isPlaying()) {
                    backgroundMusic.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void pauseBackgroundMusic() {
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.pause();
        }
    }

    public void resumeBackgroundMusic() {
        if (backgroundMusic != null && !isMusicMuted) {
            try {
                backgroundMusic.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void playShootSound() {
        if (soundPool != null && !isMuted) {
            soundPool.play(shootSound, effectVolume, effectVolume, 0, 0, 1);
        }
    }

    public void toggleMute() {
        isMuted = !isMuted;

        // Save to preferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isMute", isMuted);
        editor.apply();

        // Update background music volume
        if (backgroundMusic != null) {
            float volume = isMusicMuted ? 0 : musicVolume;
            backgroundMusic.setVolume(volume, volume);

            if (isMusicMuted) {
                pauseBackgroundMusic();
            } else {
                resumeBackgroundMusic();
            }
        }
    }

    public void toggleSoundEffects() {
        isMuted = !isMuted;

        // Save to preferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isMute", isMuted);
        editor.apply();
    }

    public void toggleMusic() {
        isMusicMuted = !isMusicMuted;

        // Save to preferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isMusicMute", isMusicMuted);
        editor.apply();

        // Update background music volume
        if (backgroundMusic != null) {
            float volume = isMusicMuted ? 0 : musicVolume;
            backgroundMusic.setVolume(volume, volume);

            if (isMusicMuted) {
                pauseBackgroundMusic();
            } else {
                resumeBackgroundMusic();
            }
        }
    }

    public void toggleAll() {
        boolean newMuteState = !(isMuted && isMusicMuted);
        isMuted = newMuteState;
        isMusicMuted = newMuteState;

        // Save to preferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isMute", isMuted);
        editor.putBoolean("isMusicMute", isMusicMuted);
        editor.apply();
    }

    public boolean isMuted() {
        return isMuted; // Sound effects mute state
    }

    public boolean isMusicMuted() {
        return isMusicMuted; // Music mute state
    }

    public void cleanup() {
        if (backgroundMusic != null) {
            if (backgroundMusic.isPlaying()) {
                backgroundMusic.stop();
            }
            backgroundMusic.release();
            backgroundMusic = null;
        }

        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    public void setMusicVolume(float volume) {
        this.musicVolume = Math.max(0, Math.min(1, volume));
        if (backgroundMusic != null && !isMuted) {
            backgroundMusic.setVolume(musicVolume, musicVolume);
        }
    }

    public void setEffectVolume(float volume) {
        this.effectVolume = Math.max(0, Math.min(1, volume));
    }
}