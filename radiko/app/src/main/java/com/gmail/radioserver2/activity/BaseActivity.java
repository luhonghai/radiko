package com.gmail.radioserver2.activity;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockActivity;
import com.crashlytics.android.Crashlytics;
import com.gmail.radioserver2.service.CustomUncaughtExceptionHandler;
import com.gmail.radioserver2.utils.AndroidUtil;


import io.fabric.sdk.android.Fabric;

/**
 * Created by luhonghai on 2/21/15.
 */
public class BaseActivity extends SherlockActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //AndroidUtil.updateLanguage(this);
        Thread.setDefaultUncaughtExceptionHandler(new CustomUncaughtExceptionHandler());
        Fabric.with(this, new Crashlytics());
    }
}
