package com.gmail.radioserver2.service;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import com.dotohsoft.radio.api.APIRequester;
import com.dotohsoft.radio.data.RadioArea;
import com.dotohsoft.radio.data.RadioChannel;
import com.dotohsoft.radio.data.RadioProgram;
import com.gmail.radioserver2.BuildConfig;
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

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import wseemann.media.FFmpegMediaPlayer;

/**
 * Created by Trinh Quan on 016 16/6/2015.
 */
public class MediaRecord {
    private static final long MIN_RECORDING_LENGTH = 5 * 1000;
    private final int MAX_FILE_SIZE = 2 * 1024 * 1024;
    private Context mContext;
    private Timer selectedTimer;
    private Channel channel;
    private Gson gson = new Gson();
    private FFmpegMediaPlayer recorder;
    private Handler timeOutHandler = new Handler(Looper.getMainLooper());
    private Handler completeHandler = new Handler(Looper.getMainLooper());
    private Handler retryHandler = new Handler(Looper.getMainLooper());
    private final int MAX_TIMEOUT = 60 * 1000;
    private int currentTimeout = 1000;
    private final String NEW_LINE = "\n";
    private IMediaPlaybackService mService;
    private OnRecordStateChangeListenner mStateChangeListenner;
    private String mCurrentLink;
    private int mCurrentRetry;
    private int mCurrentRetryCount = 0;
    private RadioProgram mRadioProgram;
    private File mLogFile;
    private long failedID = -1;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd - HH:mm:ss");

