package com.gmail.radioserver2.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gmail.radioserver2.R;
import com.gmail.radioserver2.adapter.ChannelAdapter;
import com.gmail.radioserver2.adapter.OnListItemActionListener;
import com.gmail.radioserver2.data.Channel;
import com.gmail.radioserver2.data.sqlite.ext.ChannelDBAdapter;
import com.gmail.radioserver2.utils.Constants;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.gmail.radioserver2.view.swipelistview.BaseSwipeListViewListener;
import com.gmail.radioserver2.view.swipelistview.SwipeListView;
import com.google.gson.Gson;

import java.util.Collection;

public class ChannelFragmentTab extends FragmentTab implements OnListItemActionListener<Channel> {
    protected SwipeListView listView;

    private String radikoRegion = "all";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().registerReceiver(mHandleAction, new IntentFilter(Constants.INTENT_FILTER_FRAGMENT_ACTION));
    }

    @Override
    public void onDestroy() {
        try {
            getActivity().unregisterReceiver(mHandleAction);
        } catch (Exception ex) {
            //
        }
        super.onDestroy();
    }

    private int openItem = -1;
    private int lastOpenedItem = -1;
    private int lastClosedItem = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_channel_tab, container, false);
        listView = (SwipeListView) v.findViewById(R.id.list_channel);

        openItem = -1;
        lastOpenedItem = -1;
        lastClosedItem = -1;

        listView.setSwipeListViewListener(new BaseSwipeListViewListener() {
            @Override
            public void onOpened(int position, boolean toRight) {
                lastOpenedItem = position;
                if (openItem > -1 && lastOpenedItem != lastClosedItem) {
                    listView.closeAnimate(openItem);
                }
                openItem = position;
            }

            @Override
            public void onStartClose(int position, boolean right) {
                lastClosedItem = position;
            }
        });
        loadData();
        return v;
    }

    public ChannelFragmentTab setRadikoRegion(String radikoRegion) {
        this.radikoRegion = radikoRegion;
        return this;
    }

    protected void loadData() {
        if (listView == null) return;
        ChannelDBAdapter dbAdapter = new ChannelDBAdapter(getActivity());
        try {
            dbAdapter.open();
            Collection<Channel> channels = dbAdapter.loadChannelByRadikoRegion(radikoRegion);
            SimpleAppLog.info("Load " + (channels == null ? 0 : channels.size()) + " channels to listview");
            ChannelAdapter adapter = new ChannelAdapter(getActivity());
            listView.setAdapter(adapter);
            adapter.setDataList(channels);
            adapter.setOnListItemActionListener(this);
            listView.dismissSelected();
        } catch (Exception e) {
            SimpleAppLog.error("Could not load channel", e);
        } finally {
            dbAdapter.close();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listView = null;
    }

    @Override
    public void onDeleteItem(Channel obj) {
        if (openItem > -1 && lastOpenedItem != lastClosedItem) {
            listView.closeItem(openItem);
        }
        ChannelDBAdapter adapter = new ChannelDBAdapter(getActivity());
        try {
            adapter.open();
            adapter.delete(obj);
        } catch (Exception ex) {
            SimpleAppLog.error("Could not delete channel", ex);
        } finally {
            adapter.close();
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                loadData();
            }
        }, 100);
    }

    @Override
    public void onSelectItem(Channel obj) {
        SimpleAppLog.info("Select item: " + obj.getName());
        Intent intent = new Intent(Constants.INTENT_FILTER_FRAGMENT_ACTION);
        intent.putExtra(Constants.FRAGMENT_ACTION_TYPE, Constants.ACTION_SELECT_CHANNEL_ITEM);
        Gson gson = new Gson();
        intent.putExtra(Constants.ARG_OBJECT, gson.toJson(obj));
        getActivity().sendBroadcast(intent);
    }

    @Override
    public void onEditItem(Channel obj) {

    }

    @Override
    public void onSelectItems(SparseArray<Channel> items) {
        //passed
    }

    private final BroadcastReceiver mHandleAction = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            int type = bundle.getInt(Constants.FRAGMENT_ACTION_TYPE);
            switch (type) {
                case Constants.ACTION_RELOAD_LIST:
                    loadData();
                    break;
            }
        }
    };
}
