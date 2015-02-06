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

package wseemann.media.fmpdemo.activity;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import wseemann.media.fmpdemo.service.IMediaPlaybackService;
import wseemann.media.fmpdemo.service.MusicUtils;
import wseemann.media.fmpdemo.service.MusicUtils.ServiceToken;
import wseemann.media.fmpdemo.R;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.dotohsoft.rtmpdump.RTMP;

import org.apache.commons.io.FileUtils;

public class FMPDemo extends FragmentActivity implements ServiceConnection {

    private IMediaPlaybackService mService = null;
	private ServiceToken mToken;
    private static final long MAX_WAIT_TIME = 30000;

    private EditText uriText;
    private Button btnStart;
    private Button btnStop;

    private Button btnToken;

    private Button btnRecord;
    private Button btnPlay;

    enum BUTTON_STAGE {
        STREAMING,
        RECORDING,
        DEFAULT,
        DISABLED,
        PLAYING
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
           // RTMP.init("S:" + mToken, mTmpFile.getAbsolutePath());
        }
    }

    private RTMPRunnable mRTMPRunnable;

    private Handler mRTMPHandler;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            long start = System.currentTimeMillis();
            long flag = System.currentTimeMillis() - start;
            boolean isFound = false;

            while (flag <= MAX_WAIT_TIME) {
                final File tmp = getTempFile();
                if (tmp.exists() && !tmp.canWrite()) {
                    isFound = true;
                    break;
                }
                flag = System.currentTimeMillis() - start;
                //Log.i("RTMP waiting", "Waiting time = " + flag);
            }
            if (isFound) {
            //    Toast.makeText(FMPDemo.this, "Playing ...", Toast.LENGTH_LONG).show();
                Log.i("FMPlayer", "Gogo Playing...");
                try {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    long[] list = new long[1];
                    list[0] = MusicUtils.insert(FMPDemo.this, getTempFile().toURI().toString());
                    mService.open(list, 0);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e("FMPlayer", "Could not fetch audio stream");
                uriText.setEnabled(true);
                btnStart.setEnabled(true);
                btnToken.setEnabled(true);
            }
        }
    };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fmpdemo);
		
		uriText = (EditText) findViewById(R.id.txtToken);
        uriText.setText(getTokenString());
        btnStop = (Button) findViewById(R.id.btnStop);
//        buttonPlay.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.i("wseemann.media.fmpdemo", "Start stream");
//                buttonPlay.setEnabled(false);
//
//              //  RTMP.stop();
//                try {
//                    long[] list = new long[1];
//                    list[0] = MusicUtils.insert(FMPDemo.this, getTempFile().toURI().toString());
//                    mService.open(list, 0);
//                } catch (RemoteException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
		btnToken = (Button) findViewById(R.id.btnGetToken);
        btnToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uriText.setText("");
                btnToken.setEnabled(false);
                btnStart.setEnabled(false);
                uriText.setEnabled(false);
                AsyncTask<Void,Void,Void> getTokenTask = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        final File tmpFile = getTokenFile();
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
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } finally {

                                        }
                                    }
                                });
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btnToken.setEnabled(true);
                                    btnStart.setEnabled(true);
                                    uriText.setEnabled(true);
                                }
                            });

                        }
                        return null;
                    }
                };
                getTokenTask.execute();
            }
        });
    	btnStart = (Button) findViewById(R.id.go_button);
        btnStart.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// Clear the error message
				uriText.setError(null);
				
				// Hide the keyboard
				InputMethodManager imm = (InputMethodManager)
					FMPDemo.this.getSystemService(
					Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(uriText.getWindowToken(), 0);
				
				String uri = uriText.getText().toString();
				
				if (uri.equals("")) {
					uriText.setError(getString(R.string.uri_error));
					return;
				}
                buttonPlay.setEnabled(true);
                Toast.makeText(FMPDemo.this, "Please wait a moment ...", Toast.LENGTH_LONG).show();
				btnStart.setEnabled(false);
                btnToken.setEnabled(false);
                uriText.setEnabled(false);
				String token = uriText.getText().toString();
				File tmp = getTempFile();
//                try {
//                    FileUtils.forceDelete(tmp);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
               // new Thread(runnable).start();

               mRTMPRunnable = new RTMPRunnable(token, tmp);
               new Thread(mRTMPRunnable).start();

			}
		});
    	
        mToken = MusicUtils.bindToService(this, this);
	}
	
	@Override
	public void onDestroy() {
        MusicUtils.unbindFromService(mToken);
        mService = null;
        try {
           // RTMP.stop();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
		super.onDestroy();
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		mService = IMediaPlaybackService.Stub.asInterface(service);
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		finish();
	}

    @Override
    public void onBackPressed() {
        MusicUtils.unbindFromService(mToken);
        mService = null;
       // RTMP.stop();
        this.finish();
       // super.onBackPressed();
    }

    private File getTempFile(String fileName) {
        PackageManager m = this.getPackageManager();
        String s = this.getPackageName();
        try {
            PackageInfo p = m.getPackageInfo(s, 0);
            s = p.applicationInfo.dataDir;
            return new File(s, fileName);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private File getTempFile() {
        return getTempFile("dump.flv");
    }

    private String getTokenString() {
        String token = "";
        File tmp = getTokenFile();
        if (tmp.exists()) {
            try {
                token = FileUtils.readFileToString(tmp).trim();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return token;
    }

    private File getTokenFile() {
        PackageManager m = this.getPackageManager();
        String s = this.getPackageName();
        try {
            PackageInfo p = m.getPackageInfo(s, 0);
            s = p.applicationInfo.dataDir;
            return new File(s, "token.txt");
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
