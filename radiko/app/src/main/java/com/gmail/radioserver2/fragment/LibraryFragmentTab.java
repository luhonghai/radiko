package com.gmail.radioserver2.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.gmail.radioserver2.adapter.OnListItemActionListener;
import com.gmail.radioserver2.data.Library;
import com.gmail.radioserver2.data.sqlite.ext.LibraryDBAdapter;
import com.gmail.radioserver2.utils.Constants;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.gmail.radioserver2.view.swipelistview.SwipeListView;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.adapter.LibraryAdapter;
import com.google.gson.Gson;

import java.util.Collection;

/**
 * Created by luhonghai on 2/17/15.
 */
public class LibraryFragmentTab extends FragmentTab implements OnListItemActionListener<Library>, View.OnClickListener {
    private SwipeListView listView;

    private EditText txtSearch;
    private Button btnSearch;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_library_tab, container, false);
        listView = (SwipeListView) v.findViewById(R.id.list_library);
        txtSearch = (EditText) v.findViewById(R.id.txtSearch);
        btnSearch = (Button) v.findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(this);
        loadData();
        return v;

    }

    private void loadData() {
        LibraryDBAdapter dbAdapter = new LibraryDBAdapter(getActivity());
        try {
            dbAdapter.open();
            Collection<Library> libraries = dbAdapter.search(txtSearch.getText().toString());
            Library[] items;
            if (libraries != null && libraries.size() > 0) {
                items = new Library[libraries.size()];
                libraries.toArray(items);
            } else {
                items = new Library[] {};
            }
            LibraryAdapter adapter = new LibraryAdapter(getActivity(), items, this);
            listView.setAdapter(adapter);
            listView.dismissSelected();
            adapter.notifyDataSetChanged();
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
    }

    @Override
    public void onDeleteItem(Library obj) {
        LibraryDBAdapter adapter = new LibraryDBAdapter(getActivity());
        try {
            adapter.open();
            adapter.delete(obj);
            loadData();
        } catch (Exception ex) {
            SimpleAppLog.error("Could not delete library",ex);
        } finally {
            adapter.close();
        }
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
}
