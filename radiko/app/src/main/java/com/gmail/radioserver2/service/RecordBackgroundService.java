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
import android.os.Binder;
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
import com.gmail.radioserver2.utils.FileHelper;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;

public class RecordBackgroundService extends Service implements OnRecordStateChangeListener, ServiceConnection {
    private boolean isRecording = false;
    private final int MAX_FILE_SIZE = 2 * 1024 * 1024;
    public static final int PLAYBACK_SERVICE_STATUS = 1;
    public static final String PARAM_TIMER = "param_timer";
    public PowerManager.WakeLock mWakeLock;
    private IMediaPlaybackService mService;
    private MusicUtils.ServiceToken mServiceToken;
    private WifiManager.WifiLock mWifiLock;
    private MediaRecord mMediaRecord;
    private IBinder binder;
    private static final LinkedBlockingQueue<Timer> timerLinkedBlockingQueue = new LinkedBlockingQueue<>();

    @Override
    public void onCreate() {
        super.onCreate();
        binder = new RecordServiceBinder();
        mServiceToken = MusicUtils.bindToService(this, this);
    }

    public RecordBackgroundService() {
        super();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Bundle bd = intent.getExtras();
            if (bd != null) {
                SimpleAppLog.debug("RECORD: start command");
                if (isConnectInternet()) {
                    String strTimer = bd.getString(PARAM_TIMER);
                    Timer selectedTimer = new Gson().fromJson(strTimer, Timer.class);
                    if (selectedTimer != null) {
                        acquireLock();
                        prepareRecord(selectedTimer);
                    }
                } else {
                    File mLogFile;
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd - HH:mm:ss");
                    FileHelper fileHelper = new FileHelper(this);
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
            mWifiLock = wManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "record service");
            mWifiLock.setReferenceCounted(true);

            if (!mWifiLock.isHeld()) {
                mWifiLock.acquire();
            }
            SimpleAppLog.debug("SERVICE: acquire wifi lock");
        }
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
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unboundService();
        timerLinkedBlockingQueue.clear();
        releaseWakeLock();
        stopForeground(true);
        mMediaRecord = null;
    }

    private void prepareRecord(Timer timer) {
        if (timer.getType() == Timer.TYPE_RECORDING) {
            synchronized (timerLinkedBlockingQueue) {
                if (!isRecording) {
                    startTimer(timer);
                } else {
                    SimpleAppLog.debug("add timer to queue");
                    timerLinkedBlockingQueue.add(timer);
                }
            }
        } else {
            MediaRecord mMediaRecord = new MediaRecord(this, mService, timer, this);
            mMediaRecord.execute();
        }
    }

    private void startTimer(Timer timer) {
        isRecording = true;
        mMediaRecord = null;
        mMediaRecord = new MediaRecord(this, mService, timer, this);
        mMediaRecord.execute();
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
        if (isRecord) {
            synchronized (timerLinkedBlockingQueue) {
                isRecording = false;
                if (timerLinkedBlockingQueue.size() != 0) {
                    try {
                        Timer timer;
                        timer = timerLinkedBlockingQueue.take();
                        if (timer != null) {
                            Calendar cal = Calendar.getInstance();
                            cal.set(Calendar.HOUR_OF_DAY, timer.getFinishHour());
                            cal.set(Calendar.MINUTE, timer.getFinishMinute());
                            cal.set(Calendar.SECOND, 0);
                            if (System.currentTimeMillis() < cal.getTimeInMillis()) {
                                SimpleAppLog.debug("SERVICE: START queue timer - Queue size: " + timerLinkedBlockingQueue.size());
                                startTimer(timer);
                            } else if (timerLinkedBlockingQueue.size() != 0) {
                                SimpleAppLog.debug("SERVICE: Queue size: " + timerLinkedBlockingQueue.size());
                                refresh(true);
                            } else {
                                SimpleAppLog.debug("SERVICE: Queue size: " + timerLinkedBlockingQueue.size());
                            }
                        } else if (timerLinkedBlockingQueue.size() != 0) {
                            SimpleAppLog.debug("SERVICE: Queue size: " + timerLinkedBlockingQueue.size());
                            refresh(true);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (!isRecording) {
            releaseWakeLock();
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
