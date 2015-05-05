/*
 * FFmpegMediaPlayer: A unified interface for playing audio files and streams.
 *
 * Copyright 2014 William Seemann
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gmail.radioserver2.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.RemoteControlClient;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.BaseColumns;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.dotohsoft.radio.api.APIRequester;
import com.dotohsoft.radio.data.RadioArea;
import com.dotohsoft.radio.data.RadioChannel;
import com.dotohsoft.radio.data.RadioProgram;
import com.dotohsoft.radio.data.RadioProvider;
import com.dotohsoft.rtmpdump.RTMPSuck;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.data.Channel;
import com.gmail.radioserver2.data.RecordedProgram;
import com.gmail.radioserver2.data.sqlite.ext.ChannelDBAdapter;
import com.gmail.radioserver2.data.sqlite.ext.RecordedProgramDBAdapter;
import com.gmail.radioserver2.provider.Media;
import com.gmail.radioserver2.radiko.TokenFetcher;
import com.gmail.radioserver2.receiver.MediaButtonIntentReceiver;
import com.gmail.radioserver2.utils.AndroidUtil;
import com.gmail.radioserver2.utils.Constants;
import com.gmail.radioserver2.utils.FileHelper;
import com.gmail.radioserver2.utils.InetHelper;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.google.gson.Gson;


import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.Vector;

import at.aau.itec.android.mediaplayer.FileSource;
import at.aau.itec.android.mediaplayer.MediaSource;
import wseemann.media.FFmpegMediaPlayer;

/**
 * Provides "background" audio playback capabilities, allowing the
 * user to switch between activities without stopping playback.
 */
public class MediaPlaybackService extends Service {
    /** used to specify whether enqueue() should start playing
     * the new list of files right away, next or once all the currently
     * queued files have been played
     */
    public static final int NOW = 1;
    public static final int NEXT = 2;
    public static final int LAST = 3;
    public static final int PLAYBACKSERVICE_STATUS = 1;

    public static final int SHUFFLE_NONE = 0;
    public static final int SHUFFLE_NORMAL = 1;
    public static final int SHUFFLE_AUTO = 2;

    public static final int REPEAT_NONE = 0;
    public static final int REPEAT_CURRENT = 1;
    public static final int REPEAT_ALL = 2;

    public static final String PLAYSTATE_CHANGED = "com.android.music.playstatechanged";
    public static final String META_CHANGED = "com.android.music.metachanged";
    public static final String QUEUE_CHANGED = "com.android.music.queuechanged";

    public static final String SERVICECMD = "com.android.music.musicservicecommand";
    public static final String CMDNAME = "command";
    public static final String CMDTOGGLEPAUSE = "togglepause";
    public static final String CMDSTOP = "stop";
    public static final String CMDPAUSE = "pause";
    public static final String CMDPLAY = "play";
    public static final String CMDPREVIOUS = "previous";
    public static final String CMDNEXT = "next";

    public static final String TOGGLEPAUSE_ACTION = "com.android.music.musicservicecommand.togglepause";
    public static final String PAUSE_ACTION = "com.android.music.musicservicecommand.pause";
    public static final String PREVIOUS_ACTION = "com.android.music.musicservicecommand.previous";
    public static final String NEXT_ACTION = "com.android.music.musicservicecommand.next";

    private static final int TRACK_ENDED = 1;
    private static final int RELEASE_WAKELOCK = 2;
    private static final int SERVER_DIED = 3;
    private static final int FOCUSCHANGE = 4;
    private static final int FADEDOWN = 5;
    private static final int FADEUP = 6;
    private static final int TRACK_WENT_TO_NEXT = 7;
    private static final int MAX_HISTORY_SIZE = 100;
    /**
     * FFMpeg streaming player
     */
    private MultiPlayer mStreamingPlayer;

    private CustomMultiPlayer mPlayer;

    private boolean isStreaming;

    private String mFileToPlay;
    private int mShuffleMode = SHUFFLE_NONE;
    private int mRepeatMode = REPEAT_NONE;
    private int mMediaMountedCount = 0;
    private long [] mAutoShuffleList = null;
    private long [] mPlayList = null;
    private int mPlayListLen = 0;
    private Vector<Integer> mHistory = new Vector<Integer>(MAX_HISTORY_SIZE);
    private Cursor mCursor;
    private int mPlayPos = -1;
    private int mNextPlayPos = -1;
    private static final String LOGTAG = "MediaPlaybackService";
    private final Shuffler mRand = new Shuffler();
    private int mOpenFailedCounter = 0;
    String[] mCursorCols = new String[] {
            BaseColumns._ID,                // index must match IDCOLIDX below
            Media.MediaColumns.ARTIST,
            Media.MediaColumns.ALBUM,
            Media.MediaColumns.TITLE,
            Media.MediaColumns.URI
    };
    private final static int IDCOLIDX = 0;
    //private final static int PODCASTCOLIDX = 8;
    //private final static int BOOKMARKCOLIDX = 9;
    private BroadcastReceiver mUnmountReceiver = null;
    private WakeLock mWakeLock;
    private int mServiceStartId = -1;
    private boolean mServiceInUse = false;
    private boolean mIsSupposedToBePlaying = false;
    private boolean mQuietMode = false;
    private AudioManager mAudioManager;
    private boolean mQueueIsSaveable = true;
    // used to track what type of audio focus loss caused the playback to pause
    private boolean mPausedByTransientLossOfFocus = false;

    private SharedPreferences mPreferences;
    // We use this to distinguish between different cards when saving/restoring playlists.
    // This will have to change if we want to support multiple simultaneous cards.
    private int mCardId;

    // interval after which we stop the service when idle
    private static final int IDLE_DELAY = 60000;

    private RemoteControlClient mRemoteControlClient;

    // Added by Hai

    private WeakReference<RTMPSuck> mRTMPSuck;
    private int currentRtmpPort;

    private Date startRecordingTime;

    private File recordedFile;

    private Map<String, RadioProgram.Program> programMap = new HashMap<String, RadioProgram.Program>();

    private FFmpegMediaPlayer.OnRecordingListener recordingListener = new FFmpegMediaPlayer.OnRecordingListener() {
        @Override
        public void onCompleted(Channel channel, final int recordedSampleRate,
                                final int recordedChannel,
                                final int recordedAudioEncoding,
                                final int recordedBufferSize,String filePath) {
            if (filePath == null || filePath.length() == 0) {
                SimpleAppLog.error("Recoding could not be completed");
                return;
            }
            if (channel == null) return;
            final File mp3File = new File(filePath);
            Gson gson = new Gson();
            RecordedProgramDBAdapter adapter = new RecordedProgramDBAdapter(getApplicationContext());
            if (mp3File.exists()) {
                try {
                    RecordedProgram recordedProgram = new RecordedProgram();
                    recordedProgram.setChannelName(channel.getName() == null ? "" : channel.getName());
                    recordedProgram.setChannelKey(gson.toJson(channel));

                    if (programMap.containsKey(channel.getUrl().toLowerCase())) {
                        recordedProgram.setName(programMap.get(channel.getUrl().toLowerCase()).getTitle());
                    }

                    if (recordedProgram.getName() == null) {
                        recordedProgram.setName("");
                    }
                    recordedProgram.setStartTime(startRecordingTime);
                    long endTime = System.currentTimeMillis();
                    recordedProgram.setEndTime(new Date(endTime));
                    recordedProgram.setFilePath(mp3File.getPath());

                    adapter.open();
                    adapter.insert(recordedProgram);
                    SimpleAppLog.debug("Save recording complete");

                    Intent intent = new Intent(Constants.INTENT_FILTER_FRAGMENT_ACTION);
                    intent.putExtra(Constants.FRAGMENT_ACTION_TYPE, Constants.ACTION_RESET_FILTER_RECORDED_PROGRAM);
                    getApplicationContext().sendBroadcast(intent);
                } catch (Exception e) {
                    SimpleAppLog.error("Could not insert recorded program", e);
                } finally {
                    adapter.close();
                }
            }
        }
        @Override
        public void onError(String message, Throwable e) {
            SimpleAppLog.error(message, e);
        }
    };

    public void stopRecord() {
        if (isStreaming && mStreamingPlayer != null) {
            mStreamingPlayer.stopRecording();
        }
    }

    public void startRecord(String token, String filePath) {
        if (currentChannel == null) return;
        FileHelper fileHelper = new FileHelper(getApplicationContext());
        startRecordingTime = new Date(System.currentTimeMillis());
        recordedFile= new File(fileHelper.getRecordedProgramFolder(), currentChannel.getRecordedName() + "-" + startRecordingTime.getTime() + ".mp3");
        if (isStreaming && mStreamingPlayer != null)
            mStreamingPlayer.startRecording(currentChannel, recordedFile.getAbsolutePath());
    }

    public boolean isRecording() {
        if (isStreaming && mStreamingPlayer != null)
            return mStreamingPlayer.isRecording();
        return false;
    }

    public boolean isStreaming() {
        return isStreaming;
    }

    public void setStreaming(boolean isStreaming) {
        this.isStreaming = isStreaming;
    }

    private Channel currentChannel;

    public void setChannelObject(String channelObject) {
        if (channelObject != null && channelObject.length() > 0) {
            try {
                Gson gson = new Gson();
                currentChannel = gson.fromJson(channelObject, Channel.class);
                notifyChange(META_CHANGED);
            } catch (Exception e) {
                SimpleAppLog.error("Could not parse channel object", e);
            }
        } else {
            currentChannel = null;
            notifyChange(META_CHANGED);
        }
    }

    public void openStream(String token, String channelObject) {
        currentChannel = null;
        isStreaming = true;
        //mStreamingPlayer.setHandler(mMediaplayerHandler);
        setChannelObject(channelObject);

        if (mStreamingPlayer == null) {
            mStreamingPlayer = new MultiPlayer();
        }
        openStream(token);
    }

    private void openStream(String token) {
        if (currentChannel == null) return;

        currentChannel.setLastPlayedTime(new Date(System.currentTimeMillis()));
        ChannelDBAdapter dbAdapter = new ChannelDBAdapter(getApplicationContext());
        try {
            dbAdapter.open();
            dbAdapter.update(currentChannel);
        } catch (Exception e) {
            SimpleAppLog.error("Could not update channel last played time",e);
        } finally {
            dbAdapter.close();
        }
        mIsSupposedToBePlaying = true;
        notifyChange(META_CHANGED);
        final String playUrl = currentChannel.getUrl();

        if (playUrl.toLowerCase().startsWith("rtmpe://f-radiko.smartstream.ne.jp") && mRTMPSuck != null) {
            if (token == null || token.length() == 0) {
                TokenFetcher tokenFetcher = TokenFetcher.getTokenFetcher(getApplicationContext(), new TokenFetcher.OnTokenListener() {
                    @Override
                    public void onTokenFound(String token, String rawAreaId) {
                        currentAreaId = rawAreaId;
                        SimpleAppLog.info("Found token " + token + ". Area: " + rawAreaId);
                        openRadikoPlayUrl("S:" + token, playUrl);
                    }
                    @Override
                    public void onError(String message, Throwable throwable) {
                        SimpleAppLog.error(message, throwable);
                        isStreaming = false;
                    }
                });
                tokenFetcher.fetch();
            } else {
                if (!token.startsWith("S:")) {
                    token = "S:" + token;
                }
                openRadikoPlayUrl(token, playUrl);
            }
        } else {
            open(playUrl);
        }
    }

    private String updateRtmpSuck(String token, String playUrl) {
        try {
            String tcUrl = playUrl.substring(0, playUrl.lastIndexOf("/"));
            String app = tcUrl.substring("rtmpe://f-radiko.smartstream.ne.jp".length() + 1, tcUrl.length());
            SimpleAppLog.info("Update tcURL to " + tcUrl);
            SimpleAppLog.info("Update Radiko token to " + token);
            SimpleAppLog.info("Update app to " + app);
            mRTMPSuck.get().update(token, tcUrl, app);
            return "rtmp://127.0.0.1:" + currentRtmpPort + playUrl.substring("rtmpe://f-radiko.smartstream.ne.jp".length(), playUrl.length());
        } catch (Exception e) {
            SimpleAppLog.error("Could not create radiko stream url",e);
            return "";
        }
    }

    private void openRadikoPlayUrl(String token, String playUrl) {
        try {
            open(updateRtmpSuck(token, playUrl));
        } catch (Exception e) {
            SimpleAppLog.error("Could not open radiko stream",e);
            isStreaming = false;
        }
    }

