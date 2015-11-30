package com.gmail.radioserver2.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;

import com.dotohsoft.radio.api.APIRequester;
import com.dotohsoft.radio.data.RadioChannel;
import com.dotohsoft.radio.data.RadioProgram;
import com.gmail.radioserver2.BuildConfig;
import com.gmail.radioserver2.data.Channel;
import com.gmail.radioserver2.data.RecordedProgram;
import com.gmail.radioserver2.data.Timer;
import com.gmail.radioserver2.data.sqlite.ext.RecordedProgramDBAdapter;
import com.gmail.radioserver2.radiko.TokenFetcher;
import com.gmail.radioserver2.utils.AndroidUtil;
import com.gmail.radioserver2.utils.AppDelegate;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import wseemann.media.FFmpegMediaPlayer;

public class MediaRecordRunnable implements Callable<Boolean> {

    private static final long MIN_RECORDING_LENGTH = 60000; //60 *1000
    private static final int MAX_FILE_SIZE = 2097152; //2 * 1024 * 1024
    private static final String NEW_LINE = "\n";

    private Context mContext;
    private Timer mSelectedTimer;
    private Channel mChannel;
    private FFmpegMediaPlayer mRecorder;
    private Handler mCompleteHandler;
    private Handler mRetryHandler;
    private int mCurrentTimeout = 1000;
    private IMediaPlaybackService mService;
    private OnRecordStateChangeListener mStateChangeListener;
    private String mCurrentChannelLink;
    private int mCurrentRetry;
    private int mCurrentRetryCount = 0;
    private RadioProgram mRadioProgram;
    private File mLogFile;
    private long mFailedID = -1;
    private boolean mIsStop;
    private long mFinishTime;

