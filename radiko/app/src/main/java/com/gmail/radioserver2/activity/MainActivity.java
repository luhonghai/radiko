package com.gmail.radioserver2.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.dotohsoft.radio.Constant;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.data.Channel;
import com.gmail.radioserver2.data.Library;
import com.gmail.radioserver2.fragment.HomeFragmentTab;
import com.gmail.radioserver2.fragment.LibraryFragmentTab;
import com.gmail.radioserver2.fragment.PlayerFragmentTab;
import com.gmail.radioserver2.fragment.RecordedProgramFragmentTab;
import com.gmail.radioserver2.fragment.SettingFragmentTab;
import com.gmail.radioserver2.radiko.TokenFetcher;
import com.gmail.radioserver2.service.DataPrepareService;
import com.gmail.radioserver2.service.IMediaPlaybackService;
import com.gmail.radioserver2.service.MusicUtils;
import com.gmail.radioserver2.service.TimerManagerReceiver;
import com.gmail.radioserver2.utils.Constants;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.gmail.radioserver2.view.ReclickableTabHost;
import com.google.gson.Gson;

import io.fabric.sdk.android.Fabric;
import java.util.HashMap;
import java.util.Stack;

/**
 * Created by luhonghai on 2/16/15.
 */
public class MainActivity extends BaseFragmentActivity implements ServiceConnection {
    /* Your Tab host */
    private ReclickableTabHost mTabHost;

    /* A HashMap of stacks, where we use tab identifier as keys..*/
    private HashMap<String, Stack<Fragment>> mStacks;

    /*Save current tabs identifier in this..*/
    private String mCurrentTab;

    private Library selectedLibrary;

    private IMediaPlaybackService mService = null;

