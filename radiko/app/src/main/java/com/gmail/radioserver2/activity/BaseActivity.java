package com.gmail.radioserver2.activity;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockActivity;
import com.crashlytics.android.Crashlytics;
import com.gmail.radioserver2.analytic.AnalyticHelper;
import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;


import io.fabric.sdk.android.Fabric;

/**
 * Created by luhonghai on 2/21/15.
 */
public class BaseActivity extends SherlockActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tracker t = AnalyticHelper.getTracker(this);
        t.setScreenName(this.getClass().getName());
        t.send(new HitBuilders.ScreenViewBuilder().build());

        Fabric.with(this, new Crashlytics());


        Thread.UncaughtExceptionHandler myHandler = new ExceptionReporter(
                t,
                Thread.getDefaultUncaughtExceptionHandler(),
                this);
        Thread.setDefaultUncaughtExceptionHandler(myHandler);
    }
}