    @SuppressLint("SimpleDateFormat")
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd - HH:mm:ss");

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public MediaRecordRunnable(Context context, IMediaPlaybackService mService, Timer timer, OnRecordStateChangeListener mStateChangeListener) {
        mIsStop = false;
        mContext = context;
        mSelectedTimer = timer;
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
        mCompleteHandler = new Handler(Looper.getMainLooper());
        mRetryHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public Boolean call() throws Exception {
        SimpleAppLog.debug("RECORD: start execute");
        if (mSelectedTimer != null) {
            final String channelSrc = mSelectedTimer.getChannelKey();
            if (channelSrc != null && channelSrc.length() > 0) {
                try {
                    Gson gson = new Gson();
                    mChannel = gson.fromJson(channelSrc, Channel.class);
                } catch (Exception ex) {
                    SimpleAppLog.error("Could not parse mChannel source", ex);
                }
            }
            performRecord(mSelectedTimer);
            notifyPlayerStateChanged();
            while (!mIsStop) {
                if (System.currentTimeMillis() > mFinishTime) {
                    mIsStop = true;
                }
                Thread.sleep(1000);
            }
            finishRecord();
        } else {
            notifyChange(null);
            SimpleAppLog.error("No selected timer");
        }
        releaseResource();
        return true;
    }

    private void releaseResource() {
        mRetryHandler.removeCallbacks(retryRunnable);
        mContext = null;
        mService = null;
    }

    public void finishRecord() {
        writeLogFile("Timer recording: stop recording");
        try {
            if (mRecorder != null) {
                mRecorder.stopRecording();
                mRecorder.stop();
                mRecorder.release();
            }
        } catch (Exception e) {
            SimpleAppLog.error("Could not stop recording", e);
        }
        mCompleteHandler.post(new Runnable() {
            @Override
            public void run() {
                notifyChange(null);
            }
        });
    }

    public void stopRecord() {
        try {
            if (mRecorder != null) {
                mRecorder.stopRecording();
                mRecorder.stop();
                mRecorder.release();
                mRecorder = null;
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

    private void performRecord(Timer mSelectedTimer) {
        Calendar calStart = Calendar.getInstance();
        calStart.set(Calendar.HOUR_OF_DAY, mSelectedTimer.getStartHour());
        calStart.set(Calendar.MINUTE, mSelectedTimer.getStartMinute());
        calStart.set(Calendar.SECOND, 0);
        final Calendar calFinish = Calendar.getInstance();
        calFinish.set(Calendar.HOUR_OF_DAY, mSelectedTimer.getFinishHour());
        calFinish.set(Calendar.MINUTE, mSelectedTimer.getFinishMinute());
        calFinish.set(Calendar.SECOND, 0);
        if (mSelectedTimer.getFinishHour() < mSelectedTimer.getStartHour()) {
            calFinish.add(Calendar.DAY_OF_MONTH, 1);
        }
        SimpleAppLog.debug("TIMER TIME: start: " + calStart.toString() + " - " + calFinish.toString());
        writeLogFileRaw("____________________");
        writeLogFile("Timer recording occur: -build: " + BuildConfig.VERSION_NAME);
        long recordingLength = calFinish.getTimeInMillis() - calStart.getTimeInMillis();
        if (recordingLength <= MIN_RECORDING_LENGTH) {
            writeLogFile("Timer recording: timer length too long");
            SimpleAppLog.error("Too small recording length: " + recordingLength);
            mIsStop = true;
            return;
        }
        mFinishTime = calFinish.getTimeInMillis();
        didGetTokenAndStartRecord();
    }

    private void didGetTokenAndStartRecord() {
        TokenFetcher.getTokenFetcher(mContext, AppDelegate.getInstance().getCookie(),
                new TokenFetcher.OnTokenListener() {
                    @Override
                    public void onTokenFound(String token, final String rawAreaId) {
                        try {
                            if (mChannel != null && !mIsStop) {
                                String url = mChannel.getUrl();
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
                                mCurrentChannelLink = url;
                                APIRequester requester = new APIRequester(new FileHelper(mContext).getApiCachedFolder());
                                RadioChannel.Channel rChannel = new RadioChannel.Channel();
                                rChannel.setName(mChannel.getName());
                                rChannel.setService(mChannel.getType());
                                rChannel.setStreamURL(mChannel.getUrl());
                                rChannel.setServiceChannelId(mChannel.getKey());
                                rChannel.setRegionID(mChannel.getRegionID());
                                RadioProgram radioProgram = null;
                                for (int i = 0; i < 3; i++) {
                                    writeLogFile("Timer recording: try to fetch program #" + (i + 1));
                                    try {
                                        radioProgram = requester.getPrograms(rChannel, AndroidUtil.getAdsId(mContext));
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
                                if (!mIsStop) {
                                    startRecording(mCurrentChannelLink);
                                }
                            }
                        } catch (Exception e) {
                            mIsStop = true;
                        }
                    }

                    @Override
                    public void onError(String message, Throwable throwable) {
                        writeLogFile("Timer recording: token could not be fetched - end recording");
                        mIsStop = true;
                    }
                }).fetch();
    }


    FFmpegMediaPlayer.OnRecordingListener recordingListener = new FFmpegMediaPlayer.OnRecordingListener() {

        @Override
        public void onCompleted(Channel selectedChannel, int recordedSampleRate, int recordedChannel, int recordedAudioEncoding, int recordedBufferSize, String filePath, long recordedID) {
            if (filePath == null || filePath.length() == 0) {
                SimpleAppLog.error("Recoding could not be completed");
                return;
            }
            if (selectedChannel == null) {
                return;
            }

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
                                selectedChannel.setCurrentProgram(AndroidUtil.getMaxTimedProgram(recordedProgram.getStartTime().getTime(), endTime, MediaRecordRunnable.this.mRadioProgram.getPrograms()));
                            }
                            if (selectedChannel.getCurrentProgram() != null) {
                                recordedProgram.setName(selectedChannel.getCurrentProgram().getTitle());
                            } else {
                                recordedProgram.setName("");
                            }
                            Gson gson = new Gson();
                            recordedProgram.setChannelKey(gson.toJson(selectedChannel));
                            File newFile = new File(mp3File.getParent(), selectedChannel.getRecordedName() + "-" + recordedProgram.getStartTime().getTime() + ".mp3");
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
                                if (mFailedID > 0) {
                                    adapter.delete(mFailedID);
                                    mFailedID = -1;
                                }
                                adapter.update(recordedProgram);
                            }
                        }
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
            calFinish.set(Calendar.HOUR_OF_DAY, mSelectedTimer.getFinishHour());
            calFinish.set(Calendar.MINUTE, mSelectedTimer.getFinishMinute());
            calFinish.set(Calendar.SECOND, 0);
            if (mCompleteHandler != null) {
                mCompleteHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(Constants.INTENT_FILTER_FRAGMENT_ACTION);
                        intent.putExtra(Constants.FRAGMENT_ACTION_TYPE, Constants.ACTION_RELOAD_RECORDED_PROGRAM);
                        mContext.sendBroadcast(intent);
                    }
                });
            }
        }

        @Override
        public void onError(String message, Throwable e, long recordedID) {
            deleteRecordedProgram(recordedID);
            mIsStop = true;
        }

        @Override
        public void onRetry(FFmpegMediaPlayer mp, String cause) {
            mCurrentRetryCount++;
            mCurrentRetry++;
            writeLogFile("Timer recording: retry #" + mCurrentRetryCount + " cause " + cause + " - prepare to retry recording");
            mCurrentTimeout = mCurrentTimeout * 4;
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
                    mCurrentTimeout = 1000;
                }
            }
            FFmpegMediaPlayer.mRetryPivot = mCurrentRetry;
            SimpleAppLog.debug("RECORD " + mCurrentTimeout);
            if (mCurrentRetry <= 5 && mRetryHandler != null) {
                mRetryHandler.postDelayed(retryRunnable, mCurrentTimeout);
            } else {
                mIsStop = true;
            }
        }
    };

    private boolean validateChannelProgram(RadioProgram radioProgram) {
        if (radioProgram != null && !mIsStop) {
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
                        mChannel.setCurrentProgram(program);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void startRecording(String url) {
        if (mChannel != null && mSelectedTimer != null) {
            long startTime = System.currentTimeMillis();
            mRecorder = null;
            mRecorder = new FFmpegMediaPlayer();
            mRecorder.setRecordingOnly(true);
            notifyChange(mChannel);
            try {
                mRecorder.setDataSource(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileHelper fileHelper = new FileHelper(mContext);
            File saveFile = new File(fileHelper.getRecordedProgramFolder(), mChannel.getRecordedName() + "-" + startTime + ".mp3");
            long recordedID = saveRecordedProgram(mChannel, new Date(startTime), saveFile.getAbsolutePath());
            if (recordedID != -1) {
                mRecorder.setOnRecordingListener(recordingListener);
                mRecorder.startRecording(mChannel, saveFile.getAbsolutePath(), recordedID);
                mRecorder.setOnPreparedListener(new FFmpegMediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(FFmpegMediaPlayer mp) {
                        SimpleAppLog.info("Start Recorder");
                        writeLogFile("Timer recording: prepare successful, start record");
                        mp.start();
                    }
                });
                writeLogFile("Timer recording: prepare to record");
                SimpleAppLog.info("Prepare mRecorder");
                try {
                    mRecorder.prepare();
                } catch (IOException | IllegalStateException e) {
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
        mFailedID = recordID;
        RecordedProgramDBAdapter dbAdapter = new RecordedProgramDBAdapter(mContext);
        try {
            dbAdapter.open();
            long endTime = System.currentTimeMillis();
            RecordedProgram recordedProgram = dbAdapter.toObject(dbAdapter.get(recordID));
            if (recordedProgram != null) {
                recordedProgram.setEndTime(new Date(endTime));
                String channelKey = recordedProgram.getChannelKey();
                if (channelKey.length() != 0) {
                    Gson gson = new Gson();
                    Channel channel = gson.fromJson(channelKey, Channel.class);
                    if (channel.getCurrentProgram() != null) {
                        recordedProgram.setName(channel.getCurrentProgram().getTitle() + " (失敗)");
                    } else {
                        recordedProgram.setName("(失敗)");
                    }
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
            Gson gson = new Gson();
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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dbAdapter.close();
        }
        return recordedID;
    }

    private void notifyChange(@Nullable Channel channel) {
        SimpleAppLog.debug("Record: Notification changed");
        if (mStateChangeListener != null) {
            if (channel != null) {
                mStateChangeListener.showNotification(channel);
            } else {
                mStateChangeListener.refresh(true);
                try {
                    if (mRetryHandler != null) {
                        mRetryHandler.removeCallbacks(retryRunnable);
                        mRetryHandler = null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mStateChangeListener = null;
            }
        } else {
            SimpleAppLog.debug("Handler went away");
        }
    }

    private void notifyPlayerStateChanged() {
        Intent intent = new Intent(MediaPlaybackService.PLAYSTATE_CHANGED);
        mContext.sendBroadcast(intent);
    }

    Runnable retryRunnable = new Runnable() {
        @Override
        public void run() {
            SimpleAppLog.debug("Record: retry occur");
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    Calendar calFinish = Calendar.getInstance();
                    calFinish.set(Calendar.HOUR_OF_DAY, mSelectedTimer.getFinishHour());
                    calFinish.set(Calendar.MINUTE, mSelectedTimer.getFinishMinute());
                    if (System.currentTimeMillis() < calFinish.getTimeInMillis()) {
                        stopRecord();
                        mRetryHandler.removeCallbacks(retryRunnable);
                        startRecording(mCurrentChannelLink);
                    } else {
                        mIsStop = true;
                    }
                    return null;
                }
            }.execute();
        }
    };
}
