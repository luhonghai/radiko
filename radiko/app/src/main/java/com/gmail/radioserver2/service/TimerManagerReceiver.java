package com.gmail.radioserver2.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.gmail.radioserver2.data.Timer;
import com.gmail.radioserver2.data.sqlite.ext.TimerDBAdapter;
import com.gmail.radioserver2.utils.Constants;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.google.gson.Gson;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by luhonghai on 3/23/15.
 */
public class TimerManagerReceiver extends BroadcastReceiver {

    public static final String ACTION_START_TIMER = "com.gmail.radioserver2.service.TimerManagerReceiver.START_TIMER";
    private int flag = PendingIntent.FLAG_UPDATE_CURRENT;
    private final int OFFSET_TIME = 50;
    private Context context;
    public final static Object lockObj = new Object();
    private Gson gson = new Gson();
    public static AlarmManager mAlarmManager;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    @Override
    public void onReceive(Context context, Intent intent) {
        SimpleAppLog.info("TimerManagerReceiver starting up ...");
        this.context = context.getApplicationContext();
        // log time (ms) and entering critical section here!
        SimpleAppLog.debug("Start reset timer" + System.currentTimeMillis() + "");
        synchronized (lockObj) {
            cancel();
            startTimerManager();
            reCalculateTimer();
        }
        SimpleAppLog.debug("Stop reset timer" + System.currentTimeMillis() + "");
        // log time (ms) and leaving critical section here!
    }

