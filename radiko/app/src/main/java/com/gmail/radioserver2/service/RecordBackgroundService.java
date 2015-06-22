package com.gmail.radioserver2.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.widget.RemoteViews;

import com.dotohsoft.radio.data.RadioProgram;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.data.Channel;
import com.gmail.radioserver2.data.Timer;
import com.gmail.radioserver2.utils.Constants;
import com.gmail.radioserver2.utils.SimpleAppLog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by Trinh Quan on 016 16/6/2015.
 */
public class RecordBackgroundService extends Service implements OnRecordStateChangeListenner, ServiceConnection {
    public static final int PLAYBACKSERVICE_STATUS = 1;
    public static final String PARAM_TIMER = "param_timer";
    public static final int RECORD = 0;
    public static final int REFRESH = 1;
    public static PowerManager.WakeLock mWakeLock;
    private IMediaPlaybackService mService;
    private MusicUtils.ServiceToken mServiceToken;
    private Timer selectedTimer;
    private HashMap<Long, Long> hashMap = new HashMap<>();

    private WifiManager.WifiLock wifiLock;

    @Override
    public void onCreate() {
        super.onCreate();
        mServiceToken = MusicUtils.bindToService(this, this);
    }

    public RecordBackgroundService() {
        super();
    }

    private void acquireLock() {
        PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Record service");
        mWakeLock.setReferenceCounted(true);
        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }
        SimpleAppLog.debug("SERVICE: acquire wakelock");
        WifiManager wManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED
                || wManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
            wifiLock = wManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "record service");
            wifiLock.setReferenceCounted(true);
            if (!wifiLock.isHeld()) {
                wifiLock.acquire();
            }
            SimpleAppLog.debug("SERVICE: acquire wifi lock");
        }
    }

    private boolean isConnectInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isConnectInternet()) {
            if (intent != null) {
                Bundle bd = intent.getExtras();
                if (bd != null) {
                    selectedTimer = (Timer) bd.getSerializable(PARAM_TIMER);
                    if (selectedTimer != null) {
                        acquireLock();
                        startRecord(selectedTimer);
                    }
                }
            }
        } else {
            SimpleAppLog.debug("SERVICE: no connection");
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        SimpleAppLog.debug("RECORD: kill service");
        stopForeground(true);
        unboundService();
        releaseWakelock();
        hashMap.clear();
        super.onDestroy();
    }

    private void releaseWakelock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    private void startRecord(Timer timer) {
        new MediaRecord(this, mService, timer, timer.getId(), this).start();
        hashMap.put(timer.getId(), timer.getId());
    }

    private void showServiceNotification(String title, String description) {
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.statusbar);
        views.setImageViewResource(R.id.icon, R.drawable.app_icon);
        views.setTextViewText(R.id.trackname, title);
        views.setTextViewText(R.id.artistalbum, description);
        Notification status = new Notification();
        status.contentView = views;
        status.flags |= Notification.FLAG_ONGOING_EVENT;
        status.icon = R.drawable.app_icon;
        Intent viewer = new Intent(Constants.PLAYBACK_VIEWER_INTENT);
        viewer.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        status.contentIntent = PendingIntent.getActivity(this, 0, viewer, 0);
        startForeground(PLAYBACKSERVICE_STATUS, status);
    }

    @Override
    public void showNotification(Channel channel) {
        if (mService != null) {
            try {
                if (!mService.isPlaying()) {
                    RadioProgram.Program program = channel.getCurrentProgram();
                    if (program != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.JAPANESE);
                        final StringBuffer sb = new StringBuffer();
                        sb.append(program.getTitle()).append("\n");
                        sb.append(sdf.format(new Date(program.getFromTime())));
                        sb.append(" - ").append(sdf.format(new Date(program.getToTime())));
                        showServiceNotification(channel.getName(), sb.toString());
                    } else {
                        showServiceNotification(channel.getName(), "");
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void refresh(long keyToken) {
        SimpleAppLog.debug("SERVICE: refresh");
        try {
            hashMap.remove(keyToken);
        } catch (Exception e) {
            SimpleAppLog.debug("SERVICE: record has been removed");
        }
        if (hashMap.size() == 0) {
            stopForeground(true);
            releaseWakelock();
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        SimpleAppLog.info("RecordProcess - MediaPlaybackService. service connected");
        mService = IMediaPlaybackService.Stub.asInterface(service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mService = null;
    }

    private void unboundService() {
        MusicUtils.unbindFromService(mServiceToken);
        mService = null;
    }
}
