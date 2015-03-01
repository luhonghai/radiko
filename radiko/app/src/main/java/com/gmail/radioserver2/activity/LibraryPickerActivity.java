package com.gmail.radioserver2.activity;

import android.os.Bundle;
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
import com.gmail.radioserver2.view.swipelistview.SwipeListView;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.adapter.LibraryPickerAdapter;
import com.google.gson.Gson;

import java.util.Collection;

/**
 * Created by luhonghai on 2/22/15.
 */
public class LibraryPickerActivity extends BaseActivity implements View.OnClickListener, OnListItemActionListener<Library> {

    private SwipeListView listView;

    private Button btnSave;

    private EditText txtLibraryName;

    private Library selectedLibrary;

    private RecordedProgram recordedProgram;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library_picker_list);

        btnSave = (Button) findViewById(R.id.btnSave);
        btnSave.setOnClickListener(this);

        listView = (SwipeListView) findViewById(R.id.list_library);

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

            int selectedIndex = 0;
            Collection<Library> libraries = dbAdapter.findAll();
            Library[] items;
            if (libraries != null && libraries.size() > 0) {
                items = new Library[libraries.size()];
                libraries.toArray(items);
                if (selectedLibrary != null) {
                    SimpleAppLog.info("Found mapping library ID " + selectedLibrary.getId());
                    for (int i = 0; i < items.length; i++) {
                        if (items[i].getId() == selectedLibrary.getId()) {
                            selectedIndex = i;
                            break;
                        }
                    }
                } else {
                    SimpleAppLog.info("No latest selected library");
                }
            } else {
                items = new Library[] {};
            }
            LibraryPickerAdapter adapter = new LibraryPickerAdapter(this, items, this);
            adapter.setSelectedIndex(selectedIndex);
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
        LibraryDBAdapter adapter = new LibraryDBAdapter(this);
        RecordedProgramDBAdapter recordedProgramDBAdapter = new RecordedProgramDBAdapter(this);
        String libName = txtLibraryName.getText().toString().trim();
        Library library;
        SimpleAppLog.info("Detect lib name " + libName);
        if (libName.length() > 0) {
            // Enter new lib to add
            library = new Library();
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
        } else {
            library = selectedLibrary;
        }
        if (library != null) {
            try {
                recordedProgramDBAdapter.open();
                SimpleAppLog.info("Add mapping to database. Recorded program ID: " + recordedProgram.getId() + " and Library ID: " + library.getId());
                recordedProgramDBAdapter.addToLibrary(recordedProgram, library);
            } catch (Exception e) {
                SimpleAppLog.error("Could not mapping recorded program to library", e);
            } finally {
                recordedProgramDBAdapter.close();
            }
        } else {
            SimpleAppLog.info("No library selected");
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
        LibraryDBAdapter adapter = new LibraryDBAdapter(this);
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
        selectedLibrary = obj;
    }

    @Override
    public void onEditItem(Library obj) {

    }

    @Override
    public void onSelectIndex(int index) {
        listView.setSelection(index);
    }
}
