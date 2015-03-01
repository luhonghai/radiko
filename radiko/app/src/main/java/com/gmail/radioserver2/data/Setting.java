package com.gmail.radioserver2.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by luhonghai on 2/28/15.
 */
public class Setting {

    private static final String DEFAULT_SHARED_PREFERENCE = "setting";

    private static final String KEY_SLOW = "slow";
    private static final String KEY_FAST = "fast";
    private static final String KEY_BACK = "back";
    private static final String KEY_REGION = "region";

    public static final float MAX_SLOW_LEVEL = -0.3f;

    public static final float MIN_SLOW_LEVEL = -0.9f;

    public static final float MAX_FAST_LEVEL = 2.0f;

    public static final float MIN_FAST_LEVEL = 1.3f;

    public static final float MAX_BACK_LENGTH = 3.0f;

    public static final float MIN_BACK_LENGTH = 10.0f;

    private final Context context;

    private float slowLevel = MIN_SLOW_LEVEL;

    private float fastLevel = MIN_FAST_LEVEL;

    private float backLength = MIN_BACK_LENGTH;

    private boolean isRegion = true;

    public Setting(Context context) {
        this.context = context;
    }

    public void load() {
        SharedPreferences preferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCE, Context.MODE_PRIVATE);
        this.slowLevel = preferences.getFloat(KEY_SLOW, MIN_SLOW_LEVEL);
        this.fastLevel = preferences.getFloat(KEY_FAST, MIN_FAST_LEVEL);
        this.backLength = preferences.getFloat(KEY_BACK, MIN_BACK_LENGTH);
        this.isRegion = preferences.getBoolean(KEY_REGION, true);
    }

    public void save() {
        SharedPreferences preferences = context.getSharedPreferences(DEFAULT_SHARED_PREFERENCE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat(KEY_SLOW, slowLevel);
        editor.putFloat(KEY_FAST, fastLevel);
        editor.putFloat(KEY_BACK, backLength);
        editor.putBoolean(KEY_REGION, isRegion);
        editor.apply();
    }

    public void applyBackLength(int percent) {
        backLength = MIN_BACK_LENGTH - ((MIN_BACK_LENGTH - MAX_BACK_LENGTH) * percent) / 100 ;
    }

    public int getBackLengthPercent() {
        return Math.abs(Math.round(100 * (MIN_BACK_LENGTH - backLength) / (MIN_BACK_LENGTH - MAX_BACK_LENGTH)));
    }

    public void applyFastLevel(int percent) {
        fastLevel =  ((MAX_FAST_LEVEL - MIN_FAST_LEVEL) * percent) / 100 + MIN_FAST_LEVEL;
    }

    public int getFastLevelPercent() {
        return Math.abs(Math.round(100 * (fastLevel - MIN_FAST_LEVEL) / (MAX_FAST_LEVEL - MIN_FAST_LEVEL)));
    }

    public void applySlowLevel(int percent) {
        slowLevel = - ((MAX_SLOW_LEVEL - MIN_SLOW_LEVEL) * percent) / 100 + MIN_SLOW_LEVEL;
    }

    public int getSlowLevelPercent() {
        return Math.abs(Math.round(100 * (slowLevel - MIN_SLOW_LEVEL) / (MAX_SLOW_LEVEL - MIN_SLOW_LEVEL)));
    }

    public float getSlowLevel() {
        return slowLevel;
    }

    public void setSlowLevel(float slowLevel) {
        this.slowLevel = slowLevel;
    }

    public float getFastLevel() {
        return fastLevel;
    }

    public void setFastLevel(float fastLevel) {
        this.fastLevel = fastLevel;
    }

    public float getBackLength() {
        return backLength;
    }

    public void setBackLength(float backLength) {
        this.backLength = backLength;
    }

    public boolean isRegion() {
        return isRegion;
    }

    public void setRegion(boolean isRegion) {
        this.isRegion = isRegion;
    }
}
