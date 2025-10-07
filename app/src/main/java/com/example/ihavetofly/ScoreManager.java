package com.example.ihavetofly;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class ScoreManager {

    private final SharedPreferences prefs;
    private List<ScoreEntry> highScores;
    private int currentLevel;

    public static class ScoreEntry {
        public int score;
        public long time;

        public ScoreEntry(int score, long time) {
            this.score = score;
            this.time = time;
        }
    }

    public ScoreManager(SharedPreferences prefs, int level) {
        this.prefs = prefs;
        this.currentLevel = level;
        loadHighScores();
    }

    public void setLevel(int level) {
        this.currentLevel = level;
        loadHighScores();
    }

    private String getHighScoreKey() {
        return "high_score_level_" + currentLevel;
    }

    private String getHighScoreTimeKey() {
        return "high_score_time_level_" + currentLevel;
    }

    private String getHighScoresKey() {
        return "high_scores_level_" + currentLevel;
    }

    public int getSessionStartHighScore() {
        return prefs.getInt(getHighScoreKey(), 0);
    }

    public void saveHighScores(int score, long timeInSeconds) {
        int currentHighScore = prefs.getInt(getHighScoreKey(), 0);
        long currentHighTime = prefs.getLong(getHighScoreTimeKey(), Long.MAX_VALUE);

        boolean isNewHigh = false;
        if (score > currentHighScore) {
            isNewHigh = true;
        } else if (score == currentHighScore && timeInSeconds < currentHighTime) {
            isNewHigh = true;
        }

        if (isNewHigh) {
            prefs.edit()
                    .putInt(getHighScoreKey(), score)
                    .putLong(getHighScoreTimeKey(), timeInSeconds)
                    .apply();
        }

        String scoresStr = prefs.getString(getHighScoresKey(), "");
        List<ScoreEntry> list = new ArrayList<>();
        if (!scoresStr.isEmpty()) {
            String[] entries = scoresStr.split(";");
            for (String entry : entries) {
                String[] parts = entry.split(",");
                if (parts.length == 2) {
                    list.add(new ScoreEntry(Integer.parseInt(parts[0]), Long.parseLong(parts[1])));
                }
            }
        }
        list.add(new ScoreEntry(score, timeInSeconds));

        list.sort((a, b) -> {
            if (a.score != b.score) {
                return b.score - a.score;
            }
            return Long.compare(a.time, b.time);
        });

        if (list.size() > 6) list = list.subList(0, 6);

        highScores = new ArrayList<>(list);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(";");
            sb.append(list.get(i).score).append(",").append(list.get(i).time);
        }
        prefs.edit().putString(getHighScoresKey(), sb.toString()).apply();
    }

    public void clearAllHighScores() {
        prefs.edit()
                .remove(getHighScoreKey())
                .remove(getHighScoreTimeKey())
                .remove(getHighScoresKey())
                .apply();
        highScores = new ArrayList<>();
    }

    public void loadHighScores() {
        String scoresStr = prefs.getString(getHighScoresKey(), "");
        highScores = new ArrayList<>();
        if (!scoresStr.isEmpty()) {
            String[] entries = scoresStr.split(";");
            for (String entry : entries) {
                String[] parts = entry.split(",");
                if (parts.length == 2) {
                    highScores.add(new ScoreEntry(Integer.parseInt(parts[0]), Long.parseLong(parts[1])));
                }
            }
        }
    }

    public List<Integer> getHighScores() {
        List<Integer> scores = new ArrayList<>();
        for (ScoreEntry entry : highScores) {
            scores.add(entry.score);
        }
        return scores;
    }

    public List<ScoreEntry> getHighScoresWithTime() {
        return highScores;
    }
}