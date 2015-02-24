package com.gmail.radioserver2.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fortysevendeg.swipelistview.SwipeListView;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.adapter.ChannelAdapter;
import com.gmail.radioserver2.adapter.RecordedProgramAdapter;

/**
 * Created by luhonghai on 2/17/15.
 */
public class RecordedProgramFragmentTab extends FragmentTab {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_recorded_program_tab, container, false);
        SwipeListView listView = (SwipeListView) v.findViewById(R.id.list_recorded_programs);
        int count = 15;
        String[] items = new String[count];
        for (int i = 0; i < count; i++) {
            items[i] = getResources().getString(R.string.debug_recorded_program_name,(i + 1));
        }
        RecordedProgramAdapter adapter = new RecordedProgramAdapter(getActivity(), items);
        listView.setAdapter(adapter);
        return v;
    }
}
