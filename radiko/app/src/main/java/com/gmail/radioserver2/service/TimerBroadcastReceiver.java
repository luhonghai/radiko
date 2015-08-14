package com.gmail.radioserver2.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.dotohsoft.radio.api.APIRequester;
import com.dotohsoft.radio.data.RadioArea;
import com.dotohsoft.radio.data.RadioChannel;
import com.dotohsoft.radio.data.RadioProgram;
import com.gmail.radioserver2.data.Channel;
import com.gmail.radioserver2.data.RecordedProgram;
import com.gmail.radioserver2.data.Timer;
import com.gmail.radioserver2.data.sqlite.ext.RecordedProgramDBAdapter;
import com.gmail.radioserver2.radiko.TokenFetcher;
import com.gmail.radioserver2.utils.AndroidUtil;
import com.gmail.radioserver2.utils.Constants;
import com.gmail.radioserver2.utils.FileHelper;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import wseemann.media.FFmpegMediaPlayer;

/**
 * Created by luhonghai on 3/22/15.
 */

public class TimerBroadcastReceiver extends BroadcastReceiver {

    public static final String ACTION_START_TIMER = "com.gmail.radioserver2.service.TimerBroadcastReceiver.START_TIMER";
    private int flag = PendingIntent.FLAG_UPDATE_CURRENT;
    private Gson gson = new Gson();

    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        SimpleAppLog.info("Start timer schedule");
        synchronized (TimerManagerReceiver.lockObj) {
            mContext = context;
            String timerObj = intent.getStringExtra(Constants.ARG_OBJECT);
            String timerList = intent.getStringExtra(Constants.ARG_TIMER_LIST);
            if (timerList == null || timerList.length() == 0) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    timerList = bundle.getString(Constants.ARG_TIMER_LIST);
                }
            }

            if (timerList != null && timerList.length() > 0) {
                SimpleAppLog.info("Timer object: " + timerObj);
                try {
                    List<Timer> listTimer;
                    Type mapType = new TypeToken<List<Timer>>() {
                    }.getType();
                    listTimer = gson.fromJson(timerList, mapType);
                    if (listTimer.size() != 0) {
                        createAlarm(listTimer);
                    }
                } catch (Exception e) {
                    SimpleAppLog.error("Could not parse timer object", e);
                }
            }

            if (timerObj == null || timerObj.length() == 0) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    timerObj = bundle.getString(Constants.ARG_OBJECT);
                }
            }

            Intent serviceIntent = new Intent(mContext, RecordBackgroundService.class);
            Bundle bd = new Bundle();
            bd.putString(RecordBackgroundService.PARAM_TIMER, timerObj);
            serviceIntent.putExtras(bd);
            mContext.startService(serviceIntent);
        }
    }

    private void createAlarm(List<Timer> timers) {
        Timer timer = timers.get(0);
        timers.remove(0);
        String timerSrc = gson.toJson(timer);
        String timerList = gson.toJson(timers);
        if (TimerManagerReceiver.mAlarmManager == null) {
            TimerManagerReceiver.mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        }
        Intent i = new Intent(mContext, TimerBroadcastReceiver.class);
        Bundle bundle = i.getExtras();
        if (bundle == null) {
            bundle = new Bundle();
        }
        bundle.putString(Constants.ARG_OBJECT, timerSrc);
        bundle.putString(Constants.ARG_TIMER_LIST, timerList);
        i.putExtras(bundle);
        PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, i, flag);
        TimerManagerReceiver.mAlarmManager.set(AlarmManager.RTC_WAKEUP, timer.getNextAlarmTime(), pi);
    }

}
