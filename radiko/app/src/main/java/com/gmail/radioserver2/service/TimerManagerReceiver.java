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

    private Context context;

    private Gson gson = new Gson();

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    @Override
    public void onReceive(Context context, Intent intent) {
        SimpleAppLog.info("TimerManagerReceiver starting up ...");
        this.context = context.getApplicationContext();
        cancel();
        startTimerManager();
        reCalculateTimer();
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
        Intent intent = new Intent(context, TimerManagerReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);
    }

    private void cancel() {
        // Cancel old schedule
        Intent intent = new Intent(context, TimerManagerReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
        intent = new Intent(context, TimerBroadcastReceiver.class);
        sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        alarmManager.cancel(sender);
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
                        calculateCal.set(Calendar.SECOND, 0);
                        switch (item.getMode()) {
                            case Timer.MODE_ONE_TIME:
                                calculateCal.setTime(item.getEventDate());
                                calculateCal.set(Calendar.HOUR_OF_DAY, item.getStartHour());
                                calculateCal.set(Calendar.MINUTE, item.getStartMinute());
                                calculateCal.set(Calendar.SECOND, 0);
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
                return (t1.getNextAlarmTime() - t2.getNextAlarmTime()) >= 0 ? 1 : -1;
            }
        };
        Collections.sort(timers, comparator);
    }

    private void createAlarm(List<Timer> timers) {
        Timer timer = timers.get(0);
        timers.remove(0);
        String timerSrc = gson.toJson(timer);
        String timerList = gson.toJson(timers);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, TimerBroadcastReceiver.class);
        Bundle bundle = i.getExtras();
        if (bundle == null) {
            bundle = new Bundle();
        }
        bundle.putString(Constants.ARG_OBJECT, timerSrc);
        bundle.putString(Constants.ARG_TIMER_LIST, timerList);
        i.putExtras(bundle);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        am.set(AlarmManager.RTC_WAKEUP, timer.getNextAlarmTime(), pi);
    }

}
