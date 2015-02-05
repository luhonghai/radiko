package com.dotohsoft.rtmpdump;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class Rtmpdump extends Activity {
    
	boolean run = false;
	private ProgressBar progress;
	private EditText etUrl;
	private EditText etDest;
	
	private String url;
	private String dest;
    private final RTMPSuck rtmpSuck = new RTMPSuck();
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Button btStart = (Button) findViewById(R.id.start);
        Button btStop = (Button) findViewById(R.id.stop);
        
        etUrl = (EditText) findViewById(R.id.url);
        etDest = (EditText) findViewById(R.id.dest);
        
        progress = (ProgressBar) findViewById(R.id.progress);
        progress.setVisibility(View.INVISIBLE);
        
        progress.setVisibility(View.INVISIBLE);

        btStart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				url = etUrl.getText().toString().trim();
				dest = etDest.getText().toString().trim();
				if (! run && url != "" && dest != "") {
					progress.setVisibility(View.VISIBLE);
					new Thread(new Runnable() {
						@Override
						public void run() {
                            File file = new File(dest.substring(0, dest.lastIndexOf("/")));
                            if (file.exists() && file.isDirectory()) {
                                try {
                                    File target = new File(dest);
                                    if (target.exists()) {
                                        FileUtils.forceDelete(new File(dest));
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                rtmpSuck.init("S:nco2hYgUPMg9s6VQFbJBlA", dest);
                            } else {
                                Log.e("RTMTDUMP","Could not found dictionary: " + file.getAbsolutePath());
                            }
						}
					}).start();
					
					run = true;
				}
			}
		});
        
        btStop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				progress.setVisibility(View.INVISIBLE);
                rtmpSuck.stop();
				run = false;
			}
		});
    }
}