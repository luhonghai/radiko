package com.gmail.radioserver2.fragment;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.dotohsoft.rtmpdump.RTMP;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.activity.TimerSettingsActivity;
import com.gmail.radioserver2.data.Setting;
import com.gmail.radioserver2.radiko.ClientTokenFetcher;
import com.gmail.radioserver2.radiko.ServerTokenFetcher;
import com.gmail.radioserver2.radiko.TokenFetcher;
import com.gmail.radioserver2.service.IMediaPlaybackService;
import com.gmail.radioserver2.service.MediaPlaybackService;
import com.gmail.radioserver2.service.MusicUtils;
import com.gmail.radioserver2.utils.DateHelper;
import com.gmail.radioserver2.utils.FileHelper;
import com.gmail.radioserver2.utils.SimpleAppLog;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.WeakHashMap;

/**
 * Created by luhonghai on 2/16/15.
 */

public class PlayerFragmentTab extends FragmentTab implements ServiceConnection,View.OnClickListener {

    /**
     *  Screen state
     */

    private boolean isStreaming = false;

    private boolean isRecording = false;

    private boolean isPlaying = false;

    private boolean isRunning = false;

    private boolean isTokenLoaded = false;

    private boolean isServiceLoaded = false;

    private String radikoToken;



    private TextView txtTitle;

    private TextView txtDesciption;

    private Button btnTimer;

    private Button btnBack;

    private Button btnPlay;

    private Button btnRecord;

    private Button btnSlow;

    private Button btnFast;

    private Button btnRepeat;

    private ImageButton btnPrev;

    private ImageButton btnNext;

    private SeekBar seekBarPlayer;

    private IMediaPlaybackService mService = null;

    private MusicUtils.ServiceToken mServiceToken;

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        txtTitle = null;
        txtDesciption = null;
        btnTimer = null;
        btnBack = null;
        btnPlay = null;
        btnRecord = null;
        btnSlow = null;
        btnFast = null;
        btnRepeat = null;
        btnPrev = null;
        btnNext = null;
        seekBarPlayer = null;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isServiceLoaded = false;
        isTokenLoaded = false;
        mServiceToken = MusicUtils.bindToService(getActivity(), this);

