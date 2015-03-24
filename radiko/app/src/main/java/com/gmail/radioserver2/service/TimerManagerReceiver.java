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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

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
        this.context = context;
        cancel();
        startTimerManager();
        reCalculateTimer();
    }

    private void startTimerManager() {
        SimpleAppLog.info("TimerManagerReceiver set timer start on next day ...");
        // Start on next day
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
                Collection<Timer> timers = dbAdapter.findAll();
                if (timers != null && timers.size() > 0) {
                    for (Timer timer : timers) {
                        setAlarm(timer);
                    }
                }
            } catch (Exception e) {
                SimpleAppLog.error("Could not list timer", e);
            } finally {
                dbAdapter.close();
            }
        }
    }

    private void createAlarm(Timer timer, long onTime) {
        String timerSrc = gson.toJson(timer);
        SimpleAppLog.info("Create schedule. Time: " + sdf.format(new Date(onTime)) + "ms. Object: " + timerSrc);
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, TimerBroadcastReceiver.class);
        Bundle bundle = i.getExtras();
        if (bundle == null) {
            bundle = new Bundle();
        }
        bundle.putString(Constants.ARG_OBJECT, timerSrc);
        i.putExtras(bundle);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        am.set(AlarmManager.RTC_WAKEUP, onTime, pi);
    }

    private void setAlarm(Timer timer) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, timer.getStartHour());
        calendar.set(Calendar.MINUTE, timer.getStartMinute());

        long launchTime = calendar.getTimeInMillis();
        Calendar endOfToday = Calendar.getInstance();
        endOfToday.setTimeInMillis(System.currentTimeMillis());
        endOfToday.set(Calendar.HOUR_OF_DAY, 23);
        endOfToday.set(Calendar.MINUTE, 59);
        endOfToday.set(Calendar.SECOND, 59);

        Calendar startOfToday = Calendar.getInstance();
        startOfToday.setTimeInMillis(System.currentTimeMillis());
        startOfToday.set(Calendar.HOUR_OF_DAY, 0);
        startOfToday.set(Calendar.MINUTE, 0);
        startOfToday.set(Calendar.SECOND, 0);

        if (launchTime < endOfToday.getTimeInMillis()
                && launchTime > System.currentTimeMillis()) {
            // If the timer between current time and end of today
            // Start check alarm timer
            switch (timer.getMode()) {
                case Timer.MODE_DAILY:
                    // Ready for launch
                    createAlarm(timer, launchTime);
                    break;
                case Timer.MODE_ONE_TIME:
                    // Should check again with time
                    calendar.setTime(timer.getEventDate());
                    calendar.set(Calendar.HOUR_OF_DAY, timer.getStartHour());
                    calendar.set(Calendar.MINUTE, timer.getStartMinute());
                    if (startOfToday.getTimeInMillis() < calendar.getTimeInMillis()
                            && endOfToday.getTimeInMillis() > calendar.getTimeInMillis()) {
                        // Ready for launch
                        createAlarm(timer, launchTime);
                    }
                    break;
                case Timer.MODE_WEEKLY:
                    // Should check again with date
                    calendar.setTime(timer.getEventDate());
                    if (startOfToday.get(Calendar.DAY_OF_WEEK) == calendar.get(Calendar.DAY_OF_WEEK)){
                        // Ready for launch
                        createAlarm(timer, launchTime);
                    }
                    break;
            }
        }
    }
}
