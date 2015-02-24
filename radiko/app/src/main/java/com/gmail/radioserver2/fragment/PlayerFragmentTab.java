package com.gmail.radioserver2.fragment;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
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
import com.gmail.radioserver2.radiko.ClientTokenFetcher;
import com.gmail.radioserver2.radiko.ServerTokenFetcher;
import com.gmail.radioserver2.radiko.TokenFetcher;
import com.gmail.radioserver2.service.IMediaPlaybackService;
import com.gmail.radioserver2.service.MusicUtils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
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

    private boolean isTokenLoaded = false;
    private boolean isServiceLoaded = false;

    private String radikoToken;

    private static final String W_REF = "W_REF";

    private final WeakHashMap<String, RTMP> mRTMP = new WeakHashMap<String, RTMP>();

    private MusicUtils.ServiceToken mServiceToken;

    private class RTMPRunnable implements Runnable {
        private final String mToken;
        private final File mTmpFile;
        private RTMPRunnable(String mToken, File mTmpFile) {
            this.mToken = mToken;
            this.mTmpFile = mTmpFile;
        }

        @Override
        public void run() {
            isRecording = true;
            if (mTmpFile.exists()) {
                try {
                    FileUtils.forceDelete(mTmpFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            RTMP rtmp =  new RTMP();
            mRTMP.put(W_REF, rtmp);
            rtmp.init(mToken, mTmpFile.getAbsolutePath());
        }
    }

    private RTMPRunnable mRTMPRunnable;

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

        TokenFetcher tokenFetcher = new ClientTokenFetcher(getActivity(), new TokenFetcher.OnTokenListener() {

            @Override
            public void onTokenFound(final String token) {
                isTokenLoaded = true;
                radikoToken = token;
                showPlayer();
            }

            @Override
            public void onError(final String message, Throwable throwable) {
                isTokenLoaded = true;
                radikoToken = "";
                showPlayer();
            }
        });
        tokenFetcher.fetch();
    }

    @Override
    public void onDestroy() {
        MusicUtils.unbindFromService(mServiceToken);
        mService = null;
        stopRecording();
        super.onDestroy();
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

        switchButtonStage(ButtonStage.DISABLED);
        return v;
    }

    private void showPlayer() {
        if (isServiceLoaded && isTokenLoaded) {
            if (isPlaying) {
                if (isStreaming) {
                    switchButtonStage(ButtonStage.STREAMING);
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

                        btnPlay.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.icon_stop,0,0);
                        btnPlay.setText(R.string.button_stop);
                        btnRecord.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.icon_record,0,0);
                        btnRecord.setText(R.string.button_record);
                        btnRepeat.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.icon_repeat,0,0);
                        btnRepeat.setText(R.string.button_repeat);

                        break;
                    case RECORDING:
                        btnPlay.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.icon_stop,0,0);
                        btnPlay.setText(R.string.button_stop);
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
                        btnPlay.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.icon_stop,0,0);
                        btnPlay.setText(R.string.button_stop);
                        btnRecord.setCompoundDrawablesWithIntrinsicBounds(0,R.drawable.icon_record,0,0);
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
                        btnRepeat.setEnabled(false);
                        btnFast.setEnabled(false);
                        btnSlow.setEnabled(false);
                        btnBack.setEnabled(false);
                        break;
                }
            }
        });
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnBack:
                break;
            case R.id.btnPlay:
                switchButtonStage(ButtonStage.DISABLED);
                if (isPlaying) {
                    try {
                        if (mService.isPlaying()) {
                            mService.stop();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    isPlaying = false;
                    switchButtonStage(ButtonStage.DEFAULT);
                } else {
                    try {
                        Log.i("TESTRTMP", "Start streaming");
                        if (mService.isPlaying()) {
                            try {
                                mService.stop();
                            } catch (Exception ex) {
                            }
                        }
                        mService.openFile("rtmp://0.0.0.0:1935/TBS/_definst_/simul-stream.stream|S:" + radikoToken);
                        switchButtonStage(ButtonStage.STREAMING);
                        isStreaming = true;
                        isPlaying = true;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        isStreaming = false;
                        isPlaying = true;
                        switchButtonStage(ButtonStage.DEFAULT);
                    }
                }
                break;
            case R.id.btnRecord:
                if (isRecording) {
                    if (isStreaming) {
                        switchButtonStage(ButtonStage.STREAMING);
                    } else {
                        switchButtonStage(ButtonStage.DEFAULT);
                    }
                    isRecording = false;
                } else {
                    isRecording = true;
                    switchButtonStage(ButtonStage.RECORDING);
                }
                break;
            case R.id.btnPrev:
                break;
            case R.id.btnNext:
                break;
            case R.id.btnRepeat:
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

    private void stopRecording() {
        if (mRTMP.size() == 0) return;
        RTMP rtmp = mRTMP.get(W_REF);
        if (rtmp != null) {
            try {
                rtmp.stop();
                mRTMP.remove(W_REF);
            }  catch (Exception ex) {

            }
        }
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = IMediaPlaybackService.Stub.asInterface(service);

        try {
            String uri = mService.getMediaUri();
            if (mService.isPlaying()) {
                isPlaying = true;
            } else {
                isPlaying = false;
            }
            if (uri != null && uri.toLowerCase().startsWith("rtmp")) {
                isStreaming = true;
            } else {
                isStreaming = false;
            }
            isServiceLoaded = true;
            showPlayer();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}