    public MediaRecord(Context context, IMediaPlaybackService mService, Timer timer, final OnRecordStateChangeListenner mStateChangeListenner) {
        mContext = context;
        selectedTimer = timer;
        this.mStateChangeListenner = mStateChangeListenner;
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

    public void finishRecord() {
        writeLogFile("Timer recording: stop recording");
        try {
            if (recorder != null) {
                recorder.stopRecording();
                recorder.stop();
                recorder.release();
            }
            completeHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyChange(null, true);
                }
            });
        } catch (Exception e) {
            SimpleAppLog.error("Could not stop recording", e);
        }
    }

    public void stopRecord() {
        try {
            if (recorder != null) {
                recorder.stopRecording();
                recorder.stop();
                recorder.release();
                recorder = null;
            }
        } catch (Exception e) {
            SimpleAppLog.error("Could not stop recording", e);
        }
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

    public void execute() {
        SimpleAppLog.debug("RECORD: start excute");
        if (selectedTimer != null) {
            final String channelSrc = selectedTimer.getChannelKey();

            if (channelSrc != null && channelSrc.length() > 0) {
                try {
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
                case Timer.TYPE_RECORDING:
                    performRecord();
                    break;
            }
            notifyPlayerStateChanged();
        } else {
            notifyChange(null, false);
            SimpleAppLog.error("No selected timer");
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
            notifyChange(null, false);
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
            notifyChange(null, false);
        }
    }

    private void performRecord() {
        Calendar calStart = Calendar.getInstance();
        calStart.set(Calendar.HOUR_OF_DAY, selectedTimer.getStartHour());
        calStart.set(Calendar.MINUTE, selectedTimer.getStartMinute());
        calStart.set(Calendar.SECOND, 0);
        final Calendar calFinish = Calendar.getInstance();
        calFinish.set(Calendar.HOUR_OF_DAY, selectedTimer.getFinishHour());
        calFinish.set(Calendar.MINUTE, selectedTimer.getFinishMinute());
        calFinish.set(Calendar.SECOND, 0);
        if (selectedTimer.getFinishHour() < selectedTimer.getStartHour()) {
            calFinish.add(Calendar.DAY_OF_MONTH, 1);
        }
        SimpleAppLog.debug("TIMER TIME: start: " + calStart.toString() + " - " + calFinish.toString());
        writeLogFileRaw("____________________");
        writeLogFile("Timer recording occur: -build: " + BuildConfig.VERSION_NAME);
        long recordingLength = calFinish.getTimeInMillis() - calStart.getTimeInMillis();
        if (recordingLength <= MIN_RECORDING_LENGTH) {
            writeLogFile("Timer recording: timer length too long");
            SimpleAppLog.error("Too small recording length: " + recordingLength);
            return;
        }
        timeOutHandler.postDelayed(timeoutRunnable, calFinish.getTimeInMillis() - System.currentTimeMillis());
        TokenFetcher.getTokenFetcher(mContext, tokenListener).fetch();
    }

    private Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            SimpleAppLog.debug("Record: out of timer");
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    finishRecord();
                    return null;
                }
            }.execute();
        }
    };

    Runnable retryRunnable = new Runnable() {
        @Override
        public void run() {
            SimpleAppLog.debug("Record: retry occur");
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    Calendar calFinish = Calendar.getInstance();
                    calFinish.set(Calendar.HOUR_OF_DAY, selectedTimer.getFinishHour());
                    calFinish.set(Calendar.MINUTE, selectedTimer.getFinishMinute());
                    if (System.currentTimeMillis() < calFinish.getTimeInMillis()) {
                        stopRecord();
                        retryHandler.removeCallbacks(retryRunnable);
                        startRecording(mCurrentLink);
                    } else {
                        finishRecord();
                    }
                    return null;
                }
            }.execute();
        }
    };

    private TokenFetcher.OnTokenListener tokenListener = new TokenFetcher.OnTokenListener() {
        @Override
        public void onTokenFound(String token, final String rawAreaId) {
            String url = channel.getUrl();
            if (mService != null) {
                try {
                    if (url.toLowerCase().startsWith("rtmpe://f-radiko.smartstream.ne.jp")) {
                        url = mService.updateRtmpSuck("S:" + token, url);
                    }
                } catch (Exception e) {
                    SimpleAppLog.error("Could not update token", e);
                    writeLogFile("Timer recording: token could not be updated");
                }
            }
            mCurrentLink = url;
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    APIRequester requester = new APIRequester(new FileHelper(mContext).getApiCachedFolder());
                    RadioChannel.Channel rChannel = new RadioChannel.Channel();
                    rChannel.setName(channel.getName());
                    rChannel.setService(channel.getType());
                    rChannel.setServiceChannelId(channel.getUrl());
                    rChannel.setServiceChannelId(channel.getKey());
                    RadioProgram radioProgram = null;
                    for (int i = 0; i < 3; i++) {
                        writeLogFile("Timer recording: try to fetch program #" + (i + 1));
                        try {
                            radioProgram = requester.getPrograms(rChannel, RadioArea.getArea(rawAreaId, channel.getType()), AndroidUtil.getAdsId(mContext));
                        } catch (IOException e) {
                            SimpleAppLog.error("Could not fetch programs", e);
                            e.printStackTrace();
                        }
                        if (validateChannelProgram(radioProgram)) {
                            break;
                        } else {
                            switch (i) {
                                case 0:
                                    requester.addDay(-1);
                                    break;
                                case 1:
                                    requester.resetDate();
                                    requester.addDay(1);
                                    break;
                            }
                        }
                    }
                    startRecording(mCurrentLink);
                    return null;
                }
            }.execute();
        }

        @Override
        public void onError(String message, Throwable throwable) {
            writeLogFile("Timer recording: token could not be fetched - end recording");
            finishRecord();
            notifyChange(null, true);
        }
    };

    FFmpegMediaPlayer.OnRecordingListener recordingListener = new FFmpegMediaPlayer.OnRecordingListener() {

        @Override
        public void onCompleted(Channel selectedChannel, int recordedSampleRate, int recordedChannel, int recordedAudioEncoding, int recordedBufferSize, String filePath, long recordedID) {
            if (filePath == null || filePath.length() == 0) {
                SimpleAppLog.error("Recoding could not be completed");
                return;
            }
            if (selectedChannel == null) return;

            final File mp3File = new File(filePath);
            if (mp3File.exists()) {
                if (mp3File.length() == 0) {
                    SimpleAppLog.debug("RECORD : Delete invalid file");
                    deleteRecordedProgram(recordedID);
                    try {
                        FileUtils.forceDelete(mp3File);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    RecordedProgramDBAdapter adapter = new RecordedProgramDBAdapter(mContext);
                    try {
                        adapter.open();
                        RecordedProgram recordedProgram = adapter.toObject(adapter.get(recordedID));
                        if (recordedProgram != null) {
                            long endTime = System.currentTimeMillis();
                            if (mRadioProgram != null) {
                                channel.setCurrentProgram(AndroidUtil.getMaxTimedProgram(recordedProgram.getStartTime().getTime(), endTime, MediaRecord.this.mRadioProgram.getPrograms()));
                            }
                            if (channel.getCurrentProgram() != null) {
                                recordedProgram.setName(channel.getCurrentProgram().getTitle());
                            } else {
                                recordedProgram.setName("");
                            }
                            recordedProgram.setChannelKey(gson.toJson(channel));
                            File newFile = new File(mp3File.getParent(), channel.getRecordedName() + "-" + recordedProgram.getStartTime().getTime() + ".mp3");
                            if (mp3File.renameTo(newFile)) {
                                recordedProgram.setFilePath(newFile.getAbsolutePath());
                            }
                            recordedProgram.setEndTime(new Date(endTime));
                            if (mCurrentRetryCount > 1 && !(endTime - recordedProgram.getStartTime().getTime() > 60000)) {
                                writeLogFile("Timer Recording: file recorded too small, file length: " + (endTime - recordedProgram.getStartTime().getTime()));
                                adapter.delete(recordedID);
                                try {
                                    FileUtils.forceDelete(mp3File);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                if (failedID > 0) {
                                    adapter.delete(failedID);
                                    failedID = -1;
                                }
                                adapter.update(recordedProgram);
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        adapter.close();
                    }
                }
            } else {
                SimpleAppLog.debug("RECORD : Delete invalid file");
                deleteRecordedProgram(recordedID);
            }

            Calendar calFinish = Calendar.getInstance();
            calFinish.set(Calendar.HOUR_OF_DAY, selectedTimer.getFinishHour());
            calFinish.set(Calendar.MINUTE, selectedTimer.getFinishMinute());
            calFinish.set(Calendar.SECOND, 0);
            completeHandler.post(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(Constants.INTENT_FILTER_FRAGMENT_ACTION);
                    intent.putExtra(Constants.FRAGMENT_ACTION_TYPE, Constants.ACTION_RELOAD_RECORDED_PROGRAM);
                    mContext.sendBroadcast(intent);
                }
            });
        }

        @Override
        public void onError(String message, Throwable e, long recordedID) {
            deleteRecordedProgram(recordedID);
            finishRecord();
            notifyChange(null, true);
        }

        @Override
        public void onRetry(FFmpegMediaPlayer mp, String cause) {
            mCurrentRetryCount++;
            mCurrentRetry++;
            writeLogFile("Timer recording: retry #" + mCurrentRetryCount + " cause " + cause + " - prepare to retry recording");
            currentTimeout = currentTimeout * 4;
            String path = mp.getRecordingPath();
            if (path == null || path.length() == 0 || !(new File(path).exists())) {
                if (mCurrentRetryCount == 1) {
                    editRecordedProgram(mp.getRecordedID());
                    SimpleAppLog.debug("RECORD : Delete invalid file " + mp.getRecordedID());
                    writeLogFile("Timer recording: null file saved");
                } else {
                    deleteRecordedProgram(mp.getRecordedID());
                    SimpleAppLog.debug("RECORD : Delete invalid file " + mp.getRecordedID());
                    writeLogFile("Timer recording: delete null file");
                }
            } else {
                if (FFmpegMediaPlayer.mRetryPivot != 0 && new File(path).length() == 0) {
                    writeLogFile("Timer recording: delete zero byte file");
                    SimpleAppLog.debug("RECORD : Delete invalid file " + mp.getRecordedID());
                    deleteRecordedProgram(mp.getRecordedID());
                    try {
                        FileUtils.forceDelete(new File(path));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    mCurrentRetry = 1;
                    currentTimeout = 1000;
                }
            }
            FFmpegMediaPlayer.mRetryPivot = mCurrentRetry;
            SimpleAppLog.debug("RECORD " + currentTimeout);
            if (mCurrentRetry <= 5) {
                retryHandler.postDelayed(retryRunnable, currentTimeout);
            } else {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        finishRecord();
                        return null;
                    }
                }.execute();
            }
        }
    };

    private boolean validateChannelProgram(RadioProgram radioProgram) {
        if (radioProgram != null) {
            List<RadioProgram.Program> programList = radioProgram.getPrograms();
            if (programList != null && programList.size() > 0) {
                for (RadioProgram.Program program : programList) {
                    Calendar calNow = Calendar.getInstance();
                    Calendar calFrom = Calendar.getInstance();
                    calFrom.setTimeInMillis(program.getFromTime());
                    Calendar calTo = Calendar.getInstance();
                    calTo.setTimeInMillis(program.getToTime());
                    if (calNow.getTimeInMillis() >= calFrom.getTimeInMillis() && calNow.getTimeInMillis() <= calTo.getTimeInMillis()) {
                        this.mRadioProgram = radioProgram;
                        channel.setCurrentProgram(program);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void startRecording(String url) {
        if (channel != null && selectedTimer != null) {
            long startTime = System.currentTimeMillis();
            recorder = null;
            recorder = new FFmpegMediaPlayer();
            recorder.setRecordingOnly(true);
            notifyChange(channel, true);
            try {
                recorder.setDataSource(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileHelper fileHelper = new FileHelper(mContext);
            File saveFile = new File(fileHelper.getRecordedProgramFolder(), channel.getRecordedName() + "-" + startTime + ".mp3");
            long recordedID = saveRecordedProgram(channel, new Date(startTime), saveFile.getAbsolutePath());
            if (recordedID != -1) {
                recorder.setOnRecordingListener(recordingListener);
                recorder.startRecording(channel, saveFile.getAbsolutePath(), recordedID);
                recorder.setOnPreparedListener(new FFmpegMediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(FFmpegMediaPlayer mp) {
                        SimpleAppLog.info("Start recorder");
                        writeLogFile("Timer recording: prepare successful, start record");
                        mp.start();
                    }
                });
                writeLogFile("Timer recording: prepare to record");
                SimpleAppLog.info("Prepare recorder");
                try {
                    recorder.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean deleteRecordedProgram(long recordedID) {
        RecordedProgramDBAdapter dbAdapter = new RecordedProgramDBAdapter(mContext);
        try {
            dbAdapter.open();
            dbAdapter.delete(recordedID);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            dbAdapter.close();
        }
        return true;
    }

    private boolean editRecordedProgram(long recordID) {
        failedID = recordID;
        RecordedProgramDBAdapter dbAdapter = new RecordedProgramDBAdapter(mContext);
        try {
            dbAdapter.open();
            long endTime = System.currentTimeMillis();
            RecordedProgram recordedProgram = dbAdapter.toObject(dbAdapter.get(recordID));
            if (recordedProgram != null) {
                recordedProgram.setEndTime(new Date(endTime));
                if (channel.getCurrentProgram() != null) {
                    recordedProgram.setName(channel.getCurrentProgram().getTitle() + " (失敗)");
                } else {
                    recordedProgram.setName("(失敗)");
                }
                dbAdapter.update(recordedProgram);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            dbAdapter.close();
        }
        return true;
    }

    private long saveRecordedProgram(Channel channel, Date startTime, String path) {
        long recordedID = -1;
        RecordedProgramDBAdapter dbAdapter = new RecordedProgramDBAdapter(mContext);
        try {
            dbAdapter.open();
            RecordedProgram recordedProgram = new RecordedProgram();
            recordedProgram.setChannelName(channel.getName() == null ? "" : channel.getName());
            recordedProgram.setChannelKey(gson.toJson(channel));
            if (channel.getCurrentProgram() != null) {
                recordedProgram.setName(channel.getCurrentProgram().getTitle());
            }
            if (recordedProgram.getName() == null) {
                recordedProgram.setName("");
            }
            recordedProgram.setStartTime(startTime);
            recordedProgram.setEndTime(startTime);
            recordedProgram.setFilePath(path);
            recordedID = dbAdapter.insert(recordedProgram);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dbAdapter.close();
        }
        return recordedID;
    }

    private void notifyChange(@Nullable Channel channel, boolean isRecord) {
        SimpleAppLog.debug("Record: Notification changed");
        if (mStateChangeListenner != null) {
            if (channel != null) {
                mStateChangeListenner.showNotification(channel);
            } else {
                mStateChangeListenner.refresh(isRecord);
                try {
                    timeOutHandler.removeCallbacks(timeoutRunnable);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    retryHandler.removeCallbacks(retryRunnable);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void notifyPlayerStateChanged() {
        Intent intent = new Intent(MediaPlaybackService.PLAYSTATE_CHANGED);
        mContext.sendBroadcast(intent);
    }
}
