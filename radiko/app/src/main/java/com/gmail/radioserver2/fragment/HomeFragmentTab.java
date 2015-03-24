package com.gmail.radioserver2.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.dotohsoft.radio.Constant;
import com.gmail.radioserver2.adapter.OnListItemActionListener;
import com.gmail.radioserver2.data.Channel;
import com.gmail.radioserver2.data.Library;
import com.gmail.radioserver2.data.sqlite.ext.ChannelDBAdapter;
import com.gmail.radioserver2.service.TimerBroadcastReceiver;
import com.gmail.radioserver2.utils.Constants;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.gmail.radioserver2.view.swipelistview.SwipeListView;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.adapter.ChannelAdapter;
import com.google.gson.Gson;

import java.util.Collection;

/**
 * Created by luhonghai on 2/18/15.
 */

public class HomeFragmentTab extends FragmentTab implements OnListItemActionListener<Channel>, View.OnClickListener {
    private SwipeListView listView;

    private EditText txtSearch;
    private Button btnSearch;

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

        }
        super.onDestroy();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home_tab, container, false);
        listView = (SwipeListView) v.findViewById(R.id.list_channel);
        txtSearch = (EditText) v.findViewById(R.id.txtSearch);
        btnSearch = (Button) v.findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(this);
        loadData();
        return v;
    }
    private void loadData() {
        if (listView == null || txtSearch == null) return;
        ChannelDBAdapter dbAdapter = new ChannelDBAdapter(getActivity());
        try {
            dbAdapter.open();
            Collection<Channel> channels = dbAdapter.search(txtSearch.getText().toString());
            Channel[] items;
            if (channels != null && channels.size() > 0) {
                items = new Channel[channels.size()];
                channels.toArray(items);
            } else {
                items = new Channel[] {};
            }
            ChannelAdapter adapter = new ChannelAdapter(getActivity(), items, this);
            listView.setAdapter(adapter);
            listView.dismissSelected();
            adapter.notifyDataSetChanged();
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
        btnSearch = null;
        txtSearch = null;
    }

    @Override
    public void onDeleteItem(Channel obj) {
        ChannelDBAdapter adapter = new ChannelDBAdapter(getActivity());
        try {
            adapter.open();
            adapter.delete(obj);
            loadData();
        } catch (Exception ex) {
            SimpleAppLog.error("Could not delete channel", ex);
        } finally {
            adapter.close();
        }
    }

    @Override
    public void onSelectItem(Channel obj) {
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
    public void onSelectIndex(int index) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSearch:
                loadData();
                break;
        }
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
