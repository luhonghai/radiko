package com.gmail.radioserver2.service;

import com.gmail.radioserver2.utils.SimpleAppLog;

/**
 * Created by luhonghai on 3/11/15.
 */
public class CustomUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        SimpleAppLog.error("Detect UncaughtExceptionHandler", ex);
    }
}
