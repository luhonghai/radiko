package com.gmail.radioserver2.service;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;

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

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import wseemann.media.FFmpegMediaPlayer;

/**
 * Created by Trinh Quan on 016 16/6/2015.
 */
public class MediaRecord extends Thread {
    private static final long MIN_RECORDING_LENGTH = 5000;
    private Context mContext;
    private Timer selectedTimer;
    private Channel channel;
    private Gson gson = new Gson();
    private FFmpegMediaPlayer recorder;
    private long keyToken;
    private Handler mHandler;
    private IMediaPlaybackService mService;
    private OnRecordStateChangeListenner mStateChangeListenner;

    MediaRecord(Context context, IMediaPlaybackService mService, Timer timer, long keyToken, OnRecordStateChangeListenner mStateChangeListenner) {
        mContext = context;
        selectedTimer = timer;
        this.keyToken = keyToken;
        this.mStateChangeListenner = mStateChangeListenner;
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.arg1) {
                    case RecordBackgroundService.RECORD:
                        SimpleAppLog.debug("RECORD show notification");
                        MediaRecord.this.mStateChangeListenner.showNotification((Channel) msg.obj);
                        break;
                    case RecordBackgroundService.REFRESH:
                        SimpleAppLog.debug("RECORD end");
                        MediaRecord.this.mStateChangeListenner.refresh(MediaRecord.this.keyToken);
                        break;
                    case 2:
                        notifyPlayerStateChanged();
                        break;

                }
            }
        };
        this.mService = mService;
    }

    @Override
    public void run() {
        super.run();
        execute();
    }

    public void execute() {
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
            Message msg = mHandler.obtainMessage();
            msg.arg1 = 2;
            mHandler.sendMessage(msg);
        } else {
            notifyChange(null);
            SimpleAppLog.error("No selected timer");
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

    private void performRecord() {
        Calendar calStart = Calendar.getInstance();
        calStart.set(Calendar.HOUR_OF_DAY, selectedTimer.getStartHour());
        calStart.set(Calendar.MINUTE, selectedTimer.getStartMinute());
        calStart.set(Calendar.SECOND, 0);
        final Calendar calFinish = Calendar.getInstance();
        calFinish.set(Calendar.HOUR_OF_DAY, selectedTimer.getFinishHour());
        calFinish.set(Calendar.MINUTE, selectedTimer.getFinishMinute());
        calFinish.set(Calendar.SECOND, 0);
        long recordingLength = calFinish.getTimeInMillis() - calStart.getTimeInMillis();
        if (recordingLength <= MIN_RECORDING_LENGTH) {
            SimpleAppLog.error("Too small recording length: " + recordingLength);
            return;
        }
        Thread timeOut = new Thread() {
            @Override
            public void run() {
                while (System.currentTimeMillis() < calFinish.getTime().getTime()) {
                    synchronized (this) {
                        try {
                            wait(1000 * 5);
                        } catch (InterruptedException e) {
                            this.notifyAll();
                            e.printStackTrace();
                        }
                    }
                }
                stopRecord();
            }
        };
        timeOut.start();
        TokenFetcher.getTokenFetcher(mContext, tokenListener).fetch();
    }

    private TokenFetcher.OnTokenListener tokenListener = new TokenFetcher.OnTokenListener() {
        @Override
        public void onTokenFound(final String token, final String rawAreaId) {
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
            try {
                APIRequester requester = new APIRequester(new FileHelper(mContext).getApiCachedFolder());
                RadioChannel.Channel rChannel = new RadioChannel.Channel();
                rChannel.setName(channel.getName());
                rChannel.setService(channel.getType());
                rChannel.setServiceChannelId(channel.getUrl());
                rChannel.setServiceChannelId(channel.getKey());
                RadioProgram radioProgram;
                radioProgram = requester.getPrograms(rChannel, RadioArea.getArea(rawAreaId, channel.getType()), AndroidUtil.getAdsId(mContext));

                if (radioProgram != null) {
                    List<RadioProgram.Program> programList = radioProgram.getPrograms();
                    if (programList != null && programList.size() > 0) {
                        for (RadioProgram.Program program : programList) {
                            long now = System.currentTimeMillis();
                            if (now > program.getFromTime() && now < program.getToTime()) {
                                channel.setCurrentProgram(program);
                                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.JAPANESE);
                                final StringBuffer sb = new StringBuffer();
                                sb.append(program.getTitle()).append("\n");
                                sb.append(sdf.format(new Date(program.getFromTime())));
                                sb.append(" - ").append(sdf.format(new Date(program.getToTime())));
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                SimpleAppLog.error("Could not fetch programs", e);
            }
            startRecording(streamUrl);
        }

        @Override
        public void onError(String message, Throwable throwable) {
            notifyChange(null);
        }
    };

    public void stopRecord() {
        SimpleAppLog.debug("Stop recording");
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

    private void notifyChange(Channel channel) {
        Message msg = mHandler.obtainMessage();
        if (channel != null) {
            msg.arg1 = RecordBackgroundService.RECORD;
            msg.obj = channel;
        } else {
            msg.arg1 = RecordBackgroundService.REFRESH;
        }
        mHandler.sendMessage(msg);
    }

    private void startRecording(final String url) {
        if (channel != null && selectedTimer != null) {
            try {
                final long startTime = System.currentTimeMillis();
                recorder = new FFmpegMediaPlayer();
//                recorder.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
                recorder.setRecordingOnly(true);
                notifyChange(channel);
                recorder.setOnRecordingListener(new FFmpegMediaPlayer.OnRecordingListener() {
                    @Override
                    public void onCompleted(Channel selectedChannel, int recordedSampleRate, int recordedChannel, int recordedAudioEncoding, int recordedBufferSize, String filePath, final long recordedID) {
                        if (filePath == null || filePath.length() == 0) {
                            SimpleAppLog.error("Recoding could not be completed");
                            return;
                        }
                        if (selectedChannel == null) return;
                        final File mp3File = new File(filePath);
                        if (mp3File.exists()) {
                            Gson gson = new Gson();
                            RecordedProgramDBAdapter adapter = new RecordedProgramDBAdapter(mContext);
                            RecordedProgram recordedProgram = new RecordedProgram();
                            recordedProgram.setChannelName(selectedChannel.getName() == null ? "" : selectedChannel.getName());
                            recordedProgram.setChannelKey(gson.toJson(selectedChannel));
                            if (selectedChannel.getCurrentProgram() != null) {
                                recordedProgram.setName(selectedChannel.getCurrentProgram().getTitle());
                            }
                            if (recordedProgram.getName() == null) {
                                recordedProgram.setName("");
                            }
                            recordedProgram.setStartTime(new Date(startTime));
                            long endTime = System.currentTimeMillis();
                            recordedProgram.setEndTime(new Date(endTime));
                            recordedProgram.setFilePath(mp3File.getPath());
                            recordedProgram.setId(recordedID);
                            try {
                                adapter.open();
                                adapter.update(recordedProgram);
                                SimpleAppLog.debug("Save recording complete");
                            } catch (Exception e) {
                                SimpleAppLog.error("Could not insert recorded program", e);
                            } finally {
                                adapter.close();
                            }
                        }

                        Calendar calFinish = Calendar.getInstance();
                        calFinish.set(Calendar.HOUR_OF_DAY, selectedTimer.getFinishHour());
                        calFinish.set(Calendar.MINUTE, selectedTimer.getFinishMinute());
                        if (System.currentTimeMillis() >= calFinish.getTime().getTime()) {
                            notifyChange(null);
                        }

                        mHandler.post(new Runnable() {
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
                    public void onRetry() {
                        stopRecord();
                        Calendar calFinish = Calendar.getInstance();
                        calFinish.set(Calendar.HOUR_OF_DAY, selectedTimer.getFinishHour());
                        calFinish.set(Calendar.MINUTE, selectedTimer.getFinishMinute());
                        if (System.currentTimeMillis() < calFinish.getTimeInMillis()) {
                            startRecording(url);
                        } else {
                            notifyChange(null);
                        }
                    }
                });
                final FileHelper fileHelper = new FileHelper(mContext);
                recorder.setDataSource(url);
                final File saveFile = new File(fileHelper.getRecordedProgramFolder(), channel.getRecordedName() + "-" + startTime + ".mp3");
                final long recordedID = saveRecordedProgram(channel, new Date(startTime), saveFile.getAbsolutePath());
                if (recordedID != -1) {
                    recorder.setOnPreparedListener(new FFmpegMediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(FFmpegMediaPlayer mp) {
                            SimpleAppLog.info("Start recorder");
                            recorder.start();
                            recorder.startRecording(channel, saveFile.getAbsolutePath(), recordedID);
                        }
                    });
                    SimpleAppLog.info("Prepare recorder");
                    recorder.prepare();
                }
            } catch (Exception e) {
                SimpleAppLog.error("Could not start recording", e);
                notifyChange(null);
            }
        } else {
            notifyChange(null);
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

    private void notifyPlayerStateChanged() {
        Intent intent = new Intent(MediaPlaybackService.PLAYSTATE_CHANGED);
        mContext.sendBroadcast(intent);
    }
}
