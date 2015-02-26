package com.gmail.radioserver2.activity;

import android.content.res.Configuration;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import java.util.Locale;

/**
 * Created by luhonghai on 2/16/15.
 */

public class BaseFragmentActivity extends SherlockFragmentActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration c = new Configuration(getResources().getConfiguration());
        c.locale = Locale.JAPANESE;
        getResources().updateConfiguration(c, getResources().getDisplayMetrics());
    }
}
