package com.gmail.radioserver2.service;

import android.annotation.SuppressLint;
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
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.widget.RemoteViews;

import com.dotohsoft.radio.data.RadioProgram;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.data.Channel;
import com.gmail.radioserver2.data.Timer;
import com.gmail.radioserver2.utils.Constants;
import com.gmail.radioserver2.utils.FileHelper;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecordBackgroundService extends Service implements OnRecordStateChangeListener, ServiceConnection {
    public static final int PLAYBACK_SERVICE_STATUS = 1;
    public static final String PARAM_TIMER = "param_timer";
    public PowerManager.WakeLock mWakeLock;
    private IMediaPlaybackService mService;
    private MusicUtils.ServiceToken mServiceToken;
    private WifiManager.WifiLock mWifiLock;
    private IBinder binder;
    private ExecutorService mRecordExecutor;
    private ExecutorService mMediaEventExecutor;
    private Handler mHandler;
    private int mReferenceCount;

    @Override
    public void onCreate() {
        super.onCreate();
        mServiceToken = MusicUtils.bindToService(this, this);
        mRecordExecutor = Executors.newSingleThreadExecutor();
        mMediaEventExecutor = Executors.newSingleThreadExecutor();
        mHandler = new Handler(Looper.getMainLooper());
        mReferenceCount = 0;
    }

    public RecordBackgroundService() {
        super();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("SimpleDateFormat")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Bundle bd = intent.getExtras();
            if (bd != null && bd.containsKey(PARAM_TIMER)) {
                SimpleAppLog.debug("RECORD: start command");
                if (isConnectInternet()) {
                    String strTimer = bd.getString(PARAM_TIMER);
                    Timer selectedTimer = new Gson().fromJson(strTimer, Timer.class);
                    if (selectedTimer != null) {
                        acquireLock();
                        prepareMediaAction(selectedTimer);
                    }
                } else {
                    File mLogFile;
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd - HH:mm:ss");
                    FileHelper fileHelper = new FileHelper(this);
                    mLogFile = new File(fileHelper.getRecordedProgramFolder(), "record_log.txt");
                    if (mLogFile.exists() && FileUtils.sizeOf(mLogFile) > 2097152) { //2 * 1024 * 1024
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
                    try {
                        FileUtils.writeStringToFile(mLogFile, dateFormat.format(new Date(System.currentTimeMillis()))
                                        + " - No internet connection to do timer task\n",
                                Charset.forName("US-ASCII"), true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return START_STICKY;
    }


    private void acquireLock() {
        if (mWakeLock == null) {
            PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Record service");
            mWakeLock.setReferenceCounted(true);
        }
        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }
        SimpleAppLog.debug("SERVICE: acquire wakelock");
        WifiManager wManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED
                || wManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
            if (mWifiLock == null) {
                mWifiLock = wManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "record service");
                mWifiLock.setReferenceCounted(true);
            }
            if (!mWifiLock.isHeld()) {
                mWifiLock.acquire();
            }
            SimpleAppLog.debug("SERVICE: acquire wifi lock");
        }
        mReferenceCount++;
    }

    private boolean isConnectInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
        }
        if (mWifiLock != null && mWifiLock.isHeld()) {
            mWifiLock.release();
            mWifiLock = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (binder == null) {
            binder = new RecordServiceBinder();
        }
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unboundService();
        releaseWakeLock();
        stopForeground(true);
        mMediaEventExecutor.shutdown();
        mRecordExecutor.shutdown();
    }

    private void prepareMediaAction(final Timer timer) {
        if (mService != null) {
            if (timer.getType() == Timer.TYPE_RECORDING) {
                MediaRecordRunnable mediaRecordRunnable = new MediaRecordRunnable(getApplicationContext(), mService, timer, this);
                mRecordExecutor.submit(mediaRecordRunnable);
            } else {
                MediaEventRunnable eventRunnable = new MediaEventRunnable(getApplicationContext(), mService, timer, this);
                mMediaEventExecutor.submit(eventRunnable);
            }
        } else {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    prepareMediaAction(timer);
                }
            }, 50);
        }
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
        startForeground(PLAYBACK_SERVICE_STATUS, status);
    }

    @Override
    public void showNotification(Channel channel) {
        if (mService != null) {
            try {
                if (!mService.isPlaying()) {
                    RadioProgram.Program program = channel.getCurrentProgram();
                    if (program != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.JAPANESE);
                        StringBuilder sb = new StringBuilder();
                        sb.append(program.getTitle());
                        sb.append("\n");
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
    public void refresh(boolean isRecord) {
        SimpleAppLog.debug("SERVICE: refresh");
        mReferenceCount--;
        if (mReferenceCount <= 0) {
            releaseWakeLock();
        }
        if (isRecord) {
            stopForeground(true);
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

    public class RecordServiceBinder extends Binder {
        public RecordBackgroundService getService() {
            return RecordBackgroundService.this;
        }
    }
}
