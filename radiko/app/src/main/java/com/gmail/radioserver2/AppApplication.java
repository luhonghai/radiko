package com.gmail.radioserver2;

/*Copyright*/

import android.app.Application;
import android.content.SharedPreferences;
import android.media.AudioManager;

import com.gmail.radioserver2.utils.AppDelegate;
import com.gmail.radioserver2.utils.Constants;

public class AppApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.SHARE_PREF, MODE_PRIVATE);
        int defaultVolume = sharedPreferences.getInt(Constants.KEY_DEFAULT_VOLUME, -1);
        if (defaultVolume == -1) {
            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(Constants.KEY_DEFAULT_VOLUME, maxVolume);
            editor.apply();
        }
        AppDelegate.getInstance().setUserName(sharedPreferences.getString(Constants.KEY_USERNAME, ""));
        AppDelegate.getInstance().setPassword(sharedPreferences.getString(Constants.KEY_PASSWORD, ""));
        AppDelegate.getInstance().setPremium(sharedPreferences.getBoolean(Constants.KEY_PREMIUM, false));
    }
}