        TokenFetcher tokenFetcher;
        if (getResources().getBoolean(R.bool.radiko_token_type)) {
            tokenFetcher  = new ClientTokenFetcher(getActivity(),onTokenListener);
        } else {
            tokenFetcher  = new ServerTokenFetcher(getActivity(),onTokenListener);
        }
        tokenFetcher.fetch();

        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
        f.addAction(MediaPlaybackService.META_CHANGED);
        getActivity().registerReceiver(mStatusListener, new IntentFilter(f));
    }


    private TokenFetcher.OnTokenListener onTokenListener = new TokenFetcher.OnTokenListener() {

        @Override
        public void onTokenFound(final String token) {
            SimpleAppLog.info("Found token: " + token);
            isTokenLoaded = true;
            radikoToken = token;
            showPlayer();
        }

        @Override
        public void onError(final String message, Throwable throwable) {
            isTokenLoaded = true;
            radikoToken = "";
            SimpleAppLog.error(message, throwable);
            showPlayer();
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        MusicUtils.unbindFromService(mServiceToken);
        mService = null;
        try {
            getActivity().unregisterReceiver(mStatusListener);
        } catch (Exception ex) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_player_tab, container, false);
        btnBack = (Button) v.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(this);
        btnTimer = (Button) v.findViewById(R.id.btnTimer);
        btnTimer.setOnClickListener(this);
        btnSlow = (Button) v.findViewById(R.id.btnSlow);
        btnSlow.setOnClickListener(this);
        btnFast = (Button) v.findViewById(R.id.btnFast);
        btnFast.setOnClickListener(this);
        btnRecord = (Button) v.findViewById(R.id.btnRecord);
        btnRecord.setOnClickListener(this);
        btnRepeat = (Button) v.findViewById(R.id.btnRepeat);
        btnRepeat.setOnClickListener(this);
        btnPlay = (Button) v.findViewById(R.id.btnPlay);
        btnPlay.setOnClickListener(this);

        btnPrev = (ImageButton) v.findViewById(R.id.btnPrev);
        btnPrev.setOnClickListener(this);
        btnNext = (ImageButton) v.findViewById(R.id.btnNext);
        btnNext.setOnClickListener(this);
        txtTitle = (TextView) v.findViewById(R.id.txtTitle);
        txtDesciption = (TextView) v.findViewById(R.id.txtDescription);

        seekBarPlayer = (SeekBar) v.findViewById(R.id.seekBarPlayer);
        seekBarPlayer.setOnSeekBarChangeListener(mSeekListener);

        switchButtonStage(ButtonStage.DISABLED);
        return v;
    }

    private void showPlayer() {
        if (isServiceLoaded && isTokenLoaded) {
            if (isRunning) {
                if (isStreaming) {
                    if (isRecording) {
                        switchButtonStage(ButtonStage.RECORDING);
                    } else {
                        switchButtonStage(ButtonStage.STREAMING);
                    }
                } else {
                    switchButtonStage(ButtonStage.PLAYING);
                }
            } else {
                switchButtonStage();
            }
        }
    }

    enum ButtonStage {
        STREAMING,
        RECORDING,
        DEFAULT,
        DISABLED,
        PLAYING
    }

    private ButtonStage buttonStage = ButtonStage.DEFAULT;

    private void switchButtonStage() {
        switchButtonStage(buttonStage);
    }

    private void switchButtonStage(final ButtonStage stage) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (stage) {
                    case STREAMING:
                        btnRecord.setEnabled(true);
                        btnPlay.setEnabled(true);
                        btnRepeat.setEnabled(false);
                        btnFast.setEnabled(false);
                        btnSlow.setEnabled(false);
                        btnBack.setEnabled(false);
                        btnPlay.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.icon_pause,0,0);
                        btnPlay.setText(R.string.button_pause);
                        btnRecord.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.icon_record,0,0);
                        btnRecord.setText(R.string.button_record);
                        btnRepeat.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.icon_repeat,0,0);
                        btnRepeat.setText(R.string.button_repeat);
                        break;
                    case RECORDING:
                        btnPlay.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.icon_pause,0,0);
                        btnPlay.setText(R.string.button_pause);
                        btnRecord.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.icon_stop,0,0);
                        btnRecord.setText(R.string.button_stop);
                        btnRepeat.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.icon_repeat,0,0);
                        btnRepeat.setText(R.string.button_repeat);
                        btnRecord.setEnabled(true);
                        btnPlay.setEnabled(true);
                        btnRepeat.setEnabled(false);
                        btnFast.setEnabled(false);
                        btnSlow.setEnabled(false);
                        btnBack.setEnabled(false);
                        break;
                    case DISABLED:
                        btnRecord.setEnabled(false);
                        btnPlay.setEnabled(false);
                        btnRepeat.setEnabled(false);
                        btnFast.setEnabled(false);
                        btnSlow.setEnabled(false);
                        btnBack.setEnabled(false);
                        break;
                    case PLAYING:
                        btnPlay.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.icon_pause,0,0);
                        btnPlay.setText(R.string.button_stop);
                        btnRecord.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.icon_record,0,0);
                        btnRecord.setText(R.string.button_stop);
                        btnRepeat.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.icon_repeat,0,0);
                        btnRepeat.setText(R.string.button_repeat);
                        btnRecord.setEnabled(false);
                        btnPlay.setEnabled(true);
                        btnRepeat.setEnabled(true);
                        btnFast.setEnabled(true);
                        btnSlow.setEnabled(true);
                        btnBack.setEnabled(true);
                        break;
                    case DEFAULT:
                    default:
                        btnPlay.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.icon_play,0,0);
                        btnPlay.setText(R.string.button_play);
                        btnRecord.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.icon_record,0,0);
                        btnRecord.setText(R.string.button_record);
                        btnRepeat.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.icon_repeat,0,0);
                        btnRepeat.setText(R.string.button_repeat);
                        btnRecord.setEnabled(false);
                        btnPlay.setEnabled(true);
                        btnRepeat.setEnabled(!isStreaming);
                        btnFast.setEnabled(!isStreaming);
                        btnSlow.setEnabled(!isStreaming);
                        btnBack.setEnabled(!isStreaming);
                        break;
                }
            }
        });
    }

    private TokenFetcher.OnTokenListener onTokenRecordingListener = new TokenFetcher.OnTokenListener() {

        @Override
        public void onTokenFound(final String token) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    FileHelper fileHelper = new FileHelper(getActivity());
                    try {
                        if (mService.isPlaying() && isStreaming) {
                            mService.startRecord(token, fileHelper.getTempFile().getAbsolutePath());
                            switchButtonStage(ButtonStage.RECORDING);
                        }
                    } catch (RemoteException e) {
                        if (isStreaming) {
                            switchButtonStage(ButtonStage.STREAMING);
                        } else {
                            switchButtonStage(ButtonStage.DEFAULT);
                        }
                        isRecording = false;
                        SimpleAppLog.error("Could not start recording", e);
                    }
                }
            });
        }

        @Override
        public void onError(final String message, Throwable throwable) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isStreaming) {
                        switchButtonStage(ButtonStage.STREAMING);
                    } else {
                        switchButtonStage(ButtonStage.DEFAULT);
                    }
                    isRecording = false;
                }
            });
        }
    };


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnBack:
                try {
                    if (!isStreaming && mService.isPlaying()) {
                        Setting setting = new Setting(getActivity());
                        setting.load();
                        mService.doBack(Math.round(setting.getBackLength()));
                    }
                } catch (RemoteException e) {
                    SimpleAppLog.error("Could not back",e);
                }
                break;
            case R.id.btnPlay:
                switchButtonStage(ButtonStage.DISABLED);
                if (isRunning) {
                    try {
                        if (mService.isPlaying()) {
                            if (isStreaming) {
                                mService.stop();
                            } else {
                                mService.pause();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    isRunning = false;
                    switchButtonStage(ButtonStage.DEFAULT);
                } else {
                    try {
                        if (isStreaming && mService.isRecording())
                            mService.stopRecord();
                        isRecording = false;
                    } catch (Exception ex) {
                        SimpleAppLog.error("Could not stop recording",ex);
                    }
                    try {
                        if (isStreaming && mService.isPlaying()) {
                            try {
                                mService.stop();
                            } catch (Exception ex) {
                                SimpleAppLog.error("Could not stop streaming",ex);
                            }
                        }
                        if (isStreaming) {
                            mService.openFile("rtmp://0.0.0.0:1935/TBS/_definst_/simul-stream.stream|S:" + radikoToken);
                            switchButtonStage(ButtonStage.STREAMING);
                        } else {
                            mService.play();
                            switchButtonStage(ButtonStage.PLAYING);
                        }
                        isRunning = true;
                    } catch (RemoteException e) {
                        SimpleAppLog.error("Could not play",e);
                        isRunning = false;
                        switchButtonStage(ButtonStage.DEFAULT);
                    }
                }
                break;
            case R.id.btnRecord:
                if (isRecording) {
                    try {
                        if (mService.isRecording())
                            mService.stopRecord();
                    } catch (RemoteException e) {
                        SimpleAppLog.error("Could not stop recording",e);
                    }
                    if (isStreaming) {
                        switchButtonStage(ButtonStage.STREAMING);
                    } else {
                        switchButtonStage(ButtonStage.DEFAULT);
                    }
                    isRecording = false;
                } else {
                    isRecording = true;
                    switchButtonStage(ButtonStage.DISABLED);
                    TokenFetcher tokenFetcher;
                    if (getResources().getBoolean(R.bool.radiko_token_type)) {
                        tokenFetcher  = new ClientTokenFetcher(getActivity(),onTokenRecordingListener);
                    } else {
                        tokenFetcher  = new ServerTokenFetcher(getActivity(),onTokenRecordingListener);
                    }
                    tokenFetcher.fetch();
                }
                break;
            case R.id.btnPrev:
                try {
                    if (mService != null)
                        mService.prev();
                } catch (RemoteException e) {
                    SimpleAppLog.error("Could not go prev item", e);
                }
                break;
            case R.id.btnNext:
                try {
                    if (mService != null)
                        mService.next();
                } catch (RemoteException e) {
                    SimpleAppLog.error("Could not go next item", e);
                }
                break;
            case R.id.btnRepeat:
                try {
                    switch(mService.getStateAB()) {
                        case MediaPlaybackService.ABState.FLAG:
                            mService.markAB();
                            break;
                        case MediaPlaybackService.ABState.PLAY:
                            mService.stop();
                            break;
                        case MediaPlaybackService.ABState.STOP:
                            mService.markAB();
                            break;
                    }
                } catch (RemoteException e) {
                    SimpleAppLog.error("Could call AB state", e);
                }
                break;
            case R.id.btnSlow:
                break;
            case R.id.btnFast:
                break;
            case R.id.btnTimer:
                Intent intent = new Intent();
                intent.setClass(getActivity(), TimerSettingsActivity.class);
                startActivity(intent);
                break;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = IMediaPlaybackService.Stub.asInterface(service);

        try {
            String uri = mService.getMediaUri();
            isRunning = mService.isPlaying();
            isRecording = mService.isRecording();
            isStreaming = uri == null || uri.toLowerCase().startsWith("rtmp");
            isServiceLoaded = true;
            showPlayer();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mService = null;
    }

    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MediaPlaybackService.META_CHANGED)) {
                // redraw the artist/title info and
                // set new max for progress bar
                setPauseButtonImage();
                updateProcess();
            } else if (action.equals(MediaPlaybackService.PLAYSTATE_CHANGED)) {
                setPauseButtonImage();
                updateProcess();
            }
        }
    };

    private void updateProcess() {
        try {
            if (mService != null && mService.isPlaying() && seekBarPlayer != null) {
                mDuration = mService.duration();
                long pos = mPosOverride < 0 ? mService.position() : mPosOverride;
                if ((pos >= 0)) {
                    if (mDuration > 0) {
                        seekBarPlayer.setProgress((int) (1000 * pos / mDuration));
                    } else {
                        seekBarPlayer.setProgress(1000);
                    }
                } else {
                    seekBarPlayer.setProgress(1000);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void setPauseButtonImage() {

        try {
            if (mService != null && mService.isPlaying()) {
                String uri = mService.getMediaUri();
                isRunning = mService.isPlaying();
                isRecording = mService.isRecording();
                isStreaming = (uri == null || uri.toLowerCase().startsWith("rtmp"));

                showPlayer();
            } else {
                btnPlay.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.icon_play,0,0);
                btnPlay.setText(R.string.button_play);
            }
        } catch (RemoteException ex) {
        }
    }

    private long mStartSeekPos = 0;
    private long mLastSeekEventTime;
    private long mPosOverride = -1;
    private boolean mFromTouch = false;
    private long mDuration;

    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            mLastSeekEventTime = 0;
            mFromTouch = true;
        }
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser || (mService == null)) return;
            long now = SystemClock.elapsedRealtime();
            if ((now - mLastSeekEventTime) > 250) {
                mLastSeekEventTime = now;
                mPosOverride = mDuration * progress / 1000;
                try {
                    mService.seek(mPosOverride);
                } catch (RemoteException ex) {
                }
                // trackball event, allow progress updates
                if (!mFromTouch) {
                    updateProcess();
                    mPosOverride = -1;
                }
            }
        }
        public void onStopTrackingTouch(SeekBar bar) {
            mPosOverride = -1;
            mFromTouch = false;
        }
    };
}
