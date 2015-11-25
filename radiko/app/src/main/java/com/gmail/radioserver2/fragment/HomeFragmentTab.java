package com.gmail.radioserver2.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.gmail.radioserver2.R;
import com.gmail.radioserver2.utils.AppDelegate;
import com.gmail.radioserver2.view.slidingtab.SlidingTabLayout;
import com.gmail.radioserver2.view.slidingtab.TabPagerTracking;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.HashMap;

/**
 * Created by luhonghai on 2/18/15.
 */

public class HomeFragmentTab extends FragmentTab implements View.OnClickListener, TabPagerTracking.OnTabChange {
    private EditText txtSearch;
    private Button btnSearch;
    private View btCancelSearch;
    private FragmentManager mFragmentManager;
    private AdView mAdView;

    private HashMap<String, ChannelFragmentTab> channelStack;
    private int mTabPos;
    private SlidingTabLayout mSlidingTab;
    private View labelSelectChannel;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        IntentFilter filter = new IntentFilter();
        filter.addAction("sadhkahd.recreate_ui");
        getActivity().registerReceiver(reCreateUI, filter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (savedInstanceState != null) {
            mTabPos = savedInstanceState.getInt("tabPos");
        } else {
            mTabPos = 0;
        }
        View v = inflater.inflate(R.layout.fragment_home_tab, container, false);
        mAdView = (AdView) v.findViewById(R.id.adView);
        if (mAdView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }
        txtSearch = (EditText) v.findViewById(R.id.txtSearch);
        txtSearch.addTextChangedListener(textSearchChanged);
        btnSearch = (Button) v.findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(this);
        btCancelSearch = v.findViewById(R.id.btCancelSearch);
        btCancelSearch.setOnClickListener(this);
        mSlidingTab = (SlidingTabLayout) v.findViewById(R.id.channelTab);
        labelSelectChannel = v.findViewById(R.id.labelSelectChannel);
        setupUI();
        return v;
    }

    private void setupUI() {
        TabPagerTracking pagerTracking = new TabPagerTracking();
        pagerTracking.addTab(new TabPagerTracking.Tab("全て", "all"));
        if (AppDelegate.getInstance().isPremium()) {
            labelSelectChannel.setVisibility(View.GONE);
            mSlidingTab.setVisibility(View.VISIBLE);
            pagerTracking.addTab(new TabPagerTracking.Tab("全国", "a0"));
            pagerTracking.addTab(new TabPagerTracking.Tab("北海道・東北", "a1"));
            pagerTracking.addTab(new TabPagerTracking.Tab("関東", "a2"));
            pagerTracking.addTab(new TabPagerTracking.Tab("北陸・甲信越", "a3"));
            pagerTracking.addTab(new TabPagerTracking.Tab("中部", "a4"));
            pagerTracking.addTab(new TabPagerTracking.Tab("近畿", "a5"));
            pagerTracking.addTab(new TabPagerTracking.Tab("中国・四国", "a6"));
            pagerTracking.addTab(new TabPagerTracking.Tab("九州・沖縄", "a7"));
        } else {
            labelSelectChannel.setVisibility(View.VISIBLE);
            mSlidingTab.setVisibility(View.GONE);
        }
        channelStack = new HashMap<>();
        mFragmentManager = getChildFragmentManager();
        mSlidingTab.setAdapter(pagerTracking);
        pagerTracking.setTabChange(this);
        mSlidingTab.selectTab(mTabPos);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tabPos", mTabPos);
    }

    private ChannelFragmentTab getFragment(String tabName) {
        ChannelFragmentTab frg = channelStack.get(tabName);
        if (frg == null) {
            frg = new ChannelFragmentTab();
            frg.setRadikoRegion(tabName);
            channelStack.put(tabName, frg);
        }
        return frg;
    }

    private void pushFragment(Fragment fragment) {
        mFragmentManager.beginTransaction().replace(R.id.channelContent, fragment).commit();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAdView != null) mAdView.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) mAdView.resume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        btnSearch = null;
        txtSearch = null;
        if (mAdView != null) {
            mAdView.destroy();
            mAdView = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            getActivity().unregisterReceiver(reCreateUI);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSearch:
                SearchFragmentTab searchFragmentTab = new SearchFragmentTab();
                searchFragmentTab.setSearchStr(txtSearch.getText().toString());
                searchFragmentTab.setOnSelectedChannelInSearch(onSelectedChannelInSearch);
                pushFragment(searchFragmentTab);
                break;
            case R.id.btCancelSearch:
                if (txtSearch != null) {
                    txtSearch.setText("");
                }
                if (mSlidingTab != null) {
                    mSlidingTab.selectTab(mTabPos);
                }
                break;
        }
    }

    private SearchFragmentTab.OnSelectedChannelInSearch onSelectedChannelInSearch = new SearchFragmentTab.OnSelectedChannelInSearch() {
        @Override
        public void onSelectedChannel() {
            if (mSlidingTab != null) {
                mSlidingTab.selectTab(mTabPos);
            }
        }
    };

    @Override
    public void selectedTab(int pos, TabPagerTracking.Tab tab) {
        pushFragment(getFragment(tab.getName()));
        mTabPos = pos;
    }

    private TextWatcher textSearchChanged = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.length() == 0) {
                if (btCancelSearch != null && btCancelSearch.isShown()) {
                    btCancelSearch.setVisibility(View.GONE);
                }
            } else {
                if (btCancelSearch != null && !btCancelSearch.isShown()) {
                    btCancelSearch.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    private BroadcastReceiver reCreateUI = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setupUI();
        }
    };
}
