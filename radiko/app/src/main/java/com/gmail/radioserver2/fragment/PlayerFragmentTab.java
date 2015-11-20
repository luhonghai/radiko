package com.gmail.radioserver2.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.dotohsoft.radio.data.RadioProgram;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.activity.TimerSettingsActivity;
import com.gmail.radioserver2.adapter.ProgramListAdapter;
import com.gmail.radioserver2.data.Channel;
import com.gmail.radioserver2.data.Setting;
import com.gmail.radioserver2.service.IMediaPlaybackService;
import com.gmail.radioserver2.service.MediaPlaybackService;
import com.gmail.radioserver2.service.MusicUtils;
import com.gmail.radioserver2.utils.Constants;
import com.gmail.radioserver2.utils.FileHelper;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.gmail.radioserver2.view.CustomSeekBar;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by luhonghai on 2/16/15.
 */

public class PlayerFragmentTab extends FragmentTab implements ServiceConnection, View.OnClickListener {
    /**
     * Screen state
     */
    private TextView txtTitle;

    private TextView txtDescription;

    private ImageButton btnTimer;

    private TextView txtTimer;

    private ImageButton btnBack;

    private TextView txtBack;

    private ImageButton btnPlay;

    private TextView txtPlay;

    private ImageButton btnRecord;

    private TextView txtRecord;

    private ImageButton btnRepeat;

    private TextView txtRepeat;

    private Button btnSlow;

    private Button btnFast;
    private View volumeLowIndicator;
    private ImageButton btnPrev;

    private ImageButton btnNext;

    private CustomSeekBar seekBarPlayer;

    private WebView gifView;

    private IMediaPlaybackService mService = null;

    private MusicUtils.ServiceToken mServiceToken;

