package com.gmail.radioserver2.service;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.RemoteException;
import android.support.annotation.Nullable;

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

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import wseemann.media.FFmpegMediaPlayer;

/**
 * Created by Trinh Quan on 016 16/6/2015.
 */
public class MediaRecord {
    private static final long MIN_RECORDING_LENGTH = 5 * 1000;
    private Context mContext;
    private Timer selectedTimer;
    private Channel channel;
    private Gson gson = new Gson();
    private FFmpegMediaPlayer recorder;
    private Handler timeOutHandler = new Handler();
    private Handler completeSaveHandler = new Handler();
    private Handler retryHandler = new Handler();
    private final int MAX_TIMEOUT = 30 * 1000;
    private int currentTimeout = 1000;

    private IMediaPlaybackService mService;
    private OnRecordStateChangeListenner mStateChangeListenner;
    private String[] link;
    private String currentLink;
    private int currentRetry;
    private int currentLinkPos = 0;
    private int retryCount = 0;
    private int numLink = 0;
    private RadioProgram radioProgram;

    public MediaRecord(Context context, IMediaPlaybackService mService, Timer timer, final OnRecordStateChangeListenner mStateChangeListenner) {
        mContext = context;
        selectedTimer = timer;
        this.mStateChangeListenner = mStateChangeListenner;
        this.mService = mService;
    }


    public void stopRecord() {
        try {
            if (recorder != null) {
                recorder.stopRecording();
                recorder.stop();
                recorder.release();
            }
        } catch (Exception e) {
            SimpleAppLog.error("Could not stop recording", e);
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
            notifyChange(null);
            SimpleAppLog.error("No selected timer");
        }
    }

    private void performSleep() {
        try {
            if (mService.isPlaying()) {
                mService.stop();

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

            }
        } catch (RemoteException e) {
            SimpleAppLog.error("Could not open stream", e);
        } finally {
            notifyChange(null);
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
        long recordingLength = calFinish.getTimeInMillis() - calStart.getTimeInMillis();
        if (recordingLength <= MIN_RECORDING_LENGTH) {
            SimpleAppLog.error("Too small recording length: " + recordingLength);
            return;
        }
        timeOutHandler.postDelayed(timeoutRunable, calFinish.getTimeInMillis() - System.currentTimeMillis());
        TokenFetcher.getTokenFetcher(mContext, tokenListener).fetch();
    }

    private Runnable timeoutRunable = new Runnable() {
        @Override
        public void run() {
            SimpleAppLog.debug("Record: out of timer");
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    stopRecord();
                    return null;
                }
            }.execute();
        }
    };

    Runnable retryRunable = new Runnable() {
        @Override
        public void run() {
            SimpleAppLog.debug("Record: retry occur");
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    stopRecord();
                    retryHandler.removeCallbacks(retryRunable);
                    Calendar calFinish = Calendar.getInstance();
                    calFinish.set(Calendar.HOUR_OF_DAY, selectedTimer.getFinishHour());
                    calFinish.set(Calendar.MINUTE, selectedTimer.getFinishMinute());
                    if (System.currentTimeMillis() < calFinish.getTimeInMillis()) {
                        retryCount++;
                        startRecording(currentLink);
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
                }
            }
            final String streamUrl = url;
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
                    link = streamUrl.split("\\|{4}");
                    numLink = link.length;
                    currentLinkPos = 0;
                    currentLink = link[currentLinkPos];
                    startRecording(currentLink);
                    return null;
                }
            }.execute();
        }

        @Override
        public void onError(String message, Throwable throwable) {
            notifyChange(null);
        }
    };

    FFmpegMediaPlayer.OnRecordingListener recordingListener = new FFmpegMediaPlayer.OnRecordingListener() {

        @Override
        public void onCompleted(Channel selectedChannel, int recordedSampleRate, int recordedChannel, int recordedAudioEncoding, int recordedBufferSize, String filePath, long recordedID, boolean forceStop) {
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
                            if (radioProgram != null) {
                                channel.setCurrentProgram(AndroidUtil.getMaxTimedProgram(recordedProgram.getStartTime().getTime(), endTime, MediaRecord.this.radioProgram.getPrograms()));
                            }
                            if (channel.getCurrentProgram() != null) {
                                recordedProgram.setName(channel.getCurrentProgram().getTitle());
                            } else {
                                recordedProgram.setName("");
                            }
                            File newFile = new File(mp3File.getParent(), channel.getRecordedName() + "-" + recordedProgram.getStartTime().getTime() + ".mp3");
                            if (mp3File.renameTo(newFile)) {
                                recordedProgram.setFilePath(newFile.getAbsolutePath());
                            }
                            recordedProgram.setEndTime(new Date(endTime));
                            adapter.update(recordedProgram);
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
            if (System.currentTimeMillis() >= calFinish.getTimeInMillis() || forceStop) {
                notifyChange(null);
            }
            completeSaveHandler.post(new Runnable() {
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
            notifyChange(null);
        }

        @Override
        public void onRetry(FFmpegMediaPlayer mp) {
            currentLinkPos++;
            if (currentLinkPos >= numLink) {
                currentLinkPos = 0;
                currentRetry++;
                currentTimeout = currentTimeout * 2 > MAX_TIMEOUT ? MAX_TIMEOUT : currentTimeout * 2;
            }
            currentLink = link[currentLinkPos];
            FFmpegMediaPlayer.retry = currentRetry;

            String path = mp.getRecordingPath();
            if (path == null || path.length() == 0 || !(new File(path).exists())) {
                deleteRecordedProgram(mp.getRecordedID());
                SimpleAppLog.debug("RECORD : Delete invalid file " + mp.getRecordedID());
            } else {
                currentRetry = 1;
                currentTimeout = 1000;
            }
            SimpleAppLog.debug("RECORD " + currentTimeout);
            if (currentRetry <= 10) {
                retryHandler.postDelayed(retryRunable, currentTimeout);
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
                        this.radioProgram = radioProgram;
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
            recorder = new FFmpegMediaPlayer();
            recorder.setRecordingOnly(true);
            notifyChange(channel);
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
                        mp.start();
                    }
                });
                SimpleAppLog.info("Prepare recorder");
                try {
                    recorder.prepare();
                } catch (IOException e) {
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
        } catch (Exception e) {
            e.printStackTrace();
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
            Gson gson = new Gson();
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

    private void notifyChange(@Nullable Channel channel) {
        SimpleAppLog.debug("Record: Notification changed");
        if (mStateChangeListenner != null) {
            if (channel != null) {
                mStateChangeListenner.showNotification(channel);
            } else {
                mStateChangeListenner.refresh();
                try {
                    timeOutHandler.removeCallbacks(timeoutRunable);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    retryHandler.removeCallbacks(retryRunable);
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
