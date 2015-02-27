package com.gmail.radioserver2.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gmail.radioserver2.adapter.OnListItemActionListener;
import com.gmail.radioserver2.data.RecordedProgram;
import com.gmail.radioserver2.data.sqlite.ext.RecoredProgramDBAdapter;
import com.gmail.radioserver2.view.swipelistview.SwipeListView;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.adapter.RecordedProgramAdapter;

import java.util.Collection;

/**
 * Created by luhonghai on 2/17/15.
 */
public class RecordedProgramFragmentTab extends FragmentTab implements OnListItemActionListener<RecordedProgram> {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_recorded_program_tab, container, false);
        SwipeListView listView = (SwipeListView) v.findViewById(R.id.list_recorded_programs);
        RecoredProgramDBAdapter dbAdapter = new RecoredProgramDBAdapter(getActivity());
        try {
            Collection<RecordedProgram> programs = dbAdapter.findAll();
            if (programs != null && programs.size() > 0) {
                RecordedProgram[] objects = new RecordedProgram[programs.size()];
                programs.toArray(objects);
                RecordedProgramAdapter adapter = new RecordedProgramAdapter(getActivity(), objects, this);
                listView.setAdapter(adapter);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return v;
    }

    @Override
    public void onDeleteItem(RecordedProgram obj) {

    }

    @Override
    public void onSelectItem(RecordedProgram obj) {

    }

    @Override
    public void onEditItem(RecordedProgram obj) {

    }
}