    public String getChannelObject() {
        if (currentChannel != null) {
            Gson gson = new Gson();
            return gson.toJson(currentChannel);
        }
        return "";
    }

    public void doBack(int length) {
        if (isStreaming) return;
        if (mPlayer.isInitialized()) {
            long pos = mPlayer.position();
            long toBackPos = pos - length * 1000;
            if (toBackPos < 0) toBackPos = 0;
            seek(toBackPos);
        }
    }

    public static class ABState {
        public static final int ERROR = -1;
        public static final int STOP = 0;
        public static final int FLAG = 1;
        public static final int PLAY = 2;
    }

    private int stateAB = ABState.STOP;
    private long posA = -1;
    private long posB = -1;

    public long getPosA() {
        return posA;
    }

    public long getPosB() {
        return posB;
    }

    private int getStateAB() {
        if (isStreaming)
            return ABState.ERROR;
        return stateAB;
    }

    private void stopAB() {
        try {
            handlerCheckAB.removeCallbacks(checkABRunnable);
        } catch (Exception ex) {

        }
        stateAB = ABState.STOP;
        posA = -1;
        posB = -1;
    }

    private Runnable checkABRunnable = new Runnable() {
        @Override
        public void run() {
            checkAB();
            handlerCheckAB.postDelayed(checkABRunnable, 100);
        }
    };

    private Handler handlerCheckAB = new Handler();

    public void checkAB() {
        if (isStreaming)
            return;
        if (mPlayer.isInitialized() && isPlaying()) {
            if (stateAB == ABState.PLAY) {
                long currentPos = mPlayer.position();
                if (currentPos > posB) {
                    seek(posA);
                }
            }
        }
    }

    private void markAB() {
        if (isStreaming) return;
        if (mPlayer.isInitialized()) {
            if (stateAB == ABState.STOP) {
                posA = mPlayer.position();
                stateAB = ABState.FLAG;
            } else if (stateAB == ABState.FLAG) {
                posB = mPlayer.position();
                stateAB = ABState.PLAY;
                seek(posA);
                handlerCheckAB.post(checkABRunnable);
            } else if (stateAB == ABState.PLAY) {
                stateAB = ABState.STOP;
            }
        }
    }

    private Runnable fastRunnable = new Runnable() {
        private final Boolean flag = false;
        @Override
        public void run() {
            synchronized (flag) {
                if (!isStreaming) {
                    if (mPlayer.position() < mPlayer.duration() - 100) {
                        mPlayer.seek(mPlayer.position() + 100);
                        fastHandler.postDelayed(fastRunnable, 100);
                    }
                }
            }
        }
    };

    private Handler fastHandler = new Handler();

    private boolean isFast;

    private boolean isSlow;

    public boolean isFast() {
        return isFast;
    }

    public boolean isSlow() {
        return isSlow;
    }

    private void doFast(float level) {
        stopFast();
        stopSlow();
        //fastHandler.post(fastRunnable);
        if (!isStreaming && mPlayer.isInitialized()) {
            mPlayer.setPlaybackSpeed(level);
            isFast = true;
        }
    }

    private void stopFast() {
        isFast = false;
        if (isStreaming || !mPlayer.isInitialized()) return;
        try {
            //  fastHandler.removeCallbacks(fastRunnable);
            mPlayer.setPlaybackSpeed(1.0f);
        } catch (Exception ex) {

        }
    }

    private Runnable slowRunnable = new Runnable() {
        private final Boolean flag = false;
        private boolean isPause = false;
        @Override
        public void run() {
            synchronized (flag) {
                if (!isStreaming && mPlayer.isInitialized()) {
                    if (isPause) {
                        mPlayer.start();
                        isPause = false;
                    } else {
                        mPlayer.pause();
                        isPause = true;
                    }
                    slowHandler.postDelayed(slowRunnable, 100);
                }
            }
        }
    };

    private Handler slowHandler = new Handler();

    private void doSlow(float level) {
        stopFast();
        stopSlow();
        // slowHandler.post(slowRunnable);
        if (!isStreaming && mPlayer.isInitialized()) {
            mPlayer.setPlaybackSpeed(level);
            isSlow = true;
        }
    }

    private void stopSlow() {
        isSlow = false;
        try {
            mPlayer.setPlaybackSpeed(1.0f);
            //    slowHandler.removeCallbacks(slowRunnable);
        } catch (Exception ex) {

        }
    }

