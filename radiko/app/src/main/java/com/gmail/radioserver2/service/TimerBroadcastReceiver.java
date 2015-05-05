package com.gmail.radioserver2.service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import wseemann.media.FFmpegMediaPlayer;

/**
 * Created by luhonghai on 3/22/15.
 */

public class TimerBroadcastReceiver extends BroadcastReceiver implements ServiceConnection {

    private static final long MIN_RECORDING_LENGTH = 5000;

    public static final String ACTION_START_TIMER = "com.gmail.radioserver2.service.TimerBroadcastReceiver.START_TIMER";

    private IMediaPlaybackService mService = null;

    private MusicUtils.ServiceToken mServiceToken;

    private Timer selectedTimer;

    private Channel channel = null;

    private Gson gson = new Gson();

    private Context mContext;

    private FFmpegMediaPlayer recorder;

    @Override
    public void onReceive(Context context, Intent intent) {
        SimpleAppLog.info("Start timer schedule");
        mServiceToken = MusicUtils.bindToService(context.getApplicationContext(), this);
        mContext = context;
        String timerObj = intent.getStringExtra(Constants.ARG_OBJECT);
        if (timerObj == null || timerObj.length() == 0) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                timerObj = bundle.getString(Constants.ARG_OBJECT);
            }
        }
        if (timerObj != null && timerObj.length() > 0) {
            SimpleAppLog.info("Timer object: " + timerObj);
            try {
                selectedTimer = gson.fromJson(timerObj, Timer.class);
            } catch (Exception e) {
                SimpleAppLog.error("Could not parse timer object" ,e);
            }
        }

    }

    private void execute() {
        if (selectedTimer != null) {
            final String channelSrc = selectedTimer.getChannelKey();

            if (channelSrc != null && channelSrc.length() > 0) {
                try {
                    channel = gson.fromJson(channelSrc, Channel.class);
                } catch (Exception ex) {
                    SimpleAppLog.error("Could not parse channel source",ex);
                }
            }
            switch (selectedTimer.getType()) {
                case Timer.TYPE_ALARM:
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
                        stop();
                    }
                    break;
                case Timer.TYPE_SLEEP:
                    try {
                        if (mService.isPlaying()) {
                            mService.stop();
                        }
                    } catch (RemoteException e) {
                        SimpleAppLog.error("Could not stop playing", e );
                    } finally {
                        stop();
                    }
                    break;
                case Timer.TYPE_RECORDING:
                    Calendar calStart = Calendar.getInstance();
                    calStart.set(Calendar.HOUR, selectedTimer.getStartHour());
                    calStart.set(Calendar.MINUTE, selectedTimer.getStartMinute());
                    Calendar calFinish = Calendar.getInstance();
                    calFinish.set(Calendar.HOUR, selectedTimer.getFinishHour());
                    calFinish.set(Calendar.MINUTE, selectedTimer.getFinishMinute());
                    long recordingLength = calFinish.getTimeInMillis() - calStart.getTimeInMillis();
                    if (recordingLength <= MIN_RECORDING_LENGTH) {
                        SimpleAppLog.error("Too small recording length: " + recordingLength);
                        return;
                    }
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
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
                    }, recordingLength);

                    TokenFetcher.getTokenFetcher(mContext, new TokenFetcher.OnTokenListener() {
                        @Override
                        public void onTokenFound(final String token, final String rawAreaId) {
                            String url = channel.getUrl();
                            if (mService != null) {
                                try {
                                    if (url.toLowerCase().startsWith("rtmpe://f-radiko.smartstream.ne.jp"))
                                    {
                                        url = mService.updateRtmpSuck("S:" + token, url);
                                    }
                                } catch (Exception e) {
                                    SimpleAppLog.error("Could not update token", e);
                                }
                            }
                            final String streamUrl = url;
                            new AsyncTask<Void, Void, Void>() {
                                @Override
                                protected Void doInBackground(Void... params) {
                                    try {
                                        APIRequester requester = new APIRequester(new FileHelper(mContext).getApiCachedFolder());
                                        RadioChannel.Channel rChannel = new RadioChannel.Channel();
                                        rChannel.setName(channel.getName());
                                        rChannel.setService(channel.getType());
                                        rChannel.setServiceChannelId(channel.getUrl());
                                        rChannel.setServiceChannelId(channel.getKey());
                                        RadioProgram radioProgram = requester.getPrograms(rChannel, RadioArea.getArea(rawAreaId, channel.getType()), AndroidUtil.getAdsId(mContext));
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
                                    return null;
                                }
                            }.execute();
                        }
                        @Override
                        public void onError(String message, Throwable throwable) {
                            stop();
                        }
                    }).fetch();

                    break;
            }
        } else {
            SimpleAppLog.error("No selected timer");
            stop();
        }
    }

    private void startRecording(String url) {
        if (channel != null && selectedTimer != null) {
            try {
                final long startTime = System.currentTimeMillis();
                recorder = new FFmpegMediaPlayer();
                recorder.setRecordingOnly(true);
                recorder.setOnRecordingListener(new FFmpegMediaPlayer.OnRecordingListener() {
                    @Override
                    public void onCompleted(Channel selectedChannel, int recordedSampleRate, int recordedChannel, int recordedAudioEncoding, int recordedBufferSize, String filePath) {
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
                            try {
                                adapter.open();
                                adapter.insert(recordedProgram);
                                SimpleAppLog.debug("Save recording complete");
                            } catch (Exception e) {
                                SimpleAppLog.error("Could not insert recorded program", e);
                            } finally {
                                adapter.close();
                            }
                        }
                        stop();
                    }

                    @Override
                    public void onError(String message, Throwable e) {
                        stop();
                    }
                });
                final FileHelper fileHelper = new FileHelper(mContext);
                recorder.setDataSource(url);
                recorder.setOnPreparedListener(new FFmpegMediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(FFmpegMediaPlayer mp) {
                        SimpleAppLog.info("Start recorder");
                        recorder.start();
                        recorder.startRecording(channel, new File(fileHelper.getRecordedProgramFolder(), channel.getRecordedName() + "-" + startTime + ".mp3").getAbsolutePath());
                    }
                });
                SimpleAppLog.info("Prepare recorder");
                recorder.prepare();
            } catch (Exception e) {
                SimpleAppLog.error("Could not start recording", e);
                stop();
            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        SimpleAppLog.info("TimerBroadcastReceiver - MediaPlaybackService. service connected");
        mService = IMediaPlaybackService.Stub.asInterface(service);
        execute();
    }

    private void stop() {
        // Complete process
        MusicUtils.unbindFromService(mServiceToken);
        mService = null;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mService = null;
    }


}