    private void startTimerManager() {
        SimpleAppLog.info("TimerManagerReceiver set timer start on next day ...");
        // Start on next day
        context.startService(new Intent(context, RecordBackgroundService.class));
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 1);
//debug only
        Intent intent = new Intent(context, TimerManagerReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        if (mAlarmManager == null) {
            mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        }
        mAlarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);
    }

    private void cancel() {
        // Cancel old schedule
        Intent intent = new Intent(context, TimerManagerReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, flag);
        if (mAlarmManager == null) {
            mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        }
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mAlarmManager.cancel(sender);
        sender.cancel();
        intent = new Intent(context, TimerBroadcastReceiver.class);
        sender = PendingIntent.getBroadcast(context, 0, intent, flag);
        mAlarmManager.cancel(sender);
        sender.cancel();
    }

    private void reCalculateTimer() {
        if (context != null) {
            SimpleAppLog.info("TimerManagerReceiver re-calculate timer ...");
            TimerDBAdapter dbAdapter = new TimerDBAdapter(context);
            try {
                dbAdapter.open();
                List<Timer> timers = (List<Timer>) dbAdapter.findAll();
                List<Timer> todayTimerList = new ArrayList<>();
                final Calendar currentCal = Calendar.getInstance();

                if (timers != null && timers.size() != 0) {
                    for (Timer item : timers) {
                        Calendar calculateCal = Calendar.getInstance();
                        calculateCal.set(Calendar.HOUR_OF_DAY, item.getStartHour());
                        calculateCal.set(Calendar.MINUTE, item.getStartMinute());
                        if (item.getStartHour() == 0 && item.getStartMinute() == 0) {
                            calculateCal.set(Calendar.SECOND, OFFSET_TIME);
                        } else {
                            calculateCal.set(Calendar.SECOND, 0);
                        }
                        calculateCal.set(Calendar.MILLISECOND, 0);
                        switch (item.getMode()) {
                            case Timer.MODE_ONE_TIME:
                                calculateCal.setTime(item.getEventDate());
                                calculateCal.set(Calendar.HOUR_OF_DAY, item.getStartHour());
                                calculateCal.set(Calendar.MINUTE, item.getStartMinute());
                                if (item.getStartHour() == 0 && item.getStartMinute() == 0) {
                                    calculateCal.set(Calendar.SECOND, OFFSET_TIME);
                                } else {
                                    calculateCal.set(Calendar.SECOND, 0);
                                }
                                if (calculateCal.getTimeInMillis() > currentCal.getTimeInMillis()) {
                                    item.setNextAlarmTime(calculateCal.getTimeInMillis());
                                    todayTimerList.add(item);
                                    SimpleAppLog.debug("Add today timer: " + new Date(item.getNextAlarmTime()).toString());
                                }
                                break;

                            case Timer.MODE_WEEKLY:
                                if (calculateCal.getTimeInMillis() > currentCal.getTimeInMillis()) {
                                    calculateCal.setTime(item.getEventDate());
                                    if (currentCal.get(Calendar.DAY_OF_WEEK) == calculateCal.get(Calendar.DAY_OF_WEEK)) {
                                        calculateCal.set(Calendar.HOUR_OF_DAY, item.getStartHour());
                                        calculateCal.set(Calendar.MINUTE, item.getStartMinute());
                                        calculateCal.set(Calendar.SECOND, 0);
                                        if (item.getStartHour() == 0 && item.getStartMinute() == 0) {
                                            calculateCal.set(Calendar.SECOND, OFFSET_TIME);
                                        } else {
                                            calculateCal.set(Calendar.SECOND, 0);
                                        }
                                        item.setNextAlarmTime(calculateCal.getTimeInMillis());
                                        todayTimerList.add(item);
                                        SimpleAppLog.debug("Add today timer: " + new Date(item.getNextAlarmTime()).toString());
                                    }
                                }
                                break;
                            case Timer.MODE_DAILY:
                                if (calculateCal.getTimeInMillis() > currentCal.getTimeInMillis()) {
                                    item.setNextAlarmTime(calculateCal.getTimeInMillis());
                                    todayTimerList.add(item);
                                    SimpleAppLog.debug("Add today timer: " + new Date(item.getNextAlarmTime()).toString());
                                }
                                break;
                        }
                    }

                    if (todayTimerList.size() != 0) {
                        sortTimer(todayTimerList);
                        createAlarm(todayTimerList);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                dbAdapter.close();
            }
        }
    }

    private void sortTimer(List<Timer> timers) {
        Comparator<Timer> comparator = new Comparator<Timer>() {
            @Override
            public int compare(Timer t1, Timer t2) {
                int result = compareTime(t1.getNextAlarmTime(), t2.getNextAlarmTime()) > 0 ? 1 :
                        (compareTime(t1.getNextAlarmTime(), t2.getNextAlarmTime()) == 0)
                                && (t1.getType() == Timer.TYPE_RECORDING && t1.getType() == t2.getType()) ? 0 : -1;
                if (result == 0) {
                    result = totalRecordTime(t1) - totalRecordTime(t2) > 0 ? 1 : -1;
                }
                return result;
            }
        };
        Collections.sort(timers, comparator);
    }

    private byte compareTime(long var1, long var2) {
        long offset = var1 - var2;
        return (byte) (offset < 1000 ? 0 : offset < 0 ? -1 : 1);
    }

    private long totalRecordTime(Timer t) {
        Calendar finishCal = Calendar.getInstance();
        finishCal.set(Calendar.HOUR_OF_DAY, t.getFinishHour());
        finishCal.set(Calendar.MINUTE, t.getFinishMinute());
        finishCal.set(Calendar.SECOND, 0);
        if (t.getFinishHour() < t.getStartHour()) {
            finishCal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return finishCal.getTimeInMillis() - t.getNextAlarmTime();
    }

    private void createAlarm(List<Timer> timers) {
        Timer timer = timers.get(0);
        timers.remove(0);
        String timerSrc = gson.toJson(timer);
        String timerList = gson.toJson(timers);
        if (mAlarmManager == null) {
            mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        }
        Intent i = new Intent(context, TimerBroadcastReceiver.class);
        Bundle bundle = i.getExtras();
        if (bundle == null) {
            bundle = new Bundle();
        }
        bundle.putString(Constants.ARG_OBJECT, timerSrc);
        bundle.putString(Constants.ARG_TIMER_LIST, timerList);
        i.putExtras(bundle);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, flag);
        mAlarmManager.set(AlarmManager.RTC_WAKEUP, timer.getNextAlarmTime(), pi);
    }
}
