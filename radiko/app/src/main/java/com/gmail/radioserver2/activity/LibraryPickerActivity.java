package com.gmail.radioserver2.activity;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.gmail.radioserver2.adapter.OnListItemActionListener;
import com.gmail.radioserver2.data.Library;
import com.gmail.radioserver2.data.RecordedProgram;
import com.gmail.radioserver2.data.sqlite.ext.LibraryDBAdapter;
import com.gmail.radioserver2.data.sqlite.ext.RecordedProgramDBAdapter;
import com.gmail.radioserver2.utils.Constants;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.gmail.radioserver2.view.swipelistview.BaseSwipeListViewListener;
import com.gmail.radioserver2.view.swipelistview.SwipeListView;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.adapter.LibraryPickerAdapter;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by luhonghai on 2/22/15.
 */
public class LibraryPickerActivity extends BaseActivity implements View.OnClickListener, OnListItemActionListener<Library> {

    private SwipeListView listView;

    private Button btnSave;

    private EditText txtLibraryName;

    private Collection<Library> selectedLibrary;

    private RecordedProgram recordedProgram;

    private LibraryPickerAdapter adapter;

    private int openItem = -1;
    private int lastOpenedItem = -1;
    private int lastClosedItem = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library_picker_list);

        btnSave = (Button) findViewById(R.id.btnSave);
        btnSave.setOnClickListener(this);

        listView = (SwipeListView) findViewById(R.id.list_library);

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

        txtLibraryName = (EditText) findViewById(R.id.txtLibraryName);

        Gson gson = new Gson();
        recordedProgram = gson.fromJson(getIntent().getExtras().getString(Constants.ARG_OBJECT), RecordedProgram.class);
        loadData();
    }

    private void loadData() {
        LibraryDBAdapter dbAdapter = new LibraryDBAdapter(this);
        try {
            dbAdapter.open();
            selectedLibrary = dbAdapter.findByRecordedProgram(recordedProgram);
            if (selectedLibrary == null) {
                selectedLibrary = new ArrayList<Library>();
            }
            int selectedIndex = -1;
            Collection<Library> libraries = dbAdapter.findAll();
            Library[] items;
            List<Library> selectedItems = new ArrayList<Library>();
            if (libraries != null && libraries.size() > 0) {
                items = new Library[libraries.size()];
                libraries.toArray(items);
                for (int i = 0; i < items.length; i++) {
                    if (selectedLibrary != null && selectedLibrary.size() > 0) {
                        for (Library library : selectedLibrary) {
                            if (library.getId() == items[i].getId()) {
                                if (!selectedItems.contains(library)) {
                                    selectedItems.add(library);
                                }
                            }
                        }
                    }
                }
            } else {
                items = new Library[] {};
            }
            adapter = new LibraryPickerAdapter(this, items, selectedItems, this);
            listView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
            listView.dismissSelected();
            listView.setSelection(selectedIndex);
        } catch (Exception e) {
            SimpleAppLog.error("Could not load library", e);
        } finally {
            dbAdapter.close();
        }
    }

    private void save() {
        selectedLibrary = adapter.getSelectedItems();
        LibraryDBAdapter adapter = new LibraryDBAdapter(this);
        RecordedProgramDBAdapter recordedProgramDBAdapter = new RecordedProgramDBAdapter(this);
        String libName = txtLibraryName.getText().toString().trim();
        SimpleAppLog.info("Detect lib name " + libName);
        if (libName.length() > 0) {
            // Enter new lib to add
            Library library = new Library();
            library.setName(libName);
            try {
                adapter.open();
                SimpleAppLog.info("Insert to database");
                long id = adapter.insert(library);
                library = adapter.find(id);
            } catch (Exception e) {
                SimpleAppLog.error("Could not insert new library",e);
            } finally {
                adapter.close();
            }
            if (!selectedLibrary.contains(library)) {
                selectedLibrary.add(library);
            }
        }

        try {
            recordedProgramDBAdapter.open();
            recordedProgramDBAdapter.deleteAllMapping(recordedProgram);
            if (selectedLibrary != null && selectedLibrary.size() > 0) {
                for (Library library : selectedLibrary) {
                    SimpleAppLog.info(
                            "Add mapping to database. Recorded program ID: "
                                    + recordedProgram.getId()
                                    + " and Library ID: "
                                    + library.getId());
                    recordedProgramDBAdapter.addToLibrary(recordedProgram, library);
                }
            } else {
                SimpleAppLog.info("No library selected");
            }
        } catch (Exception e) {
            SimpleAppLog.error("Could not mapping recorded program to library", e);
        } finally {
            recordedProgramDBAdapter.close();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSave:
                save();
                onBackPressed();
                break;
        }
    }

    @Override
    public void onDeleteItem(Library obj) {
        if (openItem > -1 && lastOpenedItem != lastClosedItem) {
            listView.closeItem(openItem);
        }

        LibraryDBAdapter adapter = new LibraryDBAdapter(this);
        try {
            adapter.open();
            adapter.delete(obj);
        } catch (Exception ex) {
            SimpleAppLog.error("Could not delete library",ex);
        } finally {
            adapter.close();
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                loadData();
            }
        },100);
    }

    @Override
    public void onSelectItem(Library obj) {
        selectedLibrary = adapter.getSelectedItems();
    }

    @Override
    public void onEditItem(Library obj) {

    }

    @Override
    public void onSelectIndex(int index) {
        listView.setSelection(index);
    }
}
