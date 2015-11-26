package com.gmail.radioserver2.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.gmail.radioserver2.R;
import com.gmail.radioserver2.adapter.LibraryAdapter;
import com.gmail.radioserver2.adapter.OnListItemActionListener;
import com.gmail.radioserver2.data.Library;
import com.gmail.radioserver2.data.RecordedProgram;
import com.gmail.radioserver2.data.sqlite.ext.LibraryDBAdapter;
import com.gmail.radioserver2.data.sqlite.ext.RecordedProgramDBAdapter;
import com.gmail.radioserver2.utils.AndroidUtil;
import com.gmail.radioserver2.utils.Constants;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.gmail.radioserver2.view.swipelistview.BaseSwipeListViewListener;
import com.gmail.radioserver2.view.swipelistview.SwipeListView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.gson.Gson;

import java.sql.SQLException;
import java.util.Collection;

/**
 * Created by luhonghai on 2/17/15.
 */
public class LibraryFragmentTab extends FragmentTab implements OnListItemActionListener<Library>, View.OnClickListener {
    private SwipeListView listView;

    private EditText txtSearch;
    private Button btnSearch;

    private int openItem = -1;
    private int lastOpenedItem = -1;
    private int lastClosedItem = -1;

    private AdView mAdView;
    private View btShare;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_library_tab, container, false);

        mAdView = (AdView) v.findViewById(R.id.adView);
        if (mAdView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }

        listView = (SwipeListView) v.findViewById(R.id.list_library);

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

        txtSearch = (EditText) v.findViewById(R.id.txtSearch);
        btnSearch = (Button) v.findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(this);
        btShare = v.findViewById(R.id.btShare);
        btShare.setOnClickListener(this);
        loadData();
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

    private void loadData() {
        LibraryDBAdapter dbAdapter = new LibraryDBAdapter(getActivity());
        try {
            dbAdapter.open();
            Collection<Library> libraries = dbAdapter.search(txtSearch.getText().toString());
            LibraryAdapter adapter = new LibraryAdapter(getActivity());
            listView.setAdapter(adapter);
            adapter.setOnListItemActionListener(this);
            adapter.setDataList(libraries);
        } catch (Exception e) {
            SimpleAppLog.error("Could not load library", e);
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
        if (mAdView != null) {
            mAdView.destroy();
            mAdView = null;
        }
    }

    @Override
    public void onDeleteItem(Library obj) {
        if (openItem > -1 && lastOpenedItem != lastClosedItem) {
            listView.closeItem(openItem);
        }
        RecordedProgramDBAdapter recordedProgramDBAdapter = new RecordedProgramDBAdapter(getActivity());
        try {
            recordedProgramDBAdapter.open();
            Collection<RecordedProgram> recordedPrograms = recordedProgramDBAdapter.findByLibrary(obj, "");
            for (RecordedProgram recordedProgram : recordedPrograms) {
                recordedProgramDBAdapter.deleteAllMapping(recordedProgram);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            recordedProgramDBAdapter.close();
        }
        LibraryDBAdapter adapter = new LibraryDBAdapter(getActivity());
        try {
            adapter.open();
            adapter.delete(obj);
        } catch (Exception ex) {
            SimpleAppLog.error("Could not delete library", ex);
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
    public void onSelectItem(Library obj) {
        Intent intent = new Intent(Constants.INTENT_FILTER_FRAGMENT_ACTION);
        intent.putExtra(Constants.FRAGMENT_ACTION_TYPE, Constants.ACTION_SELECT_LIBRARY_ITEM);
        Gson gson = new Gson();
        intent.putExtra(Constants.ARG_OBJECT, gson.toJson(obj));
        getActivity().sendBroadcast(intent);
    }

    @Override
    public void onEditItem(Library obj) {

    }

    @Override
    public void onSelectItems(SparseArray<Library> items) {
        if (items.size() > 0) {
            if (!btShare.isShown()) {
                btShare.setVisibility(View.VISIBLE);
            }
        } else {
            if (btShare.isShown()) {
                btShare.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSearch:
                loadData();
                break;
            case R.id.btShare:
                shareProgram();
                break;
        }
    }

    private void shareProgram() {
        LibraryAdapter libraryAdapter = (LibraryAdapter) listView.getAdapter();
        SparseArray<Library> librarySparseArray = libraryAdapter.getSelected();
        if (librarySparseArray.size() > 0) {
            SparseArray<RecordedProgram> recordedProgramSparseArray = new SparseArray<>();
            int size = librarySparseArray.size();
            RecordedProgramDBAdapter recordedProgramDBAdapter = new RecordedProgramDBAdapter(getActivity());
            try {
                recordedProgramDBAdapter.open();
                for (int i = 0; i < size; i++) {
                    Collection<RecordedProgram> recordedPrograms = recordedProgramDBAdapter.findByLibrary(librarySparseArray.valueAt(i), "");
                    for (RecordedProgram recordedProgram : recordedPrograms) {
                        recordedProgramSparseArray.append((int) recordedProgram.getUniqueID(), recordedProgram);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (recordedProgramSparseArray.size() > 0) {
                AndroidUtil.shareProgram(getContext(), recordedProgramSparseArray);
            }
        }
    }
}
