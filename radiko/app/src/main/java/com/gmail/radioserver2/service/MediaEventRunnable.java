package com.gmail.radioserver2.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import com.gmail.radioserver2.BuildConfig;
import com.gmail.radioserver2.data.Channel;
import com.gmail.radioserver2.data.Timer;
import com.gmail.radioserver2.utils.FileHelper;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;

public class MediaEventRunnable implements Callable<Boolean> {

    private static final int MAX_FILE_SIZE = 2097152; //2 * 1024 * 1024
    private static final String NEW_LINE = "\n";

    private Context mContext;
    private Timer selectedTimer;
    private Channel channel;
    private IMediaPlaybackService mService;
    private OnRecordStateChangeListener mStateChangeListener;
    private File mLogFile;

    @SuppressLint("SimpleDateFormat")
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd - HH:mm:ss");

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public MediaEventRunnable(Context context, IMediaPlaybackService mService, Timer timer, final OnRecordStateChangeListener mStateChangeListener) {
        mContext = context;
        selectedTimer = timer;
        this.mStateChangeListener = mStateChangeListener;
        this.mService = mService;
        FileHelper fileHelper = new FileHelper(mContext);
        mLogFile = new File(fileHelper.getRecordedProgramFolder(), "record_log.txt");
        if (mLogFile.exists() && FileUtils.sizeOf(mLogFile) > MAX_FILE_SIZE) {
            File tempLogFile = new File(fileHelper.getRecordedProgramFolder(), "record_log_1.txt");
            if (tempLogFile.exists()) {
                try {
                    FileUtils.forceDelete(tempLogFile);
                    mLogFile.renameTo(tempLogFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                mLogFile.renameTo(tempLogFile);
            }
        }
    }


    @Override
    public Boolean call() throws Exception {
        SimpleAppLog.debug("MEDIA EVENT: start execute");
        if (selectedTimer != null) {
            final String channelSrc = selectedTimer.getChannelKey();

            if (channelSrc != null && channelSrc.length() > 0) {
                try {
                    Gson gson = new Gson();
                    channel = gson.fromJson(channelSrc, Channel.class);
                } catch (Exception ex) {
                    SimpleAppLog.error("Could not parse channel source", ex);
                }
            }
            switch (selectedTimer.getType()) {
                case Timer.TYPE_ALARM:
                    performAlarm(channelSrc);
                    break;
                case Timer.TYPE_SLEEP:
                    performSleep();
                    break;
            }
            notifyPlayerStateChanged();
        } else {
            notifyChange(null);
            SimpleAppLog.error("No selected timer");
        }
        releaseResource();
        return true;
    }

    private void writeLogFileRaw(String log) {
        try {
            FileUtils.writeStringToFile(mLogFile, log + NEW_LINE, Charset.forName("US-ASCII"), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeLogFile(String log) {
        try {
            FileUtils.writeStringToFile(mLogFile, dateFormat.format(new Date(System.currentTimeMillis())) + " - " + log + NEW_LINE,
                    Charset.forName("US-ASCII"), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void performSleep() {
        try {
            if (mService.isPlaying()) {
                mService.stop();
                writeLogFileRaw("____________________");
                writeLogFile("Timer stop playing occur: -build: " + BuildConfig.VERSION_NAME);
            }
        } catch (RemoteException e) {
            SimpleAppLog.error("Could not stop playing", e);
        } finally {
            notifyChange(null);
        }
    }

    private void performAlarm(String channelSrc) {
        try {
            if (channel != null) {
                if (mService.isPlaying()) {
                    mService.stop();
                }
                mService.openStream("", channelSrc);
                writeLogFileRaw("____________________");
                writeLogFile("Timer start playing occur: -build: " + BuildConfig.VERSION_NAME);
            }
        } catch (RemoteException e) {
            SimpleAppLog.error("Could not open stream", e);
        } finally {
            notifyChange(null);
        }
    }

    private void releaseResource() {
        mContext = null;
        mLogFile = null;
        mService = null;
    }


    private void notifyChange(@Nullable Channel channel) {
        SimpleAppLog.debug("Record: Notification changed");
        if (mStateChangeListener != null) {
            if (channel != null) {
                mStateChangeListener.showNotification(channel);
            } else {
                mStateChangeListener.refresh(false);
            }
            mStateChangeListener = null;
        }
    }

    private void notifyPlayerStateChanged() {
        Intent intent = new Intent(MediaPlaybackService.PLAYSTATE_CHANGED);
        mContext.sendBroadcast(intent);
    }
}
