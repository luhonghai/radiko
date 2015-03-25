package com.gmail.radioserver2.activity;

import android.content.res.Configuration;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.crashlytics.android.Crashlytics;
import com.gmail.radioserver2.service.CustomUncaughtExceptionHandler;
import com.gmail.radioserver2.utils.AndroidUtil;

import java.util.Locale;

import io.fabric.sdk.android.Fabric;

/**
 * Created by luhonghai on 2/16/15.
 */

public class BaseFragmentActivity extends SherlockFragmentActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // AndroidUtil.updateLanguage(this);
        Thread.setDefaultUncaughtExceptionHandler(new CustomUncaughtExceptionHandler());
        Fabric.with(this, new Crashlytics());
    }
}
