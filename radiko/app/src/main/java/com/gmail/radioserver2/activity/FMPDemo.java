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

package com.gmail.radioserver2.activity;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.WeakHashMap;

import com.gmail.radioserver2.R;
import com.gmail.radioserver2.service.IMediaPlaybackService;
import com.gmail.radioserver2.service.MusicUtils;
import com.gmail.radioserver2.service.MusicUtils.ServiceToken;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.dotohsoft.rtmpdump.RTMP;
import com.gmail.radioserver2.utils.FileHelper;

import org.apache.commons.io.FileUtils;

public class FMPDemo extends FragmentActivity implements ServiceConnection, View.OnClickListener {

    private IMediaPlaybackService mService = null;

    private ServiceToken mToken;

    private static final long MAX_WAIT_TIME = 30000;

    private EditText uriText;

    private Button btnStart;

    private Button btnStop;

    private Button btnToken;

    private Button btnRecord;

    private Button btnPlay;

    private Button btnStopRecord;

    private File recordedFile;

    private static final String W_REF = "W_REF";

    private static final String TMP_RECORD = "recorded.flv";

    private final WeakHashMap<String, RTMP> mRTMP = new WeakHashMap<String, RTMP>();


    private boolean isRecording = false;
    private boolean isStreaming = false;
    private boolean isPlaying = false;

    enum ButtonStage {
        NO_TOKEN,
        STREAMING,
        RECORDING,
        DEFAULT,
        DISABLED,
        PLAYING
    }

    private ButtonStage buttonStage = ButtonStage.DEFAULT;

