package com.example.ihavetofly;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HighScoreManager {

    private static final String PREF_NAME = "high_scores";
    private static final String KEY_SCORE_PREFIX = "score_";
    private static final int MAX_TOP = 3;

    private SharedPreferences prefs;

    public HighScoreManager(Context context){
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Thêm score mới
    public void addScore(long timeInSeconds){
        List<Long> topScores = getTopScores();
        topScores.add(timeInSeconds);
        Collections.sort(topScores); // thời gian càng nhỏ càng tốt
        if(topScores.size() > MAX_TOP) topScores = topScores.subList(0, MAX_TOP);

        SharedPreferences.Editor editor = prefs.edit();
        for(int i=0;i<topScores.size();i++){
            editor.putLong(KEY_SCORE_PREFIX + i, topScores.get(i));
        }
        editor.apply();
    }

    // Lấy danh sách top score (dưới 3 phần tử)
    public List<Long> getTopScores(){
        List<Long> list = new ArrayList<>();
        for(int i=0;i<MAX_TOP;i++){
            long t = prefs.getLong(KEY_SCORE_PREFIX + i, Long.MAX_VALUE);
            if(t != Long.MAX_VALUE) list.add(t);
        }
        return list;
    }

    // Xóa tất cả (nếu cần reset)
    public void clearScores(){
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }
}
