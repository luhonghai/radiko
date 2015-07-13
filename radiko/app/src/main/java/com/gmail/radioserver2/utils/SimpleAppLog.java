package com.gmail.radioserver2.utils;

import android.util.Log;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

/**
 * Created by luhonghai on 2/28/15.
 * Simple log
 */
public class SimpleAppLog {

    private static final String TAG = "Radio Server";

    public static void info(String log) {
        //Log.i(TAG, log);
        if (Fabric.isInitialized())
            Crashlytics.log(Log.INFO, TAG, log);
    }

    public static void debug(String log) {
        Log.d(TAG,log);
    }

    public static void error(String log) {
        error(log, null);
    }

    public static void error(String log, Throwable throwable) {
        if (Fabric.isInitialized())
            Crashlytics.log(Log.ERROR, TAG, log);
        if (throwable == null) {
            Log.e(TAG, log);
        } else {
            Log.e(TAG, log, throwable);
            throwable.printStackTrace();
        }
    }
}