    private void switchButtonStage(final ButtonStage stage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (stage) {
                    case NO_TOKEN:
                        uriText.setEnabled(true);
                        btnStart.setEnabled(false);
                        btnStop.setEnabled(false);
                        btnToken.setEnabled(true);
                        btnRecord.setEnabled(false);
                        btnPlay.setEnabled(false);
                        btnStopRecord.setEnabled(false);
                        break;
                    case STREAMING:
                        uriText.setEnabled(false);
                        btnStart.setEnabled(false);
                        btnStop.setEnabled(true);
                        btnToken.setEnabled(false);
                        btnRecord.setEnabled(true);
                        btnPlay.setEnabled(false);
                        btnStopRecord.setEnabled(false);
                        break;
                    case RECORDING:
                        uriText.setEnabled(false);
                        btnStart.setEnabled(false);
                        btnStop.setEnabled(true);
                        btnToken.setEnabled(false);
                        btnRecord.setEnabled(false);
                        btnPlay.setEnabled(false);
                        btnStopRecord.setEnabled(true);
                        break;
                    case DISABLED:
                        uriText.setEnabled(false);
                        btnStart.setEnabled(false);
                        btnStop.setEnabled(false);
                        btnToken.setEnabled(false);
                        btnRecord.setEnabled(false);
                        btnPlay.setEnabled(false);
                        btnStopRecord.setEnabled(false);
                        break;
                    case PLAYING:
                        uriText.setEnabled(false);
                        btnStart.setEnabled(false);
                        btnStop.setEnabled(false);
                        btnToken.setEnabled(false);
                        btnRecord.setEnabled(false);
                        btnPlay.setEnabled(false);
                        btnStopRecord.setEnabled(true);
                        break;
                    case DEFAULT:
                    default:
                        uriText.setEnabled(true);
                        btnStart.setEnabled(true);
                        btnStop.setEnabled(false);
                        btnToken.setEnabled(true);
                        btnRecord.setEnabled(false);
                        btnStopRecord.setEnabled(false);
                        btnPlay.setEnabled(recordedFile.exists());
                        break;
                }
            }
        });
    }

    private void stopRecording() {
        if (mRTMP.size() == 0) return;
        RTMP rtmp = mRTMP.get(W_REF);
        if (rtmp != null) {
            try {
                rtmp.stop();
                mRTMP.remove(W_REF);
            } catch (Exception ex) {

            }
        }
    }

    @Override
    public void onClick(View v) {
        try {
            switch (v.getId()) {
                case R.id.btnPlayRecord:
                    if (recordedFile.exists()) {
                        try {
                            mService.openFile(recordedFile.getAbsolutePath());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        switchButtonStage(ButtonStage.PLAYING);
                        isPlaying = true;
                    }
                    break;
                case R.id.btnStopRecord:
                    if (isRecording) {
                        stopRecording();
                        switchButtonStage(ButtonStage.STREAMING);
                        isRecording = false;
                    }
                    if (mService.isPlaying()) {
                        try {
                            mService.stop();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        isPlaying = false;
                        switchButtonStage(ButtonStage.DEFAULT);
                    }
                    break;
                case R.id.btnRecord:
                    if (isStreaming) {
                        switchButtonStage(ButtonStage.DISABLED);
                        // Clear the error message
                        uriText.setError(null);
                        // Hide the keyboard
                        InputMethodManager imm = (InputMethodManager) FMPDemo.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(uriText.getWindowToken(), 0);
                        String uri = uriText.getText().toString();
                        if (uri.equals("")) {
                            uriText.setError(getString(R.string.uri_error));
                            return;
                        }
                        isRecording = true;
                        switchButtonStage(ButtonStage.RECORDING);
                        new Thread(new RTMPRunnable("S:" + uri, recordedFile)).start();
                    }
                    break;
                case R.id.btnStop:
                    if (isRecording) {
                        stopRecording();
                        isRecording = false;
                    }
                    try {
                        if (mService.isPlaying()) {
                            try {
                                mService.stop();
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                            isStreaming = false;
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    switchButtonStage(ButtonStage.DEFAULT);
                    break;
                case R.id.btnStart:
                    uriText.setError(null);
                    // Hide the keyboard
                    InputMethodManager imm = (InputMethodManager)
                            FMPDemo.this.getSystemService(
                                    Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(uriText.getWindowToken(), 0);
                    final String uri = uriText.getText().toString();
                    if (uri.equals("")) {
                        uriText.setError(getString(R.string.uri_error));
                        return;
                    }
                    switchButtonStage(ButtonStage.DISABLED);
                    try {
                        Log.i("TESTRTMP", "Start streaming");
                        if (mService.isPlaying()) {
                            try {
                                mService.stop();
                            } catch (Exception ex) {
                            }
                        }
                        mService.openFile("rtmp://0.0.0.0:1935/TBS/_definst_/simul-stream.stream|S:" + uri);
                        switchButtonStage(ButtonStage.STREAMING);
                        isStreaming = true;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        isStreaming = false;
                        switchButtonStage(ButtonStage.DEFAULT);
                    }
                    break;
                case R.id.btnGetToken:
                    uriText.setText("");
                    switchButtonStage(ButtonStage.DISABLED);
                    AsyncTask<Void, Void, Void> getTokenTask = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            final File tmpFile = fileHelper.getTokenFile(getClass().getName());
                            try {
                                if (tmpFile.exists())
                                    FileUtils.forceDelete(tmpFile);
                                FileUtils.copyURLToFile(new URL("http://stest.dotohsoft.com/~duc/rad/gettoken/getkey.php"), tmpFile);
                                if (tmpFile.exists()) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                uriText.setText(FileUtils.readFileToString(tmpFile));
                                                switchButtonStage(ButtonStage.DEFAULT);
                                            } catch (IOException e) {
                                                switchButtonStage(ButtonStage.NO_TOKEN);
                                            }

                                        }
                                    });
                                } else {
                                    switchButtonStage(ButtonStage.NO_TOKEN);
                                }

                            } catch (IOException e) {
                                switchButtonStage(ButtonStage.NO_TOKEN);
                            }
                            return null;
                        }
                    };
                    getTokenTask.execute();
                    break;
                default:
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

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
            RTMP rtmp = new RTMP();
            mRTMP.put(W_REF, rtmp);
            rtmp.init(mToken, mTmpFile.getAbsolutePath());
        }
    }

    private RTMPRunnable mRTMPRunnable;

    private FileHelper fileHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fileHelper = new FileHelper(this.getApplicationContext());
        setContentView(R.layout.activity_fmpdemo);

        recordedFile = fileHelper.getTempFile(TMP_RECORD);

        uriText = (EditText) findViewById(R.id.txtToken);
        uriText.setText(fileHelper.getTokenString());

        btnStop = (Button) findViewById(R.id.btnStop);
        btnStop.setOnClickListener(this);

        btnToken = (Button) findViewById(R.id.btnGetToken);
        btnToken.setOnClickListener(this);

        btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(this);

        btnPlay = (Button) findViewById(R.id.btnPlayRecord);
        btnPlay.setOnClickListener(this);

        btnStopRecord = (Button) findViewById(R.id.btnStopRecord);
        btnStopRecord.setOnClickListener(this);

        btnRecord = (Button) findViewById(R.id.btnRecord);
        btnRecord.setOnClickListener(this);

        switchButtonStage(ButtonStage.DISABLED);

        mToken = MusicUtils.bindToService(this, this);
    }

    @Override
    public void onDestroy() {
        MusicUtils.unbindFromService(mToken);
        mService = null;
        stopRecording();
        super.onDestroy();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = IMediaPlaybackService.Stub.asInterface(service);
        try {
            if (mService.isPlaying()) {
                switchButtonStage(ButtonStage.STREAMING);
            } else {
                switchButtonStage(ButtonStage.DEFAULT);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        finish();
    }
}