    private MusicUtils.ServiceToken mServiceToken;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                DataPrepareService prepareService = new DataPrepareService(MainActivity.this);
                prepareService.execute();
                return null;
            }
        }.execute();

        // Start timer
        Intent intent = new Intent(TimerManagerReceiver.ACTION_START_TIMER);
        sendBroadcast(intent);

        setContentView(R.layout.activity_main);

        /*
         *  Navigation stacks for each tab gets created..
         *  tab identifier is used as key_bin to get respective stack for each tab
         */
        mStacks             =   new HashMap<String, Stack<Fragment>>();
        mStacks.put(Constants.TAB_HOME, new Stack<Fragment>());
        mStacks.put(Constants.TAB_RECORDED_PROGRAM, new Stack<Fragment>());
        mStacks.put(Constants.TAB_LIBRARY, new Stack<Fragment>());
        mStacks.put(Constants.TAB_SETTING, new Stack<Fragment>());
        mStacks.put(Constants.TAB_PLAY_SCREEN, new Stack<Fragment>());

        mTabHost                =   (ReclickableTabHost)findViewById(android.R.id.tabhost);
        mTabHost.setOnTabChangedListener(listener);
        mTabHost.setOnReclickTabListener(reclickTabListener);
        mTabHost.setup();
        initializeTabs();
        mServiceToken = MusicUtils.bindToService(this, this);
        registerReceiver(mHandleAction, new IntentFilter(Constants.INTENT_FILTER_FRAGMENT_ACTION));
        checkWifiPolicy();
    }

    private boolean checkWifiPolicy() {
        WifiManager wm = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        if(!wm.isWifiEnabled())
        {
            return true;
        }
        ContentResolver cr = this.getContentResolver();
        int policyNever = android.provider.Settings.System.WIFI_SLEEP_POLICY_NEVER;
        try
        {
            android.provider.Settings.System.putInt(cr, Settings.System.WIFI_SLEEP_POLICY, policyNever);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        try
        {
            if(android.provider.Settings.System.getInt(cr, Settings.System.WIFI_SLEEP_POLICY) != policyNever)
            {
                new AlertDialog.Builder(this).setTitle(getString(R.string.wifi_policy_warning_title))
                        .setMessage(getString(R.string.wifi_policy_warning_message))
                        .setPositiveButton(getString(R.string.wifi_policy_warning_positive_button), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(Settings.ACTION_WIFI_IP_SETTINGS));
                            }
                        })
                        .setNegativeButton(getString(R.string.wifi_policy_warning_negative_button), null)
                        .show();
                return false;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    private void showTimer() {
        Intent intent = new Intent();
        intent.setClass(this, TimerSettingsActivity.class);
        startActivity(intent);
    }

    private View createTabView(final int stringId) {
        View view = LayoutInflater.from(this).inflate(R.layout.tabs_main, null);
        TextView imageView =   (TextView) view.findViewById(R.id.txtTabName);
        imageView.setText(getResources().getString(stringId));
        return view;
    }

    public void initializeTabs(){
        /* Setup your tab icons and content views.. Nothing special in this..*/
        TabHost.TabSpec spec    =   mTabHost.newTabSpec(Constants.TAB_HOME);
        mTabHost.setCurrentTab(-3);
        spec.setContent(new TabHost.TabContentFactory() {
            public View createTabContent(String tag) {
                return findViewById(R.id.realtabcontent);
            }
        });
        spec.setIndicator(createTabView(R.string.tab_home));
        mTabHost.addTab(spec);


        spec                    =   mTabHost.newTabSpec(Constants.TAB_RECORDED_PROGRAM);
        spec.setContent(new TabHost.TabContentFactory() {
            public View createTabContent(String tag) {
                return findViewById(R.id.realtabcontent);
            }
        });
        spec.setIndicator(createTabView(R.string.tab_recorded_program));
        mTabHost.addTab(spec);

        spec                    =   mTabHost.newTabSpec(Constants.TAB_LIBRARY);
        spec.setContent(new TabHost.TabContentFactory() {
            public View createTabContent(String tag) {
                return findViewById(R.id.realtabcontent);
            }
        });
        spec.setIndicator(createTabView(R.string.tab_library));
        mTabHost.addTab(spec);

        spec                    =   mTabHost.newTabSpec(Constants.TAB_SETTING);
        spec.setContent(new TabHost.TabContentFactory() {
            public View createTabContent(String tag) {
                return findViewById(R.id.realtabcontent);
            }
        });
        spec.setIndicator(createTabView(R.string.tab_setting));
        mTabHost.addTab(spec);
        setSelectedTabColor();
    }

    ReclickableTabHost.OnReclickTabListener reclickTabListener = new ReclickableTabHost.OnReclickTabListener() {
        @Override
        public void onReclick(int index) {
            if (index == Constants.TAB_HOME_ID) {
                if (mStacks.get(Constants.TAB_HOME).size() == 0){
                    pushFragments(Constants.TAB_HOME, new HomeFragmentTab(), false,true);
                } else {
                    pushFragments(Constants.TAB_HOME, mStacks.get(Constants.TAB_HOME).lastElement(), false,false);
                }
            } else if (index == Constants.TAB_RECORDED_PROGRAM_ID) {
                selectedLibrary = null;
                RecordedProgramFragmentTab recordedProgramFragmentTab = new RecordedProgramFragmentTab();
                recordedProgramFragmentTab.setSelectedLibrary(null);
                pushFragments(Constants.TAB_RECORDED_PROGRAM, recordedProgramFragmentTab, false, false);
            }
        }
    };

    /*Comes here when user switch tab, or we do programmatically*/
    TabHost.OnTabChangeListener listener    =   new TabHost.OnTabChangeListener() {
        public void onTabChanged(String tabId) {
        /*Set current tab..*/
            mCurrentTab                     =   tabId;
            if (!tabId.equals(Constants.TAB_RECORDED_PROGRAM)) {
                selectedLibrary = null;
            }
            if (mStacks.get(tabId).size() == 0){
          /*
           *    First time this tab is selected. So add first fragment of that tab.
           *    Dont need animation, so that argument is false.
           *    We are adding a new fragment which is not present in stack. So add to stack is true.
           */
                if(tabId.equals(Constants.TAB_HOME)){
                    // clear selected library\
                    pushFragments(tabId, new HomeFragmentTab(), false,true);
                }else if(tabId.equals(Constants.TAB_RECORDED_PROGRAM)){
                    RecordedProgramFragmentTab programFragmentTab = new RecordedProgramFragmentTab();
                    programFragmentTab.setSelectedLibrary(selectedLibrary);
                    pushFragments(tabId, programFragmentTab, false, true);
                }else if(tabId.equals(Constants.TAB_LIBRARY)){

                    pushFragments(tabId, new LibraryFragmentTab(), false,true);
                }else if(tabId.equals(Constants.TAB_SETTING)){

                    pushFragments(tabId, new SettingFragmentTab(), false,true);
                }else if(tabId.equals(Constants.TAB_PLAY_SCREEN)){

                    pushFragments(tabId, new PlayerFragmentTab(), false,true);
                }
            }else {

              /*
               *    We are switching tabs, and target tab is already has atleast one fragment.
               *    No need of animation, no need of stack pushing. Just show the target fragment
               */
                if (tabId.equals(Constants.TAB_RECORDED_PROGRAM) && selectedLibrary != null) {
                    RecordedProgramFragmentTab programFragmentTab = (RecordedProgramFragmentTab) mStacks.get(tabId).lastElement();
                    programFragmentTab.setSelectedLibrary(selectedLibrary);
                   // programFragmentTab.loadData();
                    pushFragments(tabId, programFragmentTab, false, false);
                } else {
                    pushFragments(tabId, mStacks.get(tabId).lastElement(), false, false);
                }
            }
            setSelectedTabColor();
        }
    };


    /* Might be useful if we want to switch tab programmatically, from inside any of the fragment.*/
    public void setCurrentTab(int val){
        mTabHost.setCurrentTab(val);
    }


    /*
     *      To add fragment to a tab.
     *  tag             ->  Tab identifier
     *  fragment        ->  Fragment to show, in tab identified by tag
     *  shouldAnimate   ->  should animate transaction. false when we switch tabs, or adding first fragment to a tab
     *                      true when when we are pushing more fragment into navigation stack.
     *  shouldAdd       ->  Should add to fragment navigation stack (mStacks.get(tag)). false when we are switching tabs (except for the first time)
     *                      true in all other cases.
     */
    public void pushFragments(String tag, Fragment fragment,boolean shouldAnimate, boolean shouldAdd){
        try {
            if (shouldAdd)
                mStacks.get(tag).push(fragment);
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction ft = manager.beginTransaction();
            if (shouldAnimate)
                ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
            ft.replace(R.id.realtabcontent, fragment);
            ft.commitAllowingStateLoss();
        } catch (Exception ex) {
            SimpleAppLog.error("Could not push fragment " + tag,ex);
        }
    }

    /*
         * Update Tab highlight color
         */
    private void setSelectedTabColor() {
        for(int i=0;i<mTabHost.getTabWidget().getChildCount();i++)
        {
            mTabHost.getTabWidget().getChildAt(i).findViewById(R.id.txtTabName)
                    .setBackgroundColor(getResources().getColor(R.color.default_button_color));
        }
        mTabHost.getTabWidget().getChildAt(mTabHost.getCurrentTab()).findViewById(R.id.txtTabName)
                .setBackgroundColor(getResources().getColor(R.color.default_button_highlight_color));
    }


    public void popFragments(){
      /*
       *    Select the second last fragment in current tab's stack..
       *    which will be shown after the fragment transaction given below
       */
        Fragment fragment             =   mStacks.get(mCurrentTab).elementAt(mStacks.get(mCurrentTab).size() - 2);

      /*pop current fragment from stack.. */
        mStacks.get(mCurrentTab).pop();

      /* We have the target fragment in hand.. Just show it.. Show a standard navigation animation*/
        FragmentManager   manager         =   getSupportFragmentManager();
        FragmentTransaction ft            =   manager.beginTransaction();
        ft.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
        ft.replace(R.id.realtabcontent, fragment);
        ft.commit();
    }


    @Override
    public void onBackPressed() {
        if(mStacks.get(mCurrentTab).size() == 1){
            // We are already showing first fragment of current tab, so when back pressed, we will finish this activity..
            finish();
            return;
        }

        //((FragmentTab)mStacks.get(mCurrentTab).lastElement()).onBackPressed();

        /* Goto previous fragment in navigation stack of this tab */
        popFragments();
    }


    /*
     *   Imagine if you wanted to get an image selected using ImagePicker intent to the fragment. Ofcourse I could have created a public function
     *  in that fragment, and called it from the activity. But couldn't resist myself.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(mStacks.get(mCurrentTab).size() == 0){
            return;
        }
        /*Now current fragment on screen gets onActivityResult callback..*/
        mStacks.get(mCurrentTab).lastElement().onActivityResult(requestCode, resultCode, data);
    }

    private Runnable startStreamingRunnable = new Runnable() {

        @Override
        public void run() {
            if (selectedObj == null || selectedObj.length() ==0) return;
            boolean isNew = true;
            try {
                if (mService.isPlaying()) {
                    if (mService.isStreaming()) {
                        Gson gson = new Gson();
                        String obj = mService.getChannelObject();
                        Channel selectedChannel = gson.fromJson(selectedObj, Channel.class);
                        Channel currentChannel = gson.fromJson(obj, Channel.class);
                        if (currentChannel.getUrl().equalsIgnoreCase(selectedChannel.getUrl())) {
                            isNew = false;
                        }
                    }
                }
                if (isNew) {
                    if (mService.isStreaming() && mService.isRecording()) {
                        mService.stopRecord();
                    }
                    mService.stop();
                }
            } catch (RemoteException e) {
                SimpleAppLog.error("Could not stop old stream",e);
            }
            if (isNew) {
                try {
                    mService.setStreaming(true);
                    mService.openStream("", selectedObj);
                } catch (RemoteException e) {
                    SimpleAppLog.error("Could not open stream", e);
                }
            }
        }

    };

    private String selectedObj;

    private Handler startStreamingHandler = new Handler();
    private final BroadcastReceiver mHandleAction = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            int type = bundle.getInt(Constants.FRAGMENT_ACTION_TYPE);
            switch (type) {
                case Constants.ACTION_CLICK_BACK_PLAYER:
                    if(mStacks.get(mCurrentTab).size() > 1) {
                        popFragments();
                    } else {
                        pushFragments(Constants.TAB_HOME, new HomeFragmentTab(), false, false);
                    }
                    break;
                case Constants.ACTION_SELECT_CHANNEL_ITEM:
                    selectedObj = bundle.getString(Constants.ARG_OBJECT);
                    startStreamingHandler.removeCallbacks(startStreamingRunnable);
                    startStreamingHandler.post(startStreamingRunnable);
                    pushFragments(Constants.TAB_PLAY_SCREEN, new PlayerFragmentTab(), true,false);
                    break;
                case Constants.ACTION_SELECT_RECORDED_PROGRAM_ITEM:
                    pushFragments(Constants.TAB_PLAY_SCREEN, new PlayerFragmentTab(), true,false);
                    break;
                case Constants.ACTION_CALL_SELECT_TAB:
                    setCurrentTab(bundle.getInt(Constants.PARAMETER_SELECTED_TAB_ID));
                    break;
                case Constants.ACTION_SELECT_LIBRARY_ITEM:
                    Gson gson = new Gson();
                    selectedLibrary = gson.fromJson(bundle.getString(Constants.ARG_OBJECT), Library.class);
                    setCurrentTab(Constants.TAB_RECORDED_PROGRAM_ID);
                    break;
                case Constants.ACTION_RESET_FILTER_RECORDED_PROGRAM:
                    selectedLibrary = null;
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(mHandleAction);
        } catch ( Exception ex) {

        }
        MusicUtils.unbindFromService(mServiceToken);
        mService = null;
        super.onDestroy();
    }

    private void checkPlaying() {
        try {
            if (mService != null && mService.isPlaying()) {
                if (mService.isStreaming()) {
                    setCurrentTab(Constants.TAB_HOME_ID);
                } else {
                    setCurrentTab(Constants.TAB_RECORDED_PROGRAM_ID);
                }
                pushFragments(Constants.TAB_PLAY_SCREEN, new PlayerFragmentTab(), false, false);
            }
        } catch (RemoteException e) {
            SimpleAppLog.error("Could not get player status",e);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = IMediaPlaybackService.Stub.asInterface(service);
        checkPlaying();
//        Intent intent = getIntent();
//        if (intent == null) return;
//        String type =intent.getType();
//        if (type != null && type.equalsIgnoreCase(Constants.PLAYBACK_VIEWER_INTENT)) {
//
//        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mService = null;
    }

}
