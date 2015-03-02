package com.gmail.radioserver2.fragment;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.gmail.radioserver2.activity.LibraryPickerActivity;
import com.gmail.radioserver2.adapter.OnListItemActionListener;
import com.gmail.radioserver2.data.Library;
import com.gmail.radioserver2.data.RecordedProgram;
import com.gmail.radioserver2.data.sqlite.ext.LibraryDBAdapter;
import com.gmail.radioserver2.data.sqlite.ext.RecordedProgramDBAdapter;
import com.gmail.radioserver2.service.IMediaPlaybackService;
import com.gmail.radioserver2.service.MediaPlaybackService;
import com.gmail.radioserver2.service.MusicUtils;
import com.gmail.radioserver2.utils.Constants;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.gmail.radioserver2.view.swipelistview.SwipeListView;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.adapter.RecordedProgramAdapter;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Collection;

/**
 * Created by luhonghai on 2/17/15.
 */
public class RecordedProgramFragmentTab extends FragmentTab implements OnListItemActionListener<RecordedProgram>, View.OnClickListener,
        ServiceConnection {
    private SwipeListView listView;

    private EditText txtSearch;
    private Button btnSearch;

    private Library selectedLibrary;

    private IMediaPlaybackService mService = null;

    private MusicUtils.ServiceToken mServiceToken;

    private RecordedProgram[] objects;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_recorded_program_tab, container, false);
        listView = (SwipeListView) v.findViewById(R.id.list_recorded_programs);
        txtSearch = (EditText) v.findViewById(R.id.txtSearch);
        btnSearch = (Button) v.findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(this);
        loadData();
        return v;
    }

    public void loadData() {
        RecordedProgramDBAdapter dbAdapter = new RecordedProgramDBAdapter(getActivity());
        try {
            dbAdapter.open();
            Collection<RecordedProgram> programs;
            String s = txtSearch.getText().toString();
            if (selectedLibrary != null) {
                programs = dbAdapter.findByLibrary(selectedLibrary, s);
            } else {
                programs = dbAdapter.search(s);
            }


            if (programs != null && programs.size() > 0) {
                objects = new RecordedProgram[programs.size()];
                programs.toArray(objects);
            } else {
                objects = new RecordedProgram[] {};
            }
            RecordedProgramAdapter adapter = new RecordedProgramAdapter(getActivity(), objects, this);
            listView.setAdapter(adapter);
            listView.dismissSelected();
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            SimpleAppLog.error("Could not load recorded program", e);
        } finally {
            dbAdapter.close();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listView = null;
        txtSearch = null;
        btnSearch = null;
        selectedLibrary = null;
        objects = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mServiceToken = MusicUtils.bindToService(getActivity(), this);
    }

    @Override
    public void onDestroy() {
        MusicUtils.unbindFromService(mServiceToken);
        mService = null;
        super.onDestroy();
    }

    @Override
    public void onDeleteItem(RecordedProgram obj) {
        RecordedProgramDBAdapter adapter = new RecordedProgramDBAdapter(getActivity());
        try {
            try {
                FileUtils.forceDelete(new File(obj.getFilePath()));
            } catch (Exception ex) {
                SimpleAppLog.error("Could not delete recorded file " + obj.getFilePath(), ex);
            }
            adapter.open();
            adapter.delete(obj);
            loadData();
        } catch (Exception ex) {
            SimpleAppLog.error("Could not delete recorded program",ex);
        } finally {
            adapter.close();
        }
    }

    @Override
    public void onSelectItem(RecordedProgram obj) {
        if (mService != null) {
            try {
                if (mService.isRecording())
                    mService.stopRecord();
            } catch (RemoteException e) {
                SimpleAppLog.error("Could not stop recording",e);
            }
            try {
                if (mService.isPlaying())
                    mService.stop();
            } catch (RemoteException e) {
                SimpleAppLog.error("Could not stop playing",e);
            }
            if (objects != null && objects.length > 0) {
                int selectedIndex = 0;
                long[] playlist = new long[objects.length];
                for (int i = 0; i < objects.length; i++) {
                    RecordedProgram rp = objects[i];
                    playlist[i] = MusicUtils.insert(getActivity(), rp.getFilePath());
                    if (obj.getId() == rp.getId()) {
                        selectedIndex = i;
                    }
                }
                try {
                    mService.setRepeatMode(MediaPlaybackService.REPEAT_ALL);
                    mService.open(playlist, selectedIndex);

                    Intent intent = new Intent(Constants.INTENT_FILTER_FRAGMENT_ACTION);
                    intent.putExtra(Constants.FRAGMENT_ACTION_TYPE, Constants.ACTION_SELECT_RECORDED_PROGRAM_ITEM);
                    getActivity().sendBroadcast(intent);
                } catch (RemoteException e) {
                    SimpleAppLog.error("Could not play list recorded program", e);
                }
            }

        }
    }

    @Override
    public void onEditItem(RecordedProgram obj) {
        Intent intent = new Intent();
        intent.setClass(getActivity(), LibraryPickerActivity.class);
        Gson gson = new Gson();
        intent.putExtra(Constants.ARG_OBJECT, gson.toJson(obj));
        getActivity().startActivity(intent);
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

    public Library getSelectedLibrary() {
        return selectedLibrary;
    }

    public void setSelectedLibrary(Library selectedLibrary) {
        this.selectedLibrary = selectedLibrary;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = IMediaPlaybackService.Stub.asInterface(service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}
