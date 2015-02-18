package com.gmail.radioserver2.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import com.fortysevendeg.swipelistview.SwipeListView;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.adapter.ChannelAdapter;

/**
 * Created by luhonghai on 2/18/15.
 */
public class HomeFragmentTab extends FragmentTab {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home_tab, container, false);
        SwipeListView listView = (SwipeListView) v.findViewById(R.id.list_channel);
        int count = 20;
        String[] channels = new String[count];
        for (int i = 0; i < count - 1; i++) {
            channels[i] = "TBS Radio " + (i + 1);
        }
        ChannelAdapter adapter = new ChannelAdapter(getActivity(), channels);
        listView.setAdapter(adapter);
        return v;
    }
}