    private Setting setting;
    private ListView lvProgramList;
    private ProgramListAdapter programListAdapter;
    private LinearLayout btShare;
    private RadioProgram.Program currentProgram;
    private View tvLoading;

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        txtTitle = null;
        txtDescription = null;
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
        lvProgramList = null;
        btShare = null;
        destroyWebView(gifView);
        gifView = null;
        if (mAdView != null) {
            mAdView.destroy();
            mAdView = null;
        }
    }

    void destroyWebView(WebView wv) {
//        wv.stopLoading();
//
//        wv.clearFormData();
//        wv.clearAnimation();
//        wv.clearDisappearingChildren();
//        wv.clearView();
//        wv.clearHistory();
//        wv.destroyDrawingCache();
//        wv.freeMemory();
//        wv.destroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mServiceToken = MusicUtils.bindToService(getActivity(), this);
        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
        f.addAction(MediaPlaybackService.META_CHANGED);
        f.addAction(MediaPlaybackService.META_VOLUME_TOO_SMALL);
        f.addAction(MediaPlaybackService.META_VOLUME_NORMAL);
        getActivity().registerReceiver(mStatusListener, new IntentFilter(f));
    }

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

    private AdView mAdView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_player_tab, container, false);
        mAdView = (AdView) v.findViewById(R.id.adView);
        if (mAdView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }
        setting = new Setting(getActivity());
        setting.load();
        tvLoading = v.findViewById(R.id.tvLoading);

        gifView = (WebView) v.findViewById(R.id.gifView);
        gifView.setVisibility(View.INVISIBLE);
        loadGifLoader();
        volumeLowIndicator = v.findViewById(R.id.tvWarningVolumeLow);
        btnBack = (ImageButton) v.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(this);

        txtBack = (TextView) v.findViewById(R.id.txtBack);
        txtBack.setOnClickListener(this);

        btnTimer = (ImageButton) v.findViewById(R.id.btnTimer);
        btnTimer.setOnClickListener(this);

        txtTimer = (TextView) v.findViewById(R.id.txtTimer);
        txtTimer.setOnClickListener(this);

        btnSlow = (Button) v.findViewById(R.id.btnSlow);
        btnSlow.setOnClickListener(this);
        btnFast = (Button) v.findViewById(R.id.btnFast);
        btnFast.setOnClickListener(this);

        btnRecord = (ImageButton) v.findViewById(R.id.btnRecord);
        btnRecord.setOnClickListener(this);

        txtRecord = (TextView) v.findViewById(R.id.txtRecord);
        txtRecord.setOnClickListener(this);

        btnRepeat = (ImageButton) v.findViewById(R.id.btnRepeat);
        btnRepeat.setOnClickListener(this);

        txtRepeat = (TextView) v.findViewById(R.id.txtRepeat);
        txtRepeat.setOnClickListener(this);

        btnPlay = (ImageButton) v.findViewById(R.id.btnPlay);
        btnPlay.setOnClickListener(this);

        txtPlay = (TextView) v.findViewById(R.id.txtPlay);
        txtPlay.setOnClickListener(this);

        btnPrev = (ImageButton) v.findViewById(R.id.btnPrev);
        btnPrev.setOnClickListener(this);
        btnNext = (ImageButton) v.findViewById(R.id.btnNext);
        btnNext.setOnClickListener(this);
        txtTitle = (TextView) v.findViewById(R.id.txtTitle);
        txtDescription = (TextView) v.findViewById(R.id.txtDescription);

        lvProgramList = (ListView) v.findViewById(R.id.lvProgram);
        programListAdapter = new ProgramListAdapter(getActivity());
        lvProgramList.setAdapter(programListAdapter);
        btShare = (LinearLayout) v.findViewById(R.id.btShare);
        btShare.setOnClickListener(this);
        seekBarPlayer = (CustomSeekBar) v.findViewById(R.id.seekBarPlayer);
        seekBarPlayer.setMax(1000);
        seekBarPlayer.setOnSeekBarChangeListener(mSeekListener);
        switchButtonStage(ButtonStage.DISABLED);
        showPlayerInit();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) mAdView.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAdView != null) mAdView.pause();
    }

    private void loadGifLoader() {
        FileHelper fileHelper = new FileHelper(getActivity());
        File loader = fileHelper.getTempFile("loader.gif");
        if (!loader.exists()) {
            try {
                FileUtils.copyInputStreamToFile(getResources().openRawResource(R.raw.loader), loader);
            } catch (IOException e) {
                SimpleAppLog.error("Could not generate loader.gif", e);
            }
        }
        StringBuffer sb = new StringBuffer();
        sb.append("<style>body{margin:0;padding:0;background:#dce6f0}</style><div style='background:dce6f0;margin:0;padding:0;text-align:center;vertical-align:middle;'>");
        sb.append("<img style='margin:0 auto' src='").append("data:image/gif;base64,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").append("'/>");
        sb.append("</div>");
        String html = sb.toString();
        //SimpleAppLog.info("Gifview html: " + html);
        if (gifView != null) {
            gifView.loadData(html, "text/html", "UTF-8");
        }
    }

    private void showProgramList(RadioProgram programs, boolean refresh) {
        if (programs != null) {
            if (programListAdapter.getCount() == 0 || refresh) {
                List<RadioProgram.Program> list = programs.getPrograms();
                List<RadioProgram.Program> programList = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    RadioProgram.Program fake = new RadioProgram.Program();
                    programList.add(fake);
                }
                for (RadioProgram.Program item : list) {
                    programList.add(item);
                }
                for (int i = 0; i < 3; i++) {
                    RadioProgram.Program fake = new RadioProgram.Program();
                    programList.add(fake);
                }
                programListAdapter.setList(programList);
                programListAdapter.notifyDataSetChanged();
                TimeZone tz = TimeZone.getTimeZone("GTM+9");
                Calendar calNow = Calendar.getInstance(tz);
                Calendar calFrom = Calendar.getInstance(tz);
                Calendar calTo = Calendar.getInstance(tz);
                int index = 0;
                for (RadioProgram.Program item : programList) {
                    calFrom.setTimeInMillis(item.getFromTime());
                    calTo.setTimeInMillis(item.getToTime());
                    if (calNow.getTimeInMillis() >= calFrom.getTimeInMillis() && calNow.getTimeInMillis() <= calTo.getTimeInMillis()) {
                        currentProgram = (RadioProgram.Program) programListAdapter.getItem(index);
                        lvProgramList.setSelection(index);
                        int height = lvProgramList.getHeight();
                        int viewHeight = getItemHeightOfListView(lvProgramList, index);
                        Log.d("View height", height + " " + viewHeight);
                        lvProgramList.setSelectionFromTop(index + 1, height / 2 - viewHeight / 2);
                        break;
                    }
                    index++;
                }
            }
        }
    }

    public int getItemHeightOfListView(ListView listView, int items) {
        ListAdapter mAdapter = listView.getAdapter();
        int listviewElementsHeight = 0;
        View childView = mAdapter.getView(items, null, listView);
        childView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        listviewElementsHeight += childView.getMeasuredHeight();
        return listviewElementsHeight;
    }

    private void showProgramInfo(RadioProgram.Program program) {
        if (program != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            try {
                if (currentProgram != null) {
                    if (currentProgram.getFromTime() != program.getFromTime()) {
                        showProgramList(selectedChannel.getProgram(), true);
                        SimpleAppLog.info("Change program " + currentProgram.getFromTime()
                                + " " + sdf.format(new Date(currentProgram.getFromTime()))
                                + " " + program.getFromTime() + " " + sdf.format(new Date(program.getFromTime())));
                    }
                } else {
                    showProgramList(selectedChannel.getProgram(), true);
                }
            } catch (Exception e) {
                //
            }
            final StringBuffer sb = new StringBuffer();
            sb.append(getString(R.string.current_program)).append(":\n");
            sb.append(program.getTitle()).append("\n");

            sb.append(sdf.format(new Date(program.getFromTime())));
            sb.append(" - ").append(sdf.format(new Date(program.getToTime())));
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (getActivity() == null || !PlayerFragmentTab.this.isAdded()) return;
                    if (txtDescription != null)
                        txtDescription.setText(sb.toString());
                }
            });
        } else {
            if (txtDescription != null)
                txtDescription.setText("");
        }
    }

    private void showABState() {
        try {
            if (mService != null && mService.isPlaying() && !mService.isStreaming()) {
                float posA = -1;
                float posB = -1;
                if (mService.getStateAB() != MediaPlaybackService.ABState.STOP
                        && mService.getStateAB() != MediaPlaybackService.ABState.ERROR) {
                    long duration = mService.duration();
                    if (mService.getAPos() != -1) {
                        posA = (float) mService.getAPos() / duration;
                    }
                    if (mService.getBPos() != -1) {
                        posB = (float) mService.getBPos() / duration;
                    }
                }
                if (seekBarPlayer != null) {
                    seekBarPlayer.setPosA(posA);
                    seekBarPlayer.setPosB(posB);
                    seekBarPlayer.invalidate();
                }
            }
        } catch (Exception e) {
            SimpleAppLog.error("Could not update ABState", e);
        }
    }

    private Channel selectedChannel;

    private void updateInformation() {
        if (mService == null) return;
        String obj = null;
        selectedChannel = null;
        try {
            obj = mService.getChannelObject();
            if (obj != null && obj.length() > 0) {
                try {
                    Gson gson = new Gson();
                    selectedChannel = gson.fromJson(obj, Channel.class);
                } catch (Exception e) {
                    SimpleAppLog.error("Could not parse channel", e);
                }
            }
        } catch (RemoteException e) {
            SimpleAppLog.error("Could not get channel object", e);
        }
        if (selectedChannel != null) {
            if (txtTitle != null)
                txtTitle.setText(selectedChannel.getName());
            showProgramInfo(selectedChannel.getCurrentProgram());
            try {
                if (mService != null && mService.isStreaming()) {
                    if (lvProgramList != null) {
                        lvProgramList.setVisibility(View.VISIBLE);
                        showProgramList(selectedChannel.getProgram(), false);
                    }
                } else {
                    if (lvProgramList != null) {
                        lvProgramList.setVisibility(View.GONE);
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            if (txtTitle != null)
                txtTitle.setText("");
            if (txtDescription != null)
                txtDescription.setText("");
        }
    }

    private void showPlayerInit() {
        try {
            btnPlay.setImageResource(R.drawable.btn_pause_gray);
            txtPlay.setText(R.string.button_pause);
            btnPlay.setEnabled(false);
            txtPlay.setEnabled(false);
        } catch (Exception e) {
            //
        }
    }

    private void showPlayer() {
        if (btnRepeat == null || !this.isAdded()) return;
        try {
            btnRepeat.setEnabled(false);
            btnFast.setEnabled(false);
            btnSlow.setEnabled(false);
            btnBack.setEnabled(false);
            btnRecord.setEnabled(false);
            btnPlay.setEnabled(false);
            btnNext.setEnabled(false);
            btnPrev.setEnabled(false);

            txtPlay.setEnabled(false);
            txtRepeat.setEnabled(false);
            txtRecord.setEnabled(false);
            txtBack.setEnabled(false);
            txtTimer.setEnabled(false);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                btnFast.setVisibility(View.VISIBLE);
                btnSlow.setVisibility(View.VISIBLE);
            } else {
                btnFast.setVisibility(View.INVISIBLE);
                btnSlow.setVisibility(View.INVISIBLE);
            }

            btnNext.setVisibility(View.INVISIBLE);
            btnPrev.setVisibility(View.INVISIBLE);
            seekBarPlayer.setVisibility(View.GONE);
            seekBarPlayer.setEnabled(false);
            if (mService != null) {
                btnPlay.setEnabled(true);
                txtPlay.setEnabled(true);
                txtRecord.setText(R.string.button_record);
                if (mService.isPlaying()) {
                    if (mService.isStreaming()) {
                        btnRepeat.setEnabled(!mService.isStreaming());
                        txtRepeat.setEnabled(!mService.isStreaming());
                        btnBack.setEnabled(!mService.isStreaming());
                        txtBack.setEnabled(!mService.isStreaming());
                        btnFast.setEnabled(!mService.isStreaming());
                        btnSlow.setEnabled(!mService.isStreaming());
                        btnPlay.setImageResource(R.drawable.btn_pause);
                        txtPlay.setText(R.string.button_pause);

                        if (mService.isSoundPlaying()) {
                            if (tvLoading.isShown()) {
                                tvLoading.setVisibility(View.GONE);
                            }
                            if (!gifView.isShown()) {
                                gifView.setVisibility(View.VISIBLE);
                            }
                        } else {
                            if (!tvLoading.isShown()) {
                                tvLoading.setVisibility(View.VISIBLE);
                            }
                            if (gifView.isShown()) {
                                gifView.setVisibility(View.GONE);
                            }
                        }
                        btnRepeat.setImageResource(R.drawable.btn_repeat_gray);
                        btnRecord.setImageResource(R.drawable.btn_record);
                        btnBack.setImageResource(R.drawable.btn_back_gray);
                        btnRecord.setEnabled(true);
                        txtRecord.setEnabled(true);
                        if (mService.isRecording()) {
                            btnRecord.setImageResource(R.drawable.btn_stop);
                            txtRecord.setText(R.string.button_stop);
                        }
                    } else {
                        if (tvLoading.isShown()) {
                            tvLoading.setVisibility(View.GONE);
                        }
                        if (gifView.isShown()) {
                            gifView.setVisibility(View.GONE);
                        }
                        btnRepeat.setImageResource(R.drawable.btn_repeat);
                        btnBack.setImageResource(R.drawable.btn_back);
                        btnRecord.setImageResource(R.drawable.btn_record_gray);
                        seekBarPlayer.setVisibility(View.VISIBLE);
                        seekBarPlayer.setEnabled(true);
                        btnPrev.setVisibility(View.VISIBLE);
                        btnNext.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (tvLoading.isShown()) {
                        tvLoading.setVisibility(View.GONE);
                    }
                    if (gifView.isShown()) {
                        gifView.setVisibility(View.GONE);
                    }
                    if (mService.getChannelObject() == null || mService.getChannelObject().length() == 0) {
                        btnPlay.setImageResource(R.drawable.btn_pause_gray);
                        txtPlay.setText(R.string.button_pause);
                        btnPlay.setEnabled(false);
                        txtPlay.setEnabled(false);
                        if (!mService.isStreaming()) {
                            seekBarPlayer.setVisibility(View.VISIBLE);
                            btnPrev.setVisibility(View.VISIBLE);
                            btnNext.setVisibility(View.VISIBLE);
                        }
                    } else {
                        btnPlay.setImageResource(R.drawable.btn_play);
                        txtPlay.setText(R.string.button_play);
                        if (!mService.isStreaming()) {
                            seekBarPlayer.setVisibility(View.VISIBLE);
                            btnPrev.setVisibility(View.VISIBLE);
                            btnNext.setVisibility(View.VISIBLE);
                        }
                    }
                }
                if (mService.isStreaming()) {
                    btnNext.setImageResource(R.drawable.btn_next_gray);
                    btnPrev.setImageResource(R.drawable.btn_prev_gray);
                } else {
                    btnNext.setImageResource(R.drawable.btn_next);
                    btnPrev.setImageResource(R.drawable.btn_prev);
                }
                btnNext.setEnabled(!mService.isStreaming());
                btnPrev.setEnabled(!mService.isStreaming());

                txtRepeat.setText(R.string.button_repeat);
                if (mService.isPlaying() && !mService.isStreaming()) {
                    btnRepeat.setEnabled(true);
                    txtRepeat.setEnabled(true);
                    switch (mService.getStateAB()) {
                        case MediaPlaybackService.ABState.PLAY:
                            btnRepeat.setImageResource(R.drawable.btn_stop);
                            break;
                        case MediaPlaybackService.ABState.STOP:
                        case MediaPlaybackService.ABState.ERROR:
                        case MediaPlaybackService.ABState.FLAG:
                            btnRepeat.setImageResource(R.drawable.btn_repeat);
                            break;
                    }
                } else {
                    btnRepeat.setEnabled(false);
                    txtRepeat.setEnabled(false);
                    switch (mService.getStateAB()) {
                        case MediaPlaybackService.ABState.PLAY:
                            btnRepeat.setImageResource(R.drawable.btn_stop_gray);
                            break;
                        case MediaPlaybackService.ABState.STOP:
                        case MediaPlaybackService.ABState.ERROR:
                        case MediaPlaybackService.ABState.FLAG:
                            btnRepeat.setImageResource(R.drawable.btn_repeat_gray);
                            break;
                    }
                }

                if (mService.getChannelObject() != null && mService.getChannelObject().length() > 0) {
                    btnTimer.setEnabled(true);
                    txtTimer.setEnabled(true);
                    btnTimer.setImageResource(R.drawable.btn_timer);
                } else {
                    btnTimer.setEnabled(false);
                    txtTimer.setEnabled(false);
                    btnTimer.setImageResource(R.drawable.btn_timer_gray);
                }
                showABState();
            }
        } catch (Exception e) {
            SimpleAppLog.error("Could to change player state", e);
        }
    }

    enum ButtonStage {
        DEFAULT,
        DISABLED
    }

    private void switchButtonStage(final ButtonStage stage) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (stage) {
                    case DISABLED:
                        btnRecord.setEnabled(false);
                        btnPlay.setEnabled(false);
                        btnRepeat.setEnabled(false);
                        btnFast.setEnabled(false);
                        btnSlow.setEnabled(false);
                        btnBack.setEnabled(false);
                        btnNext.setEnabled(false);
                        btnPrev.setEnabled(false);

                        txtPlay.setEnabled(false);
                        txtRepeat.setEnabled(false);
                        txtRecord.setEnabled(false);
                        txtBack.setEnabled(false);
                        txtTimer.setEnabled(false);
                        txtTimer.setEnabled(false);

                        btnRecord.setImageResource(R.drawable.btn_record_gray);
                        btnPlay.setImageResource(R.drawable.btn_play_gray);
                        btnRepeat.setImageResource(R.drawable.btn_repeat_gray);
                        btnBack.setImageResource(R.drawable.btn_back_gray);

                        btnNext.setImageResource(R.drawable.btn_next_gray);
                        btnPrev.setImageResource(R.drawable.btn_prev_gray);
                        break;
                    case DEFAULT:
                    default:
                        btnPlay.setImageResource(R.drawable.btn_play);
                        txtPlay.setText(R.string.button_play);
                        btnPlay.setEnabled(true);
                        txtPlay.setEnabled(true);
                        btnRecord.setImageResource(R.drawable.btn_record_gray);
                        txtRecord.setText(R.string.button_record);
                        btnRecord.setEnabled(false);

                        btnRepeat.setImageResource(R.drawable.btn_repeat);
                        txtRepeat.setText(R.string.button_repeat);
                        if (mService != null) {
                            try {
                                boolean isStreaming = mService.isStreaming();
                                btnRepeat.setEnabled(!isStreaming);
                                btnFast.setEnabled(!isStreaming);
                                btnSlow.setEnabled(!isStreaming);
                                btnBack.setEnabled(!isStreaming);
                            } catch (Exception ex) {
                                SimpleAppLog.error("Could not update state streaming", ex);
                            }
                        }
                        break;
                }
            }
        });
    }

    private String lastChannelObject;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.txtBack:
            case R.id.btnBack:
                try {
                    if (!mService.isStreaming() && mService.isPlaying()) {
                        Setting setting = new Setting(getActivity());
                        setting.load();
                        mService.doBack(Math.round(setting.getBackLength()));
                    }
                } catch (RemoteException e) {
                    SimpleAppLog.error("Could not back", e);
                }
                break;
            case R.id.txtPlay:
            case R.id.btnPlay:
                switchButtonStage(ButtonStage.DISABLED);
                try {
                    if (mService.isPlaying()) {
                        if (mService.isStreaming()) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        lastChannelObject = mService.getChannelObject();
                                        if (mService.isRecording()) {
                                            mService.stopRecord();
                                        }
                                        mService.stop();
//                                        if (mService != null) {
//                                            mService.setChannelObject("");
//                                        }
                                    } catch (Exception e) {
                                        SimpleAppLog.error("Could not stop stream", e);
                                    }
                                    try {
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                showPlayer();
                                            }
                                        });
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                        } else {
                            mService.pause();
                            showPlayer();
                        }
                    } else {
                        try {
                            if (mService.isStreaming() && mService.isRecording())
                                mService.stopRecord();
                        } catch (Exception ex) {
                            SimpleAppLog.error("Could not stop recording", ex);
                        }
                        if (mService.isStreaming() && mService.isPlaying()) {
                            mService.stop();
                        }
                        if (mService.isStreaming()) {
                            lastChannelObject = mService.getChannelObject();
                            if (lastChannelObject != null && lastChannelObject.length() > 0)
                                mService.openStream("", lastChannelObject);
                        } else {
                            mService.play();
                        }
                        showPlayer();
                        showPlayerInit();
                    }
                } catch (Exception e) {
                    switchButtonStage(ButtonStage.DEFAULT);
                }

                break;
            case R.id.txtRecord:
            case R.id.btnRecord:
                switchButtonStage(ButtonStage.DISABLED);
                try {
                    if (mService.isRecording()) {
                        mService.stopRecord();
                    } else {
                        try {
                            mService.startRecord("", "");
                        } catch (RemoteException e) {
                            SimpleAppLog.error("Could not start recording", e);
                        }
                    }
                } catch (RemoteException e) {
                    SimpleAppLog.error("Could not stop recording", e);
                }
                showPlayer();
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
            case R.id.txtRepeat:
            case R.id.btnRepeat:
                try {
                    switch (mService.getStateAB()) {
                        case MediaPlaybackService.ABState.FLAG:
                            mService.markAB();
                            break;
                        case MediaPlaybackService.ABState.PLAY:
                            mService.stopAB();
                            break;
                        case MediaPlaybackService.ABState.STOP:
                            mService.markAB();
                            break;
                    }
                } catch (RemoteException e) {
                    SimpleAppLog.error("Could call AB state", e);
                }
                showPlayer();
                break;
            case R.id.btnSlow:
                try {
                    if (mService.isPlaying()) {
                        if (mService.isSlow()) {
                            mService.stopSlow();
                        } else {
                            mService.doSlow(setting.getSlowLevel());
                        }
                    }
                } catch (Exception ex) {
                    SimpleAppLog.error("Could call slow", ex);
                }
                break;
            case R.id.btnFast:
                try {
                    if (mService.isPlaying()) {
                        if (mService.isFast()) {
                            mService.stopFast();
                        } else {
                            mService.doFast(setting.getFastLevel());
                        }
                    }
                } catch (Exception ex) {
                    SimpleAppLog.error("Could call fast", ex);
                }
                break;
            case R.id.txtTimer:
            case R.id.btnTimer:
                if (mService != null) {
                    try {
                        if (mService.getChannelObject() != null && mService.getChannelObject().length() > 0) {
                            Intent intent = new Intent();
                            intent.setClass(getActivity(), TimerSettingsActivity.class);
                            intent.putExtra(Constants.ARG_OBJECT, mService.getChannelObject());
                            startActivity(intent);
                        }
                    } catch (RemoteException e) {
                        SimpleAppLog.error("Could not get current channel", e);
                    }
                }
                break;
            case R.id.btShare: {
                shareProgram();
            }
            break;
        }
    }

    private void shareProgram() {
        Intent intent = new Intent(Constants.INTENT_FILTER_FRAGMENT_ACTION);
        intent.putExtra(Constants.FRAGMENT_ACTION_TYPE, Constants.ACTION_SHARE_FACEBOOK);
        getActivity().sendBroadcast(intent);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = IMediaPlaybackService.Stub.asInterface(service);
        updateInformation();
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
                SimpleAppLog.info("On player meta changed");
                showPlayer();
                updateInformation();
                queueNextRefresh(1);
            } else if (action.equals(MediaPlaybackService.PLAYSTATE_CHANGED)) {
                SimpleAppLog.info("On playstate changed");
                showPlayer();
                updateInformation();
            } else if (action.equals(MediaPlaybackService.META_VOLUME_TOO_SMALL)) {
                if (volumeLowIndicator != null && !volumeLowIndicator.isShown()) {
                    volumeLowIndicator.setVisibility(View.VISIBLE);
                }
                getActivity().removeStickyBroadcast(intent);
            } else if (action.equals(MediaPlaybackService.META_VOLUME_NORMAL)) {
                if (volumeLowIndicator != null && volumeLowIndicator.isShown()) {
                    volumeLowIndicator.setVisibility(View.GONE);
                }
                getActivity().removeStickyBroadcast(intent);
            }
        }
    };

    private long refreshNow() {
        if (mService == null || seekBarPlayer == null)
            return 500;
        try {
            if (mService.isStreaming()) return -1;
            if (!mService.isPlaying()) return 500;
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
            // calculate the number of milliseconds until the next full second, so
            // the counter can be updated at just the right time
            long remaining = 1000 - (pos % 1000);
            // approximate how often we would need to refresh the slider to
            // move it smoothly
            int width = seekBarPlayer.getWidth();
            if (width == 0) width = 320;
            long smoothrefreshtime = mDuration / width;

            if (smoothrefreshtime > remaining) return remaining;
            if (smoothrefreshtime < 20) return 20;
            return smoothrefreshtime;
        } catch (RemoteException ex) {
        }
        return 500;
    }

    private static final int REFRESH = 1;
    private static final int QUIT = 2;
    private boolean paused;

    @Override
    public void onStart() {
        super.onStart();
        paused = false;
        long next = refreshNow();
        queueNextRefresh(next);
    }

    @Override
    public void onStop() {
        super.onStop();
        paused = true;
        mHandler.removeMessages(REFRESH);
    }

    private void queueNextRefresh(long delay) {
        if (!paused) {
            Message msg = mHandler.obtainMessage(REFRESH);
            mHandler.removeMessages(REFRESH);
            if (delay != -1)
                mHandler.sendMessageDelayed(msg, delay);
        }
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH:
                    long next = refreshNow();
                    queueNextRefresh(next);
                    break;
                case QUIT:
                    // This can be moved back to onCreate once the bug that prevents
                    // Dialogs from being started from onCreate/onResume is fixed.
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.service_start_error_title)
                            .setMessage(R.string.service_start_error_msg)
                            .setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            getActivity().finish();
                                        }
                                    })
                            .setCancelable(false)
                            .show();
                    break;

                default:
                    break;
            }
        }
    };


    private long mPosOverride = -1;

    private long mDuration;

    private long mLastSeekEventTime;
    private boolean mFromTouch = false;

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
                    //
                }
                // trackball event, allow progress updates
                if (!mFromTouch) {
                    refreshNow();
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