    private class CustomHandler extends Handler {
        float mCurrentVolume = 1.0f;
        @Override
        public void handleMessage(Message msg) {
            MusicUtils.debugLog("mMediaplayerHandler.handleMessage " + msg.what);
            switch (msg.what) {
                case FADEDOWN:
                    mCurrentVolume -= .05f;
                    if (mCurrentVolume > .2f) {
                        mMediaplayerHandler.sendEmptyMessageDelayed(FADEDOWN, 10);
                    } else {
                        mCurrentVolume = .2f;
                    }
                    if (isStreaming) {
                        //  if (mStreamingPlayer != null)
                        //  mStreamingPlayer.setVolume(mCurrentVolume);
                    } else {
                        //mPlayer.setVolume(mCurrentVolume);
                    }
                    break;
                case FADEUP:
                    mCurrentVolume += .01f;
                    if (mCurrentVolume < 1.0f) {
                        mMediaplayerHandler.sendEmptyMessageDelayed(FADEUP, 10);
                    } else {
                        mCurrentVolume = 1.0f;
                    }
                    if (isStreaming) {
                        //if (mStreamingPlayer != null)
                        //   mStreamingPlayer.setVolume(mCurrentVolume);
                    } else {
                        //mPlayer.setVolume(mCurrentVolume);
                    }
                    break;
                case SERVER_DIED:
                    if (mIsSupposedToBePlaying) {
                        gotoNext(true);
                    } else {
                        // the server died when we were idle, so just
                        // reopen the same song (it will start again
                        // from the beginning though when the user
                        // restarts)
                        openCurrentAndNext();
                    }
                    break;
                case TRACK_WENT_TO_NEXT:
                    mPlayPos = mNextPlayPos;
                    if (mCursor != null) {
                        mCursor.close();
                        mCursor = null;
                    }
                    mCursor = getCursorForId(mPlayList[mPlayPos]);
                    notifyChange(META_CHANGED);
                    updateNotification();
                    break;
                case TRACK_ENDED:
                    if (mRepeatMode == REPEAT_CURRENT) {
                        seek(0);
                        play();
                    } else {
                        gotoNext(false);
                    }
                    break;
                case RELEASE_WAKELOCK:
                    mWakeLock.release();
                    break;

                case FOCUSCHANGE:
                    // This code is here so we can better synchronize it with the code that
                    // handles fade-in
                    switch (msg.arg1) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                            Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_LOSS");
                            if(isPlaying()) {
                                mPausedByTransientLossOfFocus = false;
                            }
                            pause();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            mMediaplayerHandler.removeMessages(FADEUP);
                            mMediaplayerHandler.sendEmptyMessage(FADEDOWN);
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_LOSS_TRANSIENT");
                            if(isPlaying()) {
                                mPausedByTransientLossOfFocus = true;
                            }
                            pause();
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                            Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_GAIN");
                            if(!isPlaying() && mPausedByTransientLossOfFocus) {
                                mPausedByTransientLossOfFocus = false;
//                                mCurrentVolume = 0f;
//                                if (isStreaming)
//                                    mStreamingPlayer.setVolume(mCurrentVolume);
//                                else
//                                    mPlayer.setVolume(mCurrentVolume);
                                play(); // also queues a fade-in
                            } else {
                                mMediaplayerHandler.removeMessages(FADEDOWN);
                                mMediaplayerHandler.sendEmptyMessage(FADEUP);
                            }
                            break;
                        default:
                            Log.e(LOGTAG, "Unknown audio focus change code");
                    }
                    break;

                default:
                    break;
            }
        }
    }

    private Handler mMediaplayerHandler = new CustomHandler();

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");
            MusicUtils.debugLog("mIntentReceiver.onReceive " + action + " / " + cmd);
            if (CMDNEXT.equals(cmd) || NEXT_ACTION.equals(action)) {
                gotoNext(true);
            } else if (CMDPREVIOUS.equals(cmd) || PREVIOUS_ACTION.equals(action)) {
                prev();
            } else if (CMDTOGGLEPAUSE.equals(cmd) || TOGGLEPAUSE_ACTION.equals(action)) {
                if (isPlaying()) {
                    pause();
                    mPausedByTransientLossOfFocus = false;
                } else {
                    play();
                }
            } else if (CMDPAUSE.equals(cmd) || PAUSE_ACTION.equals(action)) {
                pause();
                mPausedByTransientLossOfFocus = false;
            } else if (CMDPLAY.equals(cmd)) {
                play();
            } else if (CMDSTOP.equals(cmd)) {
                pause();
                mPausedByTransientLossOfFocus = false;
                seek(0);
            }
        }
    };

    private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            mMediaplayerHandler.obtainMessage(FOCUSCHANGE, focusChange, 0).sendToTarget();
        }
    };

    public MediaPlaybackService() {
    }

    private String currentAreaId;

    private TokenFetcher.OnTokenListener onTokenListener = new TokenFetcher.OnTokenListener() {
        @Override
        public void onTokenFound(final String token, String rawAreaId) {
            SimpleAppLog.info("Found token: " + token);
            currentAreaId = rawAreaId;
        }
        @Override
        public void onError(final String message, Throwable throwable) {
        }
    };

    private void initRtmpSuck() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mRTMPSuck != null) {
                        final RTMPSuck rtmpSuck = mRTMPSuck.get();
                        if (rtmpSuck != null) {
                            rtmpSuck.stop();
                        }
                    }
                    SimpleAppLog.info("Force start RTMPSuck");
                    int count = 0;
                    String ip = "127.0.0.1";
                    currentRtmpPort = 1935;
                    while(InetHelper.isPortOpen(ip, currentRtmpPort, 300)) {
                        currentRtmpPort += new Random().nextInt(10);
                        SimpleAppLog.info("Try to test an other port " + currentRtmpPort);
                        count++;
                        if (count > 5);
                    }
                    RTMPSuck rtmpSuck = new RTMPSuck();
                    mRTMPSuck = new WeakReference<RTMPSuck>(rtmpSuck);
                    mRTMPSuck.get().init("S:", currentRtmpPort);
                } catch (Exception ex) {
                    SimpleAppLog.error("Could not start RTMPSuck",ex);
                }
            }
        }).start();
    }


    @Override
    public void onCreate() {
        super.onCreate();
        initRtmpSuck();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        ComponentName rec = new ComponentName(getPackageName(),
                MediaButtonIntentReceiver.class.getName());
        mAudioManager.registerMediaButtonEventReceiver(rec);
        // TODO update to new constructor
//        mRemoteControlClient = new RemoteControlClient(rec);
//        mAudioManager.registerRemoteControlClient(mRemoteControlClient);
//
//        int flags = RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
//                | RemoteControlClient.FLAG_KEY_MEDIA_NEXT
//                | RemoteControlClient.FLAG_KEY_MEDIA_PLAY
//                | RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
//                | RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
//                | RemoteControlClient.FLAG_KEY_MEDIA_STOP;
//        mRemoteControlClient.setTransportControlFlags(flags);

        mPreferences = getSharedPreferences("Music", MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE);
        mCardId = MusicUtils.getCardId(this);

        registerExternalStorageListener();

        // Needs to be done in this thread, since otherwise ApplicationContext.getPowerManager() crashes.
        mPlayer = new CustomMultiPlayer();
        mPlayer.setHandler(mMediaplayerHandler);

        mStreamingPlayer = new MultiPlayer();
        //mStreamingPlayer.setHandler(mMediaplayerHandler);

        TokenFetcher tokenFetcher = TokenFetcher.getTokenFetcher(getApplicationContext(), onTokenListener);
        tokenFetcher.fetch();

        reloadQueue();
        notifyChange(QUEUE_CHANGED);
        notifyChange(META_CHANGED);

        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(SERVICECMD);
        commandFilter.addAction(TOGGLEPAUSE_ACTION);
        commandFilter.addAction(PAUSE_ACTION);
        commandFilter.addAction(NEXT_ACTION);
        commandFilter.addAction(PREVIOUS_ACTION);
        registerReceiver(mIntentReceiver, commandFilter);

        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
        mWakeLock.setReferenceCounted(false);

        // If the service was idle, but got killed before it stopped itself, the
        // system will relaunch it. Make sure it gets stopped again in that case.
        Message msg = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
    }

    @Override
    public void onDestroy() {
        // Check that we're not being destroyed while something is still playing.
        if (isPlaying()) {
            Log.e(LOGTAG, "Service being destroyed while still playing.");
        }
        if (mRTMPSuck != null) {
            try {
                final RTMPSuck rtmpSuck = mRTMPSuck.get();
                if (rtmpSuck != null) {
                    try {
                        rtmpSuck.stop();
                    } catch (Exception ex) {
                        //
                    }
                    mRTMPSuck = null;
                }
            } catch (Exception ex) {

            }
        }


        // release all MediaPlayer resources, including the native player and wakelocks
        Intent i = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
        i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
        sendBroadcast(i);
        try {
            mPlayer.release();
            mPlayer = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            mStreamingPlayer.release();
            mStreamingPlayer = null;
        } catch (Exception e) {
            e.printStackTrace();
        }

        mAudioManager.abandonAudioFocus(mAudioFocusListener);
        //mAudioManager.unregisterRemoteControlClient(mRemoteControlClient);

        // make sure there aren't any other messages coming
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mMediaplayerHandler.removeCallbacksAndMessages(null);

        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }

        unregisterReceiver(mIntentReceiver);
        if (mUnmountReceiver != null) {
            unregisterReceiver(mUnmountReceiver);
            mUnmountReceiver = null;
        }
        mWakeLock.release();
        super.onDestroy();
    }

    private final char hexdigits [] = new char [] {
            '0', '1', '2', '3',
            '4', '5', '6', '7',
            '8', '9', 'a', 'b',
            'c', 'd', 'e', 'f'
    };

    private void saveQueue(boolean full) {
        if (!mQueueIsSaveable) {
            return;
        }

        Editor ed = mPreferences.edit();
        //long start = System.currentTimeMillis();
        if (full) {
            StringBuilder q = new StringBuilder();

            // The current playlist is saved as a list of "reverse hexadecimal"
            // numbers, which we can generate faster than normal decimal or
            // hexadecimal numbers, which in turn allows us to save the playlist
            // more often without worrying too much about performance.
            // (saving the full state takes about 40 ms under no-load conditions
            // on the phone)
            int len = mPlayListLen;
            for (int i = 0; i < len; i++) {
                long n = mPlayList[i];
                if (n < 0) {
                    continue;
                } else if (n == 0) {
                    q.append("0;");
                } else {
                    while (n != 0) {
                        int digit = (int)(n & 0xf);
                        n >>>= 4;
                        q.append(hexdigits[digit]);
                    }
                    q.append(";");
                }
            }
            //Log.i("@@@@ service", "created queue string in " + (System.currentTimeMillis() - start) + " ms");
            ed.putString("queue", q.toString());
            ed.putInt("cardid", mCardId);
            if (mShuffleMode != SHUFFLE_NONE) {
                // In shuffle mode we need to save the history too
                len = mHistory.size();
                q.setLength(0);
                for (int i = 0; i < len; i++) {
                    int n = mHistory.get(i);
                    if (n == 0) {
                        q.append("0;");
                    } else {
                        while (n != 0) {
                            int digit = (n & 0xf);
                            n >>>= 4;
                            q.append(hexdigits[digit]);
                        }
                        q.append(";");
                    }
                }
                ed.putString("history", q.toString());
            }
        }
        ed.putInt("curpos", mPlayPos);
        if (isStreaming && mStreamingPlayer != null && mStreamingPlayer.isInitialized()) {
            ed.putLong("seekpos", mStreamingPlayer.position());
        }
        if (!isStreaming && mPlayer.isInitialized()) {
            ed.putLong("seekpos", mPlayer.position());
        }
        ed.putInt("repeatmode", mRepeatMode);
        ed.putInt("shufflemode", mShuffleMode);
        //TODO fix this
        //SharedPreferencesCompat.apply(ed);

        //Log.i("@@@@ service", "saved state in " + (System.currentTimeMillis() - start) + " ms");
    }

    private void reloadQueue() {
        String q = null;

        boolean newstyle = false;
        int id = mCardId;
        if (mPreferences.contains("cardid")) {
            newstyle = true;
            id = mPreferences.getInt("cardid", ~mCardId);
        }
        if (id == mCardId) {
            // Only restore the saved playlist if the card is still
            // the same one as when the playlist was saved
            q = mPreferences.getString("queue", "");
        }
        int qlen = q != null ? q.length() : 0;
        if (qlen > 1) {
            //Log.i("@@@@ service", "loaded queue: " + q);
            int plen = 0;
            int n = 0;
            int shift = 0;
            for (int i = 0; i < qlen; i++) {
                char c = q.charAt(i);
                if (c == ';') {
                    ensurePlayListCapacity(plen + 1);
                    mPlayList[plen] = n;
                    plen++;
                    n = 0;
                    shift = 0;
                } else {
                    if (c >= '0' && c <= '9') {
                        n += ((c - '0') << shift);
                    } else if (c >= 'a' && c <= 'f') {
                        n += ((10 + c - 'a') << shift);
                    } else {
                        // bogus playlist data
                        plen = 0;
                        break;
                    }
                    shift += 4;
                }
            }
            mPlayListLen = plen;

            int pos = mPreferences.getInt("curpos", 0);
            if (pos < 0 || pos >= mPlayListLen) {
                // The saved playlist is bogus, discard it
                mPlayListLen = 0;
                return;
            }
            mPlayPos = pos;

            // When reloadQueue is called in response to a card-insertion,
            // we might not be able to query the media provider right away.
            // To deal with this, try querying for the current file, and if
            // that fails, wait a while and try again. If that too fails,
            // assume there is a problem and don't restore the state.
            Cursor crsr = MusicUtils.query(this,
                    Media.MediaColumns.CONTENT_URI,
                    new String [] {"_id"}, "_id=" + mPlayList[mPlayPos] , null, null);
            if (crsr == null || crsr.getCount() == 0) {
                // wait a bit and try again
                SystemClock.sleep(3000);
                crsr = getContentResolver().query(
                        Media.MediaColumns.CONTENT_URI,
                        mCursorCols, "_id=" + mPlayList[mPlayPos] , null, null);
            }
            if (crsr != null) {
                crsr.close();
            }

            // Make sure we don't auto-skip to the next song, since that
            // also starts playback. What could happen in that case is:
            // - music is paused
            // - go to UMS and delete some files, including the currently playing one
            // - come back from UMS
            // (time passes)
            // - music app is killed for some reason (out of memory)
            // - music service is restarted, service restores state, doesn't find
            //   the "current" file, goes to the next and: playback starts on its
            //   own, potentially at some random inconvenient time.
            mOpenFailedCounter = 20;
            mQuietMode = true;
            openCurrentAndNext();
            mQuietMode = false;
            if (isStreaming && mStreamingPlayer != null && !mStreamingPlayer.isInitialized()) {
                mPlayListLen = 0;
            }
            if (!isStreaming && !mPlayer.isInitialized()) {
                // couldn't restore the saved state
                mPlayListLen = 0;
                return;
            }

            long seekpos = mPreferences.getLong("seekpos", 0);
            seek(seekpos >= 0 && seekpos < duration() ? seekpos : 0);
            Log.d(LOGTAG, "restored queue, currently at position "
                    + position() + "/" + duration()
                    + " (requested " + seekpos + ")");

            int repmode = mPreferences.getInt("repeatmode", REPEAT_NONE);
            if (repmode != REPEAT_ALL && repmode != REPEAT_CURRENT) {
                repmode = REPEAT_NONE;
            }
            mRepeatMode = repmode;

            int shufmode = mPreferences.getInt("shufflemode", SHUFFLE_NONE);
            if (shufmode != SHUFFLE_AUTO && shufmode != SHUFFLE_NORMAL) {
                shufmode = SHUFFLE_NONE;
            }
            if (shufmode != SHUFFLE_NONE) {
                // in shuffle mode we need to restore the history too
                q = mPreferences.getString("history", "");
                qlen = q != null ? q.length() : 0;
                if (qlen > 1) {
                    plen = 0;
                    n = 0;
                    shift = 0;
                    mHistory.clear();
                    for (int i = 0; i < qlen; i++) {
                        char c = q.charAt(i);
                        if (c == ';') {
                            if (n >= mPlayListLen) {
                                // bogus history data
                                mHistory.clear();
                                break;
                            }
                            mHistory.add(n);
                            n = 0;
                            shift = 0;
                        } else {
                            if (c >= '0' && c <= '9') {
                                n += ((c - '0') << shift);
                            } else if (c >= 'a' && c <= 'f') {
                                n += ((10 + c - 'a') << shift);
                            } else {
                                // bogus history data
                                mHistory.clear();
                                break;
                            }
                            shift += 4;
                        }
                    }
                }
            }
            if (shufmode == SHUFFLE_AUTO) {
                if (! makeAutoShuffleList()) {
                    shufmode = SHUFFLE_NONE;
                }
            }
            mShuffleMode = shufmode;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mServiceInUse = true;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mServiceInUse = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceStartId = startId;
        mDelayedStopHandler.removeCallbacksAndMessages(null);

        if (intent != null) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");
            MusicUtils.debugLog("onStartCommand " + action + " / " + cmd);

            if (CMDNEXT.equals(cmd) || NEXT_ACTION.equals(action)) {
                gotoNext(true);
            } else if (CMDPREVIOUS.equals(cmd) || PREVIOUS_ACTION.equals(action)) {
                if (position() < 2000) {
                    prev();
                } else {
                    seek(0);
                    play();
                }
            } else if (CMDTOGGLEPAUSE.equals(cmd) || TOGGLEPAUSE_ACTION.equals(action)) {
                if (isPlaying()) {
                    pause();
                    mPausedByTransientLossOfFocus = false;
                } else {
                    play();
                }
            } else if (CMDPAUSE.equals(cmd) || PAUSE_ACTION.equals(action)) {
                pause();
                mPausedByTransientLossOfFocus = false;
            } else if (CMDPLAY.equals(cmd)) {
                play();
            } else if (CMDSTOP.equals(cmd)) {
                pause();
                mPausedByTransientLossOfFocus = false;
                seek(0);
            }
        }

        // make sure the service will shut down on its own if it was
        // just started but not bound to and nothing is playing
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        Message msg = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
        return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mServiceInUse = false;

        // Take a snapshot of the current playlist
        saveQueue(true);

        if (isPlaying() || isStreaming || mPausedByTransientLossOfFocus) {
            // something is currently playing, or will be playing once 
            // an in-progress action requesting audio focus ends, so don't stop the service now.
            return true;
        }

        // If there is a playlist but playback is paused, then wait a while
        // before stopping the service, so that pause/resume isn't slow.
        // Also delay stopping the service if we're transitioning between tracks.
        if (mPlayListLen > 0  || mMediaplayerHandler.hasMessages(TRACK_ENDED)) {
            Message msg = mDelayedStopHandler.obtainMessage();
            mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
            return true;
        }

        // No active playlist, OK to stop the service right now
        stopSelf(mServiceStartId);
        return true;
    }

    private Handler mDelayedStopHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // Check again to make sure nothing is playing right now
            if (isPlaying() || isStreaming || mPausedByTransientLossOfFocus || mServiceInUse
                    || mMediaplayerHandler.hasMessages(TRACK_ENDED)) {
                return;
            }
            // save the queue again, because it might have changed
            // since the user exited the music app (because of
            // party-shuffle or because the play-position changed)
            saveQueue(true);
            stopSelf(mServiceStartId);
        }
    };

    /**
     * Called when we receive a ACTION_MEDIA_EJECT notification.
     *
     * @param storagePath path to mount point for the removed media
     */
    public void closeExternalStorageFiles(String storagePath) {
        // stop playback and clean up if the SD card is going to be unmounted.
        stop(true);
        notifyChange(QUEUE_CHANGED);
        notifyChange(META_CHANGED);
    }

    /**
     * Registers an intent to listen for ACTION_MEDIA_EJECT notifications.
     * The intent will call closeExternalStorageFiles() if the external media
     * is going to be ejected, so applications can clean up any files they have open.
     */
    public void registerExternalStorageListener() {
        if (mUnmountReceiver == null) {
            mUnmountReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                        saveQueue(true);
                        mQueueIsSaveable = false;
                        closeExternalStorageFiles(intent.getData().getPath());
                    } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                        mMediaMountedCount++;
                        mCardId = MusicUtils.getCardId(MediaPlaybackService.this);
                        reloadQueue();
                        mQueueIsSaveable = true;
                        notifyChange(QUEUE_CHANGED);
                        notifyChange(META_CHANGED);
                    }
                }
            };
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(Intent.ACTION_MEDIA_EJECT);
            iFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            iFilter.addDataScheme("file");
            registerReceiver(mUnmountReceiver, iFilter);
        }
    }

    /**
     * Notify the change-receivers that something has changed.
     * The intent that is sent contains the following data
     * for the currently playing track:
     * "id" - Integer: the database row ID
     * "artist" - String: the name of the artist
     * "album" - String: the name of the album
     * "track" - String: the name of the track
     * The intent has an action that is one of
     * "com.android.music.metachanged"
     * "com.android.music.queuechanged",
     * "com.android.music.playbackcomplete"
     * "com.android.music.playstatechanged"
     * respectively indicating that a new track has
     * started playing, that the playback queue has
     * changed, that playback has stopped because
     * the last file in the list has been played,
     * or that the play-state changed (paused/resumed).
     */
    private void notifyChange(String what) {

        Intent i = new Intent(what);
        i.putExtra("id", Long.valueOf(getAudioId()));
        i.putExtra("artist", getArtistName());
        i.putExtra("album",getAlbumName());
        i.putExtra("track", getTrackName());
        i.putExtra("playing", isPlaying());
        sendStickyBroadcast(i);

        if (what.equals(PLAYSTATE_CHANGED)) {
//            mRemoteControlClient.setPlaybackState(isPlaying() ?
//                    RemoteControlClient.PLAYSTATE_PLAYING : RemoteControlClient.PLAYSTATE_PAUSED);
        } else if (what.equals(META_CHANGED)) {
//            RemoteControlClient.MetadataEditor ed = mRemoteControlClient.editMetadata(true);
//            ed.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, getTrackName());
//            ed.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, getAlbumName());
//            ed.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, getArtistName());
//            ed.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, duration());
//            Bitmap b = MusicUtils.getArtwork(this, getAudioId(), getAlbumId(), false);
//            if (b != null) {
//                ed.putBitmap(MetadataEditor.BITMAP_KEY_ARTWORK, b);
//            }
//            ed.apply();
        }

        if (what.equals(QUEUE_CHANGED)) {
            saveQueue(true);
        } else {
            saveQueue(false);
        }
    }

    private void ensurePlayListCapacity(int size) {
        if (mPlayList == null || size > mPlayList.length) {
            // reallocate at 2x requested size so we don't
            // need to grow and copy the array for every
            // insert
            long [] newlist = new long[size * 2];
            int len = mPlayList != null ? mPlayList.length : mPlayListLen;
            for (int i = 0; i < len; i++) {
                newlist[i] = mPlayList[i];
            }
            mPlayList = newlist;
        }
        // FIXME: shrink the array when the needed size is much smaller
        // than the allocated size
    }

    // insert the list of songs at the specified position in the playlist
    private void addToPlayList(long [] list, int position) {
        int addlen = list.length;
        if (position < 0) { // overwrite
            mPlayListLen = 0;
            position = 0;
        }
        ensurePlayListCapacity(mPlayListLen + addlen);
        if (position > mPlayListLen) {
            position = mPlayListLen;
        }

        // move part of list after insertion point
        int tailsize = mPlayListLen - position;
        for (int i = tailsize ; i > 0 ; i--) {
            mPlayList[position + i] = mPlayList[position + i - addlen];
        }

        // copy list into playlist
        for (int i = 0; i < addlen; i++) {
            mPlayList[position + i] = list[i];
        }
        mPlayListLen += addlen;
        if (mPlayListLen == 0) {
            mCursor.close();
            mCursor = null;
            notifyChange(META_CHANGED);
        }
    }

    /**
     * Appends a list of tracks to the current playlist.
     * If nothing is playing currently, playback will be started at
     * the first track.
     * If the action is NOW, playback will switch to the first of
     * the new tracks immediately.
     * @param list The list of tracks to append.
     * @param action NOW, NEXT or LAST
     */
    public void enqueue(long [] list, int action) {
        synchronized(this) {
            if (action == NEXT && mPlayPos + 1 < mPlayListLen) {
                addToPlayList(list, mPlayPos + 1);
                notifyChange(QUEUE_CHANGED);
            } else {
                // action == LAST || action == NOW || mPlayPos + 1 == mPlayListLen
                addToPlayList(list, Integer.MAX_VALUE);
                notifyChange(QUEUE_CHANGED);
                if (action == NOW) {
                    mPlayPos = mPlayListLen - list.length;
                    openCurrentAndNext();
                    play();
                    notifyChange(META_CHANGED);
                    return;
                }
            }
            if (mPlayPos < 0) {
                mPlayPos = 0;
                openCurrentAndNext();
                play();
                notifyChange(META_CHANGED);
            }
        }
    }

    /**
     * Replaces the current playlist with a new list,
     * and prepares for starting playback at the specified
     * position in the list, or a random position if the
     * specified position is 0.
     * @param list The new list of tracks.
     */
    public void open(long [] list, int position) {
        synchronized (this) {
            if (mShuffleMode == SHUFFLE_AUTO) {
                mShuffleMode = SHUFFLE_NORMAL;
            }
            long oldId = getAudioId();
            int listlength = list.length;
            boolean newlist = true;
            if (mPlayListLen == listlength) {
                // possible fast path: list might be the same
                newlist = false;
                for (int i = 0; i < listlength; i++) {
                    if (list[i] != mPlayList[i]) {
                        newlist = true;
                        break;
                    }
                }
            }
            if (newlist) {
                addToPlayList(list, -1);
                notifyChange(QUEUE_CHANGED);
            }
            int oldpos = mPlayPos;
            if (position >= 0) {
                mPlayPos = position;
            } else {
                mPlayPos = mRand.nextInt(mPlayListLen);
            }
            mHistory.clear();

            saveBookmarkIfNeeded();
            openCurrentAndNext();
            if (oldId != getAudioId()) {
                notifyChange(META_CHANGED);
            }
        }
    }

    /**
     * Moves the item at index1 to index2.
     * @param index1
     * @param index2
     */
    public void moveQueueItem(int index1, int index2) {
        synchronized (this) {
            if (index1 >= mPlayListLen) {
                index1 = mPlayListLen - 1;
            }
            if (index2 >= mPlayListLen) {
                index2 = mPlayListLen - 1;
            }
            if (index1 < index2) {
                long tmp = mPlayList[index1];
                for (int i = index1; i < index2; i++) {
                    mPlayList[i] = mPlayList[i+1];
                }
                mPlayList[index2] = tmp;
                if (mPlayPos == index1) {
                    mPlayPos = index2;
                } else if (mPlayPos >= index1 && mPlayPos <= index2) {
                    mPlayPos--;
                }
            } else if (index2 < index1) {
                long tmp = mPlayList[index1];
                for (int i = index1; i > index2; i--) {
                    mPlayList[i] = mPlayList[i-1];
                }
                mPlayList[index2] = tmp;
                if (mPlayPos == index1) {
                    mPlayPos = index2;
                } else if (mPlayPos >= index2 && mPlayPos <= index1) {
                    mPlayPos++;
                }
            }
            notifyChange(QUEUE_CHANGED);
        }
    }

    /**
     * Returns the current play list
     * @return An array of integers containing the IDs of the tracks in the play list
     */
    public long [] getQueue() {
        synchronized (this) {
            int len = mPlayListLen;
            long [] list = new long[len];
            for (int i = 0; i < len; i++) {
                list[i] = mPlayList[i];
            }
            return list;
        }
    }

    private Cursor getCursorForId(long lid) {
        String id = String.valueOf(lid);

        Cursor c = getContentResolver().query(
                Media.MediaColumns.CONTENT_URI,
                mCursorCols, "_id=" + id , null, null);
        if (c != null) {
            c.moveToFirst();
        }
        return c;
    }

    private void openCurrentAndNext() {
        synchronized (this) {
            currentChannel = null;
            if (mCursor != null) {
                mCursor.close();
                mCursor = null;
            }

            if (mPlayListLen == 0) {
                return;
            }
            stop(false);
            mCursor = getCursorForId(mPlayList[mPlayPos]);
            while(true) {
                if (mCursor != null && mCursor.getCount() != 0 &&
                        //open(Media.MediaColumns.CONTENT_URI + "/" +
                        //        mCursor.getLong(IDCOLIDX))) {
                        open(mCursor.getString(mCursor.getColumnIndex(Media.MediaColumns.URI)))) {
                    break;
                }
                // if we get here then opening the file failed. We can close the cursor now, because
                // we're either going to create a new one next, or stop trying
                if (mCursor != null) {
                    mCursor.close();
                    mCursor = null;
                }
                if (mOpenFailedCounter++ < 10 &&  mPlayListLen > 1) {
                    int pos = getNextPosition(false);
                    if (pos < 0) {
                        gotoIdleState();
                        if (mIsSupposedToBePlaying) {
                            mIsSupposedToBePlaying = false;
                            notifyChange(PLAYSTATE_CHANGED);
                        }
                        return;
                    }
                    mPlayPos = pos;
                    stop(false);
                    mPlayPos = pos;
                    mCursor = getCursorForId(mPlayList[mPlayPos]);
                } else {
                    mOpenFailedCounter = 0;
                    if (!mQuietMode) {
                        Toast.makeText(this, R.string.playback_failed, Toast.LENGTH_SHORT).show();
                    }
                    Log.d(LOGTAG, "Failed to open file for playback");
                    gotoIdleState();
                    if (mIsSupposedToBePlaying) {
                        mIsSupposedToBePlaying = false;
                        notifyChange(PLAYSTATE_CHANGED);
                    }
                    return;
                }
            }

            // go to bookmark if needed
            if (isPodcast()) {
                long bookmark = getBookmark();
                // Start playing a little bit before the bookmark,
                // so it's easier to get back in to the narrative.
                seek(bookmark - 5000);
            }
        }
    }

    /**
     * Opens the specified file and readies it for playback.
     *
     * @param path The full path of the file to be opened.
     */
    public boolean open(String path) {
        synchronized (this) {
            if (path == null) {
                return false;
            }

            // if mCursor is null, try to associate path with a database cursor
            /*if (mCursor == null) {

                ContentResolver resolver = getContentResolver();
                Uri uri;
                String where;
                String selectionArgs[];
                if (path.startsWith("content://media/")) {
                    uri = Uri.parse(path);
                    where = null;
                    selectionArgs = null;
                } else {
                   uri = MediaStore.Audio.Media.getContentUriForPath(path);
                   where = MediaStore.Audio.Media.DATA + "=?";
                   selectionArgs = new String[] { path };
                }
                
                try {
                    mCursor = resolver.query(uri, mCursorCols, where, selectionArgs, null);
                    if  (mCursor != null) {
                        if (mCursor.getCount() == 0) {
                            mCursor.close();
                            mCursor = null;
                        } else {
                            mCursor.moveToNext();
                            ensurePlayListCapacity(1);
                            mPlayListLen = 1;
                            mPlayList[0] = mCursor.getLong(IDCOLIDX);
                            mPlayPos = 0;
                        }
                    }
                } catch (UnsupportedOperationException ex) {
                }
            }*/
            if (path.contains("|")) {
                final String[] tmp = path.split("\\|");
                mFileToPlay = tmp[0];
            } else {
                mFileToPlay = path;
            }
            SimpleAppLog.info("Play: " + mFileToPlay);
            if (isStreaming) {
                if (mStreamingPlayer != null) {
                    mStreamingPlayer.setDataSource(mFileToPlay);
                    if (mStreamingPlayer.isInitialized()) {
                        mOpenFailedCounter = 0;
                        return true;
                    }
                }
            } else {
                RecordedProgramDBAdapter dbAdapter = new RecordedProgramDBAdapter(getApplicationContext());
                String programInfo = "";
                try {
                    dbAdapter.open();
                    RecordedProgram program = dbAdapter.findByFilePath(mFileToPlay);
                    if (program != null) {
                        String channelSrc = program.getChannelKey();
                        Channel channel = null;
                        Gson gson = new Gson();
                        SimpleAppLog.info("Channel source: " + channelSrc);
                        if (channelSrc != null && channelSrc.length() > 0) {
                            try {
                                channel = gson.fromJson(channelSrc, Channel.class);
                            } catch (Exception e) {
                                SimpleAppLog.error("Could not parse channel", e);
                            }
                        }
                        if (channel != null) {
                            if (channel.getCurrentProgram() != null) {
                                channel.getCurrentProgram().setFromTime(program.getStartTime().getTime());
                                channel.getCurrentProgram().setToTime(program.getEndTime().getTime());
                            }
                            programInfo = gson.toJson(channel);
                        }

                    } else {
                        SimpleAppLog.info("Could not found program info");
                    }
                } catch (Exception ex) {
                    SimpleAppLog.error("Could not get recorded program info",ex);
                } finally {
                    dbAdapter.close();
                    setChannelObject(programInfo);
                }
                mPlayer.setDataSource(mFileToPlay);
                if (mPlayer.isInitialized()) {
                    mOpenFailedCounter = 0;
                    return true;
                }
            }
            stop(true);
            return false;
        }
    }

    /**
     * Opens the specified file and readies it for playback.
     *
     * @param path The full path of the file to be opened.
     */
    /*public boolean open(String path) {
        synchronized (this) {
            if (path == null) {
                return false;
            }
            
            // if mCursor is null, try to associate path with a database cursor
            if (mCursor == null) {

                ContentResolver resolver = getContentResolver();
                Uri uri;
                String where;
                String selectionArgs[];
                if (path.startsWith("content://media/")) {
                    uri = Uri.parse(path);
                    where = null;
                    selectionArgs = null;
                } else {
                   uri = MediaStore.Audio.Media.getContentUriForPath(path);
                   where = MediaStore.Audio.Media.DATA + "=?";
                   selectionArgs = new String[] { path };
                }
                
                try {
                    mCursor = resolver.query(uri, mCursorCols, where, selectionArgs, null);
                    if  (mCursor != null) {
                        if (mCursor.getCount() == 0) {
                            mCursor.close();
                            mCursor = null;
                        } else {
                            mCursor.moveToNext();
                            ensurePlayListCapacity(1);
                            mPlayListLen = 1;
                            mPlayList[0] = mCursor.getLong(IDCOLIDX);
                            mPlayPos = 0;
                        }
                    }
                } catch (UnsupportedOperationException ex) {
                }
            }
            mFileToPlay = path;
            mPlayer.setDataSource(mFileToPlay);
            if (mPlayer.isInitialized()) {
                mOpenFailedCounter = 0;
                return true;
            }
            stop(true);
            return false;
        }
    }*/

    /**
     * Starts playback of a previously opened file.
     */
    public void play() {
        mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        mAudioManager.registerMediaButtonEventReceiver(new ComponentName(this.getPackageName(),
                MediaButtonIntentReceiver.class.getName()));
        if (isStreaming) {
            if (mStreamingPlayer != null && mStreamingPlayer.isInitialized()) {
                // if we are at the end of the song, go to the next song first

                mStreamingPlayer.start();
                // make sure we fade in, in case a previous fadein was stopped because
                // of another focus loss
                mMediaplayerHandler.removeMessages(FADEDOWN);
                mMediaplayerHandler.sendEmptyMessage(FADEUP);

                updateNotification();
                handlerUpdateNotification.removeCallbacks(runnableUpdateNotification);
                handlerUpdateNotification.postDelayed(runnableUpdateNotification, 60 * 1000);

                if (!mIsSupposedToBePlaying) {
                    mIsSupposedToBePlaying = true;
                    notifyChange(PLAYSTATE_CHANGED);
                }

            } else if (mPlayListLen <= 0) {
                // This is mostly so that if you press 'play' on a bluetooth headset
                // without every having played anything before, it will still play
                // something.
                setShuffleMode(SHUFFLE_AUTO);
            }
        } else {
            if (mPlayer.isInitialized()) {
                // if we are at the end of the song, go to the next song first
//                long duration = mPlayer.duration();
//                if (mRepeatMode != REPEAT_CURRENT && duration > 2000 &&
//                        mPlayer.position() >= duration - 2000) {
//                    gotoNext(true);
//                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mPlayer.start();
                    }
                }, 200);
                // make sure we fade in, in case a previous fadein was stopped because
                // of another focus loss
                mMediaplayerHandler.removeMessages(FADEDOWN);
                mMediaplayerHandler.sendEmptyMessage(FADEUP);

                updateNotification();
                if (!mIsSupposedToBePlaying) {
                    mIsSupposedToBePlaying = true;
                    notifyChange(PLAYSTATE_CHANGED);
                }

            } else if (mPlayListLen <= 0) {
                // This is mostly so that if you press 'play' on a bluetooth headset
                // without every having played anything before, it will still play
                // something.
                setShuffleMode(SHUFFLE_AUTO);
            }
        }
    }

    private void updateCurrentProgram(final Channel channel, final String rawAreaId) {
        if (isStreaming) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    boolean done = false;
                    try {
                        APIRequester requester = new APIRequester(new FileHelper(getApplicationContext()).getApiCachedFolder());
                        RadioChannel.Channel rChannel = new RadioChannel.Channel();
                        rChannel.setName(currentChannel.getName());
                        rChannel.setService(currentChannel.getType());
                        rChannel.setServiceChannelId(currentChannel.getUrl());
                        rChannel.setServiceChannelId(currentChannel.getKey());

                        requester.setRequesterListener(new APIRequester.RequesterListener() {
                            @Override
                            public void onMessage(String message) {
                                SimpleAppLog.info(message);
                            }

                            @Override
                            public void onError(String error, Throwable throwable) {
                                SimpleAppLog.error(error, throwable);
                            }
                        });
                        DataPrepareService prepareService = new DataPrepareService(getApplicationContext(), null);
                        String areaId = prepareService.findBestAreaId(rawAreaId);

                        done = validateChannelProgram(requester.getPrograms(rChannel,
                                RadioArea.getArea(areaId, currentChannel.getType()),
                                AndroidUtil.getAdsId(getApplicationContext())));

                        if (!done) {
                            SimpleAppLog.info("Try fetch program again #1");
                            requester.resetDate();
                            requester.addDay(-1);
                            done = validateChannelProgram(requester.getPrograms(rChannel,
                                    RadioArea.getArea(areaId, currentChannel.getType()),
                                    AndroidUtil.getAdsId(getApplicationContext())));
                        }

                        if (!done) {
                            SimpleAppLog.info("Try fetch program again #2");
                            requester.resetDate();
                            if (rChannel.getService().equalsIgnoreCase(RadioProvider.NHK))
                                requester.addDay(1);
                            done = validateChannelProgram(requester.getPrograms(rChannel,
                                    RadioArea.getArea(areaId, currentChannel.getType()),
                                    AndroidUtil.getAdsId(getApplicationContext())));
                        }

                    } catch (Exception e) {
                        SimpleAppLog.error("Could not fetch programs", e);
                    }
                    if (!done) {
                        showServiceNotification(currentChannel.getName(), "");
                    }
                    return null;
                }
            }.execute();
        } else {
            RadioProgram.Program program = currentChannel.getCurrentProgram();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.JAPANESE);
            final StringBuffer sb = new StringBuffer("");
            if (program != null) {
                sb.append(program.getTitle()).append("\n");
                sb.append(sdf.format(new Date(program.getFromTime())));
                sb.append(" - ").append(sdf.format(new Date(program.getToTime())));
            }
            showServiceNotification(currentChannel.getName(), sb.toString());
        }
    }

    private boolean validateChannelProgram(RadioProgram radioProgram) {
        if (radioProgram != null) {
            List<RadioProgram.Program> programList = radioProgram.getPrograms();
            SimpleDateFormat sdfT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            if (programList != null && programList.size() > 0) {
                for (RadioProgram.Program program : programList) {
                    TimeZone tz = TimeZone.getTimeZone("GMT+09");
                    Calendar calNow = Calendar.getInstance(tz);
                    Calendar calFrom = Calendar.getInstance(tz);
                    calFrom.setTimeInMillis(program.getFromTime());
                    Calendar calTo = Calendar.getInstance(tz);
                    calTo.setTimeInMillis(program.getToTime());

                    SimpleAppLog.info("Found program: " + program.getTitle()
                            +". Start time: " + sdfT.format(calFrom.getTime())
                            + ". End time: " + sdfT.format(calTo.getTime())
                            + ". Current time: " + sdfT.format(calNow.getTime()));

                    if (calNow.getTimeInMillis() >= calFrom.getTimeInMillis()
                            && calNow.getTimeInMillis() <= calTo.getTimeInMillis()) {
                        SimpleAppLog.info("Current program is: " + program.getTitle());
                        currentChannel.setCurrentProgram(program);
                        programMap.put(currentChannel.getUrl().toLowerCase(), program);
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.JAPANESE);
                        final StringBuffer sb = new StringBuffer();
                        sb.append(program.getTitle()).append("\n");
                        sb.append(sdf.format(new Date(program.getFromTime())));
                        sb.append(" - ").append(sdf.format(new Date(program.getToTime())));
                        showServiceNotification(currentChannel.getName(), sb.toString());
                        notifyChange(META_CHANGED);
                        return true;
                    }
                }
            }
        }
        return false;
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
        Intent viewer =new Intent(Constants.PLAYBACK_VIEWER_INTENT);
        viewer.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        status.contentIntent = PendingIntent.getActivity(this, 0,
                viewer
                , 0);
        startForeground(PLAYBACKSERVICE_STATUS, status);
    }

    private Runnable runnableUpdateNotification = new Runnable() {
        @Override
        public void run() {
            updateNotification();
            handlerUpdateNotification.postDelayed(runnableUpdateNotification, 60 * 1000);
        }
    };

    private Handler handlerUpdateNotification = new Handler();

    private void updateNotification() {
        if (currentChannel != null) {
            updateCurrentProgram(currentChannel, currentAreaId);
        } else {
            String title;
            String desc = null;
            if (getAudioId() < 0) {
                // streaming
                title = getPath();
            } else {
                String artist = getArtistName();
                title = getTrackName();
                if (artist == null || artist.equals(Media.UNKNOWN_STRING)) {
                    artist = getString(R.string.unknown_artist_name);
                }
                String album = getAlbumName();
                if (album == null || album.equals(Media.UNKNOWN_STRING)) {
                    album = getString(R.string.unknown_album_name);
                }
                desc =  getString(R.string.notification_artist_album, artist, album);
            }
            showServiceNotification(title, desc);
        }

    }

    private void stop(boolean remove_status_icon) {
        mIsSupposedToBePlaying = false;
        handlerUpdateNotification.removeCallbacks(runnableUpdateNotification);
        stopFast();
        stopSlow();
        stopAB();
//        if (isStreaming && mStreamingPlayer.isInitialized()) {
//            mStreamingPlayer.stop();
//        }
        if (mStreamingPlayer != null) {
            try {
                if (mStreamingPlayer.isInitialized())
                    mStreamingPlayer.stop();
                //mStreamingPlayer = null;
                mStreamingPlayer.setHandler(null);
            } catch (Exception ex) {
                SimpleAppLog.error("Could not stop ffmpeg stream",ex);
            }
        }
        if (mPlayer != null && mPlayer.isInitialized()) {
            try {
                mPlayer.release();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        mFileToPlay = null;
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
        if (remove_status_icon) {
            gotoIdleState();
        } else {
            stopForeground(false);
        }
    }
    /**
     * Stops playback.
     */
    public void stop() {
        stopAB();
        stop(true);
    }

    /**
     * Pauses playback (call play() to resume)
     */
    public void pause() {
        synchronized(this) {
            mMediaplayerHandler.removeMessages(FADEUP);
            if (isPlaying()) {
                if (isStreaming && mStreamingPlayer != null) {
                    mStreamingPlayer.pause();
                } else {
                    mPlayer.pause();
                }
                stopFast();
                stopSlow();
                gotoIdleState();
                mIsSupposedToBePlaying = false;
                notifyChange(PLAYSTATE_CHANGED);
                saveBookmarkIfNeeded();
            }
        }
    }

    /** Returns whether something is currently playing
     *
     * @return true if something is playing (or will be playing shortly, in case
     * we're currently transitioning between tracks), false if not.
     */
    public boolean isPlaying() {
        return mIsSupposedToBePlaying;
    }

    /*
      Desired behavior for prev/next/shuffle:

      - NEXT will move to the next track in the list when not shuffling, and to
        a track randomly picked from the not-yet-played tracks when shuffling.
        If all tracks have already been played, pick from the full set, but
        avoid picking the previously played track if possible.
      - when shuffling, PREV will go to the previously played track. Hitting PREV
        again will go to the track played before that, etc. When the start of the
        history has been reached, PREV is a no-op.
        When not shuffling, PREV will go to the sequentially previous track (the
        difference with the shuffle-case is mainly that when not shuffling, the
        user can back up to tracks that are not in the history).

        Example:
        When playing an album with 10 tracks from the start, and enabling shuffle
        while playing track 5, the remaining tracks (6-10) will be shuffled, e.g.
        the final play order might be 1-2-3-4-5-8-10-6-9-7.
        When hitting 'prev' 8 times while playing track 7 in this example, the
        user will go to tracks 9-6-10-8-5-4-3-2. If the user then hits 'next',
        a random track will be picked again. If at any time user disables shuffling
        the next/previous track will be picked in sequential order again.
     */

    public void prev() {
        synchronized (this) {
            if (mShuffleMode == SHUFFLE_NORMAL) {
                // go to previously-played track and remove it from the history
                int histsize = mHistory.size();
                if (histsize == 0) {
                    // prev is a no-op
                    return;
                }
                Integer pos = mHistory.remove(histsize - 1);
                mPlayPos = pos.intValue();
            } else {
                if (mPlayPos > 0) {
                    mPlayPos--;
                } else {
                    mPlayPos = mPlayListLen - 1;
                }
            }
            saveBookmarkIfNeeded();
            stop(false);
            openCurrentAndNext();
            play();
            notifyChange(META_CHANGED);
        }
    }

    /**
     * Get the next position to play. Note that this may actually modify mPlayPos
     * if playback is in SHUFFLE_AUTO mode and the shuffle list window needed to
     * be adjusted. Either way, the return value is the next value that should be
     * assigned to mPlayPos;
     */
    private int getNextPosition(boolean force) {
        if (mRepeatMode == REPEAT_CURRENT) {
            if (mPlayPos < 0) return 0;
            return mPlayPos;
        } else if (mShuffleMode == SHUFFLE_NORMAL) {
            // Pick random next track from the not-yet-played ones
            // TODO: make it work right after adding/removing items in the queue.

            // Store the current file in the history, but keep the history at a
            // reasonable size
            if (mPlayPos >= 0) {
                mHistory.add(mPlayPos);
            }
            if (mHistory.size() > MAX_HISTORY_SIZE) {
                mHistory.removeElementAt(0);
            }

            int numTracks = mPlayListLen;
            int[] tracks = new int[numTracks];
            for (int i=0;i < numTracks; i++) {
                tracks[i] = i;
            }

            int numHistory = mHistory.size();
            int numUnplayed = numTracks;
            for (int i=0;i < numHistory; i++) {
                int idx = mHistory.get(i).intValue();
                if (idx < numTracks && tracks[idx] >= 0) {
                    numUnplayed--;
                    tracks[idx] = -1;
                }
            }

            // 'numUnplayed' now indicates how many tracks have not yet
            // been played, and 'tracks' contains the indices of those
            // tracks.
            if (numUnplayed <=0) {
                // everything's already been played
                if (mRepeatMode == REPEAT_ALL || force) {
                    //pick from full set
                    numUnplayed = numTracks;
                    for (int i=0;i < numTracks; i++) {
                        tracks[i] = i;
                    }
                } else {
                    // all done
                    return -1;
                }
            }
            int skip = mRand.nextInt(numUnplayed);
            int cnt = -1;
            while (true) {
                while (tracks[++cnt] < 0)
                    ;
                skip--;
                if (skip < 0) {
                    break;
                }
            }
            return cnt;
        } else if (mShuffleMode == SHUFFLE_AUTO) {
            doAutoShuffleUpdate();
            return mPlayPos + 1;
        } else {
            if (mPlayPos >= mPlayListLen - 1) {
                // we're at the end of the list
                if (mRepeatMode == REPEAT_NONE && !force) {
                    // all done
                    return -1;
                } else if (mRepeatMode == REPEAT_ALL || force) {
                    return 0;
                }
                return -1;
            } else {
                return mPlayPos + 1;
            }
        }
    }

    public void gotoNext(boolean force) {
        synchronized (this) {
            if (mPlayListLen <= 0) {
                Log.d(LOGTAG, "No play queue");
                return;
            }

            int pos = getNextPosition(force);
            if (pos < 0) {
                gotoIdleState();
                if (mIsSupposedToBePlaying) {
                    mIsSupposedToBePlaying = false;
                    notifyChange(PLAYSTATE_CHANGED);
                }
                return;
            }
            mPlayPos = pos;
            saveBookmarkIfNeeded();
            stop(false);
            mPlayPos = pos;
            openCurrentAndNext();
            play();
            notifyChange(META_CHANGED);
        }
    }

    private void gotoIdleState() {
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        Message msg = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
        stopForeground(true);
    }

    private void saveBookmarkIfNeeded() {
        try {
            if (isPodcast()) {
                long pos = position();
                long bookmark = getBookmark();
                long duration = duration();
                if ((pos < bookmark && (pos + 10000) > bookmark) ||
                        (pos > bookmark && (pos - 10000) < bookmark)) {
                    // The existing bookmark is close to the current
                    // position, so don't update it.
                    return;
                }
                if (pos < 15000 || (pos + 10000) > duration) {
                    // if we're near the start or end, clear the bookmark
                    pos = 0;
                }

                // write 'pos' to the bookmark field
                // TODO add back
                /*ContentValues values = new ContentValues();
                values.put(MediaStore.Audio.Media.BOOKMARK, pos);
                Uri uri = ContentUris.withAppendedId(
                		Media.MediaColumns.CONTENT_URI, mCursor.getLong(IDCOLIDX));
                getContentResolver().update(uri, values, null, null);*/
            }
        } catch (SQLiteException ex) {
        }
    }

    // Make sure there are at least 5 items after the currently playing item
    // and no more than 10 items before.
    private void doAutoShuffleUpdate() {
        boolean notify = false;

        // remove old entries
        if (mPlayPos > 10) {
            removeTracks(0, mPlayPos - 9);
            notify = true;
        }
        // add new entries if needed
        int to_add = 7 - (mPlayListLen - (mPlayPos < 0 ? -1 : mPlayPos));
        for (int i = 0; i < to_add; i++) {
            // pick something at random from the list

            int lookback = mHistory.size();
            int idx = -1;
            while(true) {
                idx = mRand.nextInt(mAutoShuffleList.length);
                if (!wasRecentlyUsed(idx, lookback)) {
                    break;
                }
                lookback /= 2;
            }
            mHistory.add(idx);
            if (mHistory.size() > MAX_HISTORY_SIZE) {
                mHistory.remove(0);
            }
            ensurePlayListCapacity(mPlayListLen + 1);
            mPlayList[mPlayListLen++] = mAutoShuffleList[idx];
            notify = true;
        }
        if (notify) {
            notifyChange(QUEUE_CHANGED);
        }
    }

    // check that the specified idx is not in the history (but only look at at
    // most lookbacksize entries in the history)
    private boolean wasRecentlyUsed(int idx, int lookbacksize) {

        // early exit to prevent infinite loops in case idx == mPlayPos
        if (lookbacksize == 0) {
            return false;
        }

        int histsize = mHistory.size();
        if (histsize < lookbacksize) {
            Log.d(LOGTAG, "lookback too big");
            lookbacksize = histsize;
        }
        int maxidx = histsize - 1;
        for (int i = 0; i < lookbacksize; i++) {
            long entry = mHistory.get(maxidx - i);
            if (entry == idx) {
                return true;
            }
        }
        return false;
    }

    // A simple variation of Random that makes sure that the
    // value it returns is not equal to the value it returned
    // previously, unless the interval is 1.
    private static class Shuffler {
        private int mPrevious;
        private Random mRandom = new Random();
        public int nextInt(int interval) {
            int ret;
            do {
                ret = mRandom.nextInt(interval);
            } while (ret == mPrevious && interval > 1);
            mPrevious = ret;
            return ret;
        }
    };

    private boolean makeAutoShuffleList() {
        ContentResolver res = getContentResolver();
        Cursor c = null;
        try {
            c = res.query(Media.MediaColumns.CONTENT_URI,
                    new String[] {Media.MediaColumns._ID}, null, //MediaStore.Audio.Media.IS_MUSIC + "=1",
                    null, null);
            if (c == null || c.getCount() == 0) {
                return false;
            }
            int len = c.getCount();
            long [] list = new long[len];
            for (int i = 0; i < len; i++) {
                c.moveToNext();
                list[i] = c.getLong(0);
            }
            mAutoShuffleList = list;
            return true;
        } catch (RuntimeException ex) {
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return false;
    }

    /**
     * Removes the range of tracks specified from the play list. If a file within the range is
     * the file currently being played, playback will move to the next file after the
     * range. 
     * @param first The first file to be removed
     * @param last The last file to be removed
     * @return the number of tracks deleted
     */
    public int removeTracks(int first, int last) {
        int numremoved = removeTracksInternal(first, last);
        if (numremoved > 0) {
            notifyChange(QUEUE_CHANGED);
        }
        return numremoved;
    }

    private int removeTracksInternal(int first, int last) {
        synchronized (this) {
            if (last < first) return 0;
            if (first < 0) first = 0;
            if (last >= mPlayListLen) last = mPlayListLen - 1;

            boolean gotonext = false;
            if (first <= mPlayPos && mPlayPos <= last) {
                mPlayPos = first;
                gotonext = true;
            } else if (mPlayPos > last) {
                mPlayPos -= (last - first + 1);
            }
            int num = mPlayListLen - last - 1;
            for (int i = 0; i < num; i++) {
                mPlayList[first + i] = mPlayList[last + 1 + i];
            }
            mPlayListLen -= last - first + 1;

            if (gotonext) {
                if (mPlayListLen == 0) {
                    stop(true);
                    mPlayPos = -1;
                    if (mCursor != null) {
                        mCursor.close();
                        mCursor = null;
                    }
                } else {
                    if (mPlayPos >= mPlayListLen) {
                        mPlayPos = 0;
                    }
                    boolean wasPlaying = isPlaying();
                    stop(false);
                    openCurrentAndNext();
                    if (wasPlaying) {
                        play();
                    }
                }
                notifyChange(META_CHANGED);
            }
            return last - first + 1;
        }
    }

    /**
     * Removes all instances of the track with the given id
     * from the playlist.
     * @param id The id to be removed
     * @return how many instances of the track were removed
     */
    public int removeTrack(long id) {
        int numremoved = 0;
        synchronized (this) {
            for (int i = 0; i < mPlayListLen; i++) {
                if (mPlayList[i] == id) {
                    numremoved += removeTracksInternal(i, i);
                    i--;
                }
            }
        }
        if (numremoved > 0) {
            notifyChange(QUEUE_CHANGED);
        }
        return numremoved;
    }

    public void setShuffleMode(int shufflemode) {
        synchronized(this) {
            if (mShuffleMode == shufflemode && mPlayListLen > 0) {
                return;
            }
            mShuffleMode = shufflemode;
            if (mShuffleMode == SHUFFLE_AUTO) {
                if (makeAutoShuffleList()) {
                    mPlayListLen = 0;
                    doAutoShuffleUpdate();
                    mPlayPos = 0;
                    openCurrentAndNext();
                    play();
                    notifyChange(META_CHANGED);
                    return;
                } else {
                    // failed to build a list of files to shuffle
                    mShuffleMode = SHUFFLE_NONE;
                }
            }
            saveQueue(false);
        }
    }
    public int getShuffleMode() {
        return mShuffleMode;
    }

    public void setRepeatMode(int repeatmode) {
        synchronized(this) {
            mRepeatMode = repeatmode;
            saveQueue(false);
        }
    }
    public int getRepeatMode() {
        return mRepeatMode;
    }

    public int getMediaMountedCount() {
        return mMediaMountedCount;
    }

    /**
     * Returns the path of the currently playing file, or null if
     * no file is currently playing.
     */
    public String getPath() {
        return mFileToPlay;
    }

    /**
     * Returns the rowid of the currently playing file, or -1 if
     * no file is currently playing.
     */
    public long getAudioId() {
        synchronized (this) {
            if (mPlayPos >= 0 && (mPlayer.isInitialized() || (mStreamingPlayer != null && mStreamingPlayer.isInitialized()))) {
                return mPlayList[mPlayPos];
            }
        }
        return -1;
    }

    /**
     * Returns the position in the queue 
     * @return the position in the queue
     */
    public int getQueuePosition() {
        synchronized(this) {
            return mPlayPos;
        }
    }

    /**
     * Starts playing the track at the given position in the queue.
     * @param pos The position in the queue of the track that will be played.
     */
    public void setQueuePosition(int pos) {
        synchronized(this) {
            stop(false);
            mPlayPos = pos;
            openCurrentAndNext();
            play();
            notifyChange(META_CHANGED);
            if (mShuffleMode == SHUFFLE_AUTO) {
                doAutoShuffleUpdate();
            }
        }
    }

    public String getArtistName() {
        synchronized(this) {
            if (mCursor == null) {
                return null;
            }
            return mCursor.getString(mCursor.getColumnIndexOrThrow(Media.MediaColumns.ARTIST));
        }
    }

    public long getArtistId() {
        synchronized (this) {
            if (mCursor == null) {
                return -1;
            }
            return -1; //mCursor.getLong(mCursor.getColumnIndexOrThrow(Media.MediaColumns.ARTIST_ID));
        }
    }

    public String getAlbumName() {
        synchronized (this) {
            if (mCursor == null) {
                return null;
            }
            return mCursor.getString(mCursor.getColumnIndexOrThrow(Media.MediaColumns.ALBUM));
        }
    }

    public long getAlbumId() {
        synchronized (this) {
            if (mCursor == null) {
                return -1;
            }
            return -1; //mCursor.getLong(mCursor.getColumnIndexOrThrow(Media.MediaColumns.ALBUM_ID));
        }
    }

    public String getTrackName() {
        synchronized (this) {
            if (mCursor == null) {
                return null;
            }
            return mCursor.getString(mCursor.getColumnIndexOrThrow(Media.MediaColumns.TITLE));
        }
    }

    private boolean isPodcast() {
        synchronized (this) {
            if (mCursor == null) {
                return false;
            }
            return false; //(mCursor.getInt(PODCASTCOLIDX) > 0);
        }
    }

    private long getBookmark() {
        synchronized (this) {
            if (mCursor == null) {
                return 0;
            }
            return 0; //mCursor.getLong(BOOKMARKCOLIDX);
        }
    }

    /**
     * Returns the duration of the file in milliseconds.
     * Currently this method returns -1 for the duration of MIDI files.
     */
    public long duration() {
        if (isStreaming && mStreamingPlayer != null && mStreamingPlayer.isInitialized()) {
            return mStreamingPlayer.duration();
        }
        if (!isStreaming && mPlayer.isInitialized()) {
            return mPlayer.duration();
        }
        return -1;
    }

    /**
     * Returns the current playback position in milliseconds
     */
    public long position() {
        if (isStreaming && mStreamingPlayer != null && mStreamingPlayer.isInitialized()) {
            return mStreamingPlayer.position();
        }
        if (!isStreaming && mPlayer.isInitialized()) {
            return mPlayer.position();
        }
        return -1;
    }

    /**
     * Seeks to the position specified.
     *
     * @param pos The position to seek to, in milliseconds
     */
    public long seek(long pos) {
        if (isStreaming && mStreamingPlayer != null && mStreamingPlayer.isInitialized()) {
            if (pos < 0) pos = 0;
            if (pos > mStreamingPlayer.duration() && mStreamingPlayer.duration() != 0) pos = mStreamingPlayer.duration();
            return mStreamingPlayer.seek(pos);
        }
        if (!isStreaming && mPlayer.isInitialized()) {
            if (pos < 0) pos = 0;
            if (pos > mPlayer.duration() && mPlayer.duration() != 0) pos = mPlayer.duration();
            return mPlayer.seek(pos);
        }
        return -1;
    }

    /**
     * Sets the audio session ID.
     *
     * @param sessionId: the audio session ID.
     */
    public void setAudioSessionId(int sessionId) {
        synchronized (this) {
            if (isStreaming)
                if (mStreamingPlayer != null)
                    mStreamingPlayer.setAudioSessionId(sessionId);
                else
                    mPlayer.setAudioSessionId(sessionId);
        }
    }

    /**
     * Returns the audio session ID.
     */
    public int getAudioSessionId() {
        synchronized (this) {
            if (isStreaming && mStreamingPlayer != null) return mStreamingPlayer.getAudioSessionId();
            return mPlayer.getAudioSessionId();
        }
    }

    private String getMediaUri() {
        synchronized (this) {
            return mFileToPlay;
        }
    }

    /**
     * Provides a unified interface for dealing with midi files and
     * other media files.
     */
    private class CustomMultiPlayer {
        private at.aau.itec.android.mediaplayer.MediaPlayer mCurrentMediaPlayer = new at.aau.itec.android.mediaplayer.MediaPlayer();
        private at.aau.itec.android.mediaplayer.MediaPlayer mNextMediaPlayer;
        private Handler mHandler;
        private boolean mIsInitialized = false;

        public CustomMultiPlayer() {
            mCurrentMediaPlayer.setWakeMode(MediaPlaybackService.this, PowerManager.PARTIAL_WAKE_LOCK);

        }

        public void setDataSource(String path) {
            mIsInitialized = setDataSourceImpl(mCurrentMediaPlayer, path);
        }

        private boolean setDataSourceImpl(at.aau.itec.android.mediaplayer.MediaPlayer player, String path) {
            try {
                player.setOnPreparedListener(null);
                if (path.startsWith("content://")) {
                    player.setDataSource(MediaPlaybackService.this, Uri.parse(path));
                } else {
                    MediaSource source = new FileSource(new File(path));
                    player.setDataSource(source);
                }
                //player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                player.setOnPreparedListener(preparedListener);
            } catch (IOException ex) {
                SimpleAppLog.error("Could not open file",ex);
                return false;
            } catch (IllegalArgumentException ex) {
                SimpleAppLog.error("Could not open file",ex);
                return false;
            }
            return true;
        }

        public boolean isInitialized() {
            return mIsInitialized;
        }

        public void setPlaybackSpeed(float speed) {
            mCurrentMediaPlayer.setPlaybackSpeed(speed);
        }

        public float getPlaybackSpeed() {
            return mCurrentMediaPlayer.getPlaybackSpeed();
        }

        public void start() {
            mCurrentMediaPlayer.start();
        }

        public void stop() {
            mCurrentMediaPlayer.stop();
            mIsInitialized = false;
        }

        /**
         * You CANNOT use this player anymore after calling release()
         */
        public void release() {
            stop();
            //mCurrentMediaPlayer.release();
        }

        public void pause() {
            mCurrentMediaPlayer.pause();
        }

        public void setHandler(Handler handler) {
            mHandler = handler;
        }

        at.aau.itec.android.mediaplayer.MediaPlayer.OnPreparedListener preparedListener = new at.aau.itec.android.mediaplayer.MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(at.aau.itec.android.mediaplayer.MediaPlayer mp) {
                mp.setOnCompletionListener(listener);
                //mp.setOnErrorListener(errorListener);
                Intent i = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
                i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
                i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
                sendBroadcast(i);
                play();
                notifyChange(META_CHANGED);
            }
        };

        at.aau.itec.android.mediaplayer.MediaPlayer.OnCompletionListener listener = new at.aau.itec.android.mediaplayer.MediaPlayer.OnCompletionListener() {
            public void onCompletion(at.aau.itec.android.mediaplayer.MediaPlayer mp) {
                if (mp == mCurrentMediaPlayer && mNextMediaPlayer != null) {
                    mCurrentMediaPlayer.stop();
                    mCurrentMediaPlayer = mNextMediaPlayer;
                    mNextMediaPlayer = null;
                    mHandler.sendEmptyMessage(TRACK_WENT_TO_NEXT);
                } else {
                    // Acquire a temporary wakelock, since when we return from
                    // this callback the MediaPlayer will release its wakelock
                    // and allow the device to go to sleep.
                    // This temporary wakelock is released when the RELEASE_WAKELOCK
                    // message is processed, but just in case, put a timeout on it.
                    mWakeLock.acquire(30000);
                    mHandler.sendEmptyMessage(TRACK_ENDED);
                    mHandler.sendEmptyMessage(RELEASE_WAKELOCK);
                }
            }
        };


        public long duration() {
            return mCurrentMediaPlayer.getDuration();
        }

        public long position() {
            return mCurrentMediaPlayer.getCurrentPosition();
        }

        public long seek(long whereto) {
            mCurrentMediaPlayer.seekTo((int) whereto);
            return whereto;
        }

        public void setVolume(float vol) {
            // mCurrentMediaPlayer.setVolume(vol, vol);
        }

        public void setAudioSessionId(int sessionId) {
            mCurrentMediaPlayer.setAudioSessionId(sessionId);
        }

        public int getAudioSessionId() {
            return mCurrentMediaPlayer.getAudioSessionId();
        }
    }

    /**
     * Provides a unified interface for dealing with midi files and
     * other media files.
     */
    private class DefaultMultiPlayer {
        private MediaPlayer mCurrentMediaPlayer = new MediaPlayer();
        private MediaPlayer mNextMediaPlayer;
        private Handler mHandler;
        private boolean mIsInitialized = false;

        public DefaultMultiPlayer() {
            mCurrentMediaPlayer.setWakeMode(MediaPlaybackService.this, PowerManager.PARTIAL_WAKE_LOCK);

        }

        public void setDataSource(String path) {
            mIsInitialized = setDataSourceImpl(mCurrentMediaPlayer, path);
        }

        private boolean setDataSourceImpl(MediaPlayer player, String path) {
            try {
                player.reset();
                player.setOnPreparedListener(null);
                if (path.startsWith("content://")) {
                    player.setDataSource(MediaPlaybackService.this, Uri.parse(path));
                } else {
                    player.setDataSource(path);
                }
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                player.setOnPreparedListener(preparedListener);
                player.prepare();
            } catch (IOException ex) {
                SimpleAppLog.error("Could not open file",ex);
                return false;
            } catch (IllegalArgumentException ex) {
                SimpleAppLog.error("Could not open file",ex);
                return false;
            }
            return true;
        }

        public boolean isInitialized() {
            return mIsInitialized;
        }

        public void start() {
            mCurrentMediaPlayer.start();
        }

        public void stop() {
            mCurrentMediaPlayer.reset();
            mIsInitialized = false;
        }

        /**
         * You CANNOT use this player anymore after calling release()
         */
        public void release() {
            stop();
            mCurrentMediaPlayer.release();
        }

        public void pause() {
            mCurrentMediaPlayer.pause();
        }

        public void setHandler(Handler handler) {
            mHandler = handler;
        }

        MediaPlayer.OnPreparedListener preparedListener = new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setOnCompletionListener(listener);
                mp.setOnErrorListener(errorListener);
                Intent i = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
                i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
                i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
                sendBroadcast(i);
                play();
                notifyChange(META_CHANGED);
            }
        };

        MediaPlayer.OnCompletionListener listener = new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                if (mp == mCurrentMediaPlayer && mNextMediaPlayer != null) {
                    mCurrentMediaPlayer.release();
                    mCurrentMediaPlayer = mNextMediaPlayer;
                    mNextMediaPlayer = null;
                    mHandler.sendEmptyMessage(TRACK_WENT_TO_NEXT);
                } else {
                    // Acquire a temporary wakelock, since when we return from
                    // this callback the MediaPlayer will release its wakelock
                    // and allow the device to go to sleep.
                    // This temporary wakelock is released when the RELEASE_WAKELOCK
                    // message is processed, but just in case, put a timeout on it.
                    mWakeLock.acquire(30000);
                    mHandler.sendEmptyMessage(TRACK_ENDED);
                    mHandler.sendEmptyMessage(RELEASE_WAKELOCK);
                }
            }
        };

        MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
                switch (what) {
                    case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                        mIsInitialized = false;
                        mCurrentMediaPlayer.release();
                        // Creating a new MediaPlayer and settings its wakemode does not
                        // require the media service, so it's OK to do this now, while the
                        // service is still being restarted
                        mCurrentMediaPlayer = new MediaPlayer();
                        mCurrentMediaPlayer.setWakeMode(MediaPlaybackService.this, PowerManager.PARTIAL_WAKE_LOCK);
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(SERVER_DIED), 2000);
                        return true;
                    default:
                        Log.d("DefaultMultiPlayer", "Error: " + what + "," + extra);
                        break;
                }
                return false;
            }
        };

        public long duration() {
            return mCurrentMediaPlayer.getDuration();
        }

        public long position() {
            return mCurrentMediaPlayer.getCurrentPosition();
        }

        public long seek(long whereto) {
            mCurrentMediaPlayer.seekTo((int) whereto);
            return whereto;
        }

        public void setVolume(float vol) {
            mCurrentMediaPlayer.setVolume(vol, vol);
        }

        public void setAudioSessionId(int sessionId) {
            mCurrentMediaPlayer.setAudioSessionId(sessionId);
        }

        public int getAudioSessionId() {
            return mCurrentMediaPlayer.getAudioSessionId();
        }
    }

    /**
     * Provides a unified interface for dealing with midi files and
     * other media files.
     */

    private class MultiPlayer {

        private static final int MIN_RECONNECT_TIMEOUT = 1000;

        private static final int EXTRA_RECONNECT_TIMEOUT = 2000;

        private final FFmpegMediaPlayer mCurrentMediaPlayer = new FFmpegMediaPlayer();

        private Handler mHandler;
        private boolean mIsInitialized = false;

        Handler mainHandler = new Handler();

        private Runnable openStreamRunnable = new Runnable() {
            @Override
            public void run() {
                openStream("");
            }
        };

        public MultiPlayer() {
            mCurrentMediaPlayer.setWakeMode(MediaPlaybackService.this, PowerManager.PARTIAL_WAKE_LOCK);
            mCurrentMediaPlayer.setOnRecordingListener(recordingListener);
        }

        public void startRecording(Channel selectedChannel, String path) {
            mCurrentMediaPlayer.startRecording(selectedChannel, path);
        }

        public void stopRecording() {
            mCurrentMediaPlayer.stopRecording();
        }

        public boolean isRecording() {
            return mCurrentMediaPlayer.isRecording();
        }

        public void setDataSource(String path) {
            mIsInitialized = setDataSourceImpl(mCurrentMediaPlayer, path);
        }

        private boolean setDataSourceImpl(FFmpegMediaPlayer player, String path) {
            try {
                mCurrentMediaPlayer.reset();
                mCurrentMediaPlayer.setOnPreparedListener(null);
                mCurrentMediaPlayer.setOnCompletionListener(null);
                mCurrentMediaPlayer.setOnErrorListener(null);
                if (path.startsWith("content://")) {
                    player.setDataSource(MediaPlaybackService.this, Uri.parse(path));
                } else {
                    player.setDataSource(path);
                }
                mCurrentMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mCurrentMediaPlayer.setOnPreparedListener(preparedListener);
                mCurrentMediaPlayer.setOnCompletionListener(listener);
                mCurrentMediaPlayer.setOnErrorListener(errorListener);

                mCurrentMediaPlayer.prepareAsync();
//                prepareThread = new Thread(prepareRunnable);
//                prepareThread.start();
            } catch (IOException ex) {
                SimpleAppLog.error("Could not set data source", ex);
                return false;
            } catch (IllegalArgumentException ex) {
                SimpleAppLog.error("Could not set data source", ex);
                return false;
            }
            return true;
        }

        public boolean isInitialized() {
            return mIsInitialized;
        }

        public void start() {

            MusicUtils.debugLog(new Exception("MultiPlayer.start called"));
            mCurrentMediaPlayer.start();

        }

        public void stop() {
            mainHandler.removeCallbacks(openStreamRunnable);
            mCurrentMediaPlayer.setOnPreparedListener(null);
            mCurrentMediaPlayer.setOnCompletionListener(null);
            mCurrentMediaPlayer.setOnErrorListener(null);
            mCurrentMediaPlayer.reset();
            mIsInitialized = false;

        }

        /**
         * You CANNOT use this player anymore after calling release()
         */
        public void release() {
            stop();
            mCurrentMediaPlayer.release();
        }

        public void pause() {
            mCurrentMediaPlayer.pause();
        }

        public void setHandler(Handler handler) {
            mHandler = handler;
        }

        FFmpegMediaPlayer.OnPreparedListener preparedListener = new FFmpegMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(FFmpegMediaPlayer mp) {
                Intent i = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
                i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
                i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
                sendBroadcast(i);
                play();
                setVolume(1.0f);
                notifyChange(META_CHANGED);
            }
        };

        private void reconnect() {
            mainHandler.removeCallbacks(openStreamRunnable);
            mainHandler.postDelayed(openStreamRunnable, EXTRA_RECONNECT_TIMEOUT);
        }

        FFmpegMediaPlayer.OnCompletionListener listener = new FFmpegMediaPlayer.OnCompletionListener() {
            public void onCompletion(FFmpegMediaPlayer mp) {
                SimpleAppLog.error("Error from proxy - try to reconnect");
                reconnect();
            }
        };

        FFmpegMediaPlayer.OnErrorListener errorListener = new FFmpegMediaPlayer.OnErrorListener() {
            public boolean onError(FFmpegMediaPlayer mp, int what, int extra) {
                SimpleAppLog.error("MultiPlayer - Error: " + what + "," + extra);
                //SimpleAppLog.info("Error from player - try to reconnect");

                //reconnect();

                notifyChange(META_CHANGED);
                return false;
            }
        };

        public long duration() {

            return mCurrentMediaPlayer.getDuration();

        }

        public long position() {
            return mCurrentMediaPlayer.getCurrentPosition();
        }

        public long seek(long whereto) {
            mCurrentMediaPlayer.seekTo((int) whereto);
            return whereto;
        }

        public void setVolume(float vol) {
            mCurrentMediaPlayer.setVolume(vol, vol);
        }

        public void setAudioSessionId(int sessionId) {
            mCurrentMediaPlayer.setAudioSessionId(sessionId);
        }

        public int getAudioSessionId() {
            return mCurrentMediaPlayer.getAudioSessionId();
        }
    }

    static class CompatMediaPlayer extends MediaPlayer implements OnCompletionListener {

        private boolean mCompatMode = true;
        private FFmpegMediaPlayer mNextPlayer;
        private OnCompletionListener mCompletion;

        public CompatMediaPlayer() {
            try {
                FFmpegMediaPlayer.class.getMethod("setNextMediaPlayer", FFmpegMediaPlayer.class);
                mCompatMode = false;
            } catch (NoSuchMethodException e) {
                mCompatMode = true;
                super.setOnCompletionListener(this);
            }
        }

        @Override
        public void setOnCompletionListener(OnCompletionListener listener) {
            if (mCompatMode) {
                mCompletion = listener;
            } else {
                super.setOnCompletionListener(listener);
            }
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            if (mNextPlayer != null) {
                // as it turns out, starting a new MediaPlayer on the completion
                // of a previous player ends up slightly overlapping the two
                // playbacks, so slightly delaying the start of the next player
                // gives a better user experience
                SystemClock.sleep(50);
                mNextPlayer.start();
            }
            mCompletion.onCompletion(this);
        }
    }

    /*
     * By making this a static class with a WeakReference to the Service, we
     * ensure that the Service can be GCd even when the system process still
     * has a remote reference to the stub.
     */
    static class ServiceStub extends IMediaPlaybackService.Stub {
        WeakReference<MediaPlaybackService> mService;

        ServiceStub(MediaPlaybackService service) {
            mService = new WeakReference<MediaPlaybackService>(service);
        }

        @Override
        public String updateRtmpSuck(String token, String playUrl) throws RemoteException {
            return mService.get().updateRtmpSuck(token, playUrl);
        }

        // Added by Hai
        @Override
        public void startRecord(String token, String fileName) throws RemoteException {
            mService.get().startRecord(token, fileName);
        }

        @Override
        public void stopRecord() throws RemoteException {
            mService.get().stopRecord();
        }

        @Override
        public boolean isRecording() throws RemoteException {
            return mService.get().isRecording();
        }

        @Override
        public void markAB() throws RemoteException {
            mService.get().markAB();
        }

        @Override
        public int getStateAB() throws RemoteException {
            return mService.get().getStateAB();
        }

        @Override
        public void stopAB() throws RemoteException {
            mService.get().stopAB();
        }

        @Override
        public long getAPos() throws RemoteException {
            return mService.get().getPosA();
        }

        @Override
        public long getBPos() throws RemoteException {
            return mService.get().getPosB();
        }

        @Override
        public boolean isFast() throws RemoteException {
            return mService.get().isFast();
        }

        @Override
        public boolean isSlow() throws RemoteException {
            return mService.get().isSlow();
        }

        @Override
        public void doBack(int length) throws RemoteException {
            mService.get().doBack(length);
        }

        @Override
        public void doFast(float level) throws RemoteException {
            mService.get().doFast( level);
        }

        @Override
        public void stopFast() throws RemoteException {
            mService.get().stopFast();
        }

        @Override
        public void doSlow(float level) throws RemoteException {
            mService.get().doSlow( level);
        }

        @Override
        public void stopSlow() throws RemoteException {
            mService.get().stopSlow();
        }

        @Override
        public boolean isStreaming() throws RemoteException {
            return mService.get().isStreaming();
        }

        @Override
        public void setStreaming(boolean isStreaming) throws RemoteException {
            mService.get().setStreaming(isStreaming);
        }

        @Override
        public void openStream(String token, String objChannel) throws RemoteException {
            mService.get().openStream(token, objChannel);
        }

        @Override
        public String getChannelObject() throws RemoteException {
            return mService.get().getChannelObject();
        }

        @Override
        public void setChannelObject(String channelObject) throws RemoteException {
            mService.get().setChannelObject(channelObject);
        }

        // Default
        public void openFile(String path)
        {
            mService.get().open(path);
        }
        public void open(long [] list, int position) {
            mService.get().open(list, position);
        }
        public int getQueuePosition() {
            return mService.get().getQueuePosition();
        }
        public void setQueuePosition(int index) {
            mService.get().setQueuePosition(index);
        }
        public boolean isPlaying() {
            return mService.get().isPlaying();
        }
        public void stop() {
            mService.get().stop();
        }
        public void pause() {
            mService.get().pause();
        }
        public void play() {
            mService.get().play();
        }
        public void prev() {
            mService.get().prev();
        }
        public void next() {
            mService.get().gotoNext(true);
        }
        public String getTrackName() {
            return mService.get().getTrackName();
        }
        public String getAlbumName() {
            return mService.get().getAlbumName();
        }
        public long getAlbumId() {
            return mService.get().getAlbumId();
        }
        public String getArtistName() {
            return mService.get().getArtistName();
        }
        public long getArtistId() {
            return mService.get().getArtistId();
        }
        public void enqueue(long [] list , int action) {
            mService.get().enqueue(list, action);
        }
        public long [] getQueue() {
            return mService.get().getQueue();
        }
        public void moveQueueItem(int from, int to) {
            mService.get().moveQueueItem(from, to);
        }
        public String getPath() {
            return mService.get().getPath();
        }
        public long getAudioId() {
            return mService.get().getAudioId();
        }
        public long position() {
            return mService.get().position();
        }
        public long duration() {
            return mService.get().duration();
        }
        public long seek(long pos) {
            return mService.get().seek(pos);
        }
        public void setShuffleMode(int shufflemode) {
            mService.get().setShuffleMode(shufflemode);
        }
        public int getShuffleMode() {
            return mService.get().getShuffleMode();
        }
        public int removeTracks(int first, int last) {
            return mService.get().removeTracks(first, last);
        }
        public int removeTrack(long id) {
            return mService.get().removeTrack(id);
        }
        public void setRepeatMode(int repeatmode) {
            mService.get().setRepeatMode(repeatmode);
        }
        public int getRepeatMode() {
            return mService.get().getRepeatMode();
        }
        public int getMediaMountedCount() {
            return mService.get().getMediaMountedCount();
        }
        public int getAudioSessionId() {
            return mService.get().getAudioSessionId();
        }
        public String getMediaUri() {
            return mService.get().getMediaUri();
        }
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.println("" + mPlayListLen + " items in queue, currently at index " + mPlayPos);
        writer.println("Currently loaded:");
        writer.println(getArtistName());
        writer.println(getAlbumName());
        writer.println(getTrackName());
        writer.println(getPath());
        writer.println("playing: " + mIsSupposedToBePlaying);
        if (isStreaming && mStreamingPlayer != null)
            writer.println("actual: " + mStreamingPlayer.mCurrentMediaPlayer.isPlaying());
        else
            writer.println("actual: " + mPlayer.mCurrentMediaPlayer.isPlaying());
        writer.println("shuffle mode: " + mShuffleMode);
        MusicUtils.debugDump(writer);
    }

    private final IBinder mBinder = new ServiceStub(this);
}
