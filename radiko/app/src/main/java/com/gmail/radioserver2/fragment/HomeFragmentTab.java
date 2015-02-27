package com.gmail.radioserver2.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gmail.radioserver2.adapter.OnListItemActionListener;
import com.gmail.radioserver2.data.Channel;
import com.gmail.radioserver2.data.sqlite.ext.ChannelDBAdapter;
import com.gmail.radioserver2.view.swipelistview.SwipeListView;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.adapter.ChannelAdapter;

import java.util.Collection;

/**
 * Created by luhonghai on 2/18/15.
 */
public class HomeFragmentTab extends FragmentTab implements OnListItemActionListener<Channel> {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home_tab, container, false);
        SwipeListView listView = (SwipeListView) v.findViewById(R.id.list_channel);
        ChannelDBAdapter dbAdapter = new ChannelDBAdapter(getActivity());
        try {
            Collection<Channel> channels = dbAdapter.findAll();
            if (channels != null && channels.size() > 0) {
                Channel[] items = new Channel[channels.size()];
                channels.toArray(items);
                ChannelAdapter adapter = new ChannelAdapter(getActivity(), items, this);
                listView.setAdapter(adapter);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return v;
    }

    @Override
    public void onDeleteItem(Channel obj) {

    }

    @Override
    public void onSelectItem(Channel obj) {

    }

    @Override
    public void onEditItem(Channel obj) {

    }
}
