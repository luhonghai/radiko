package com.gmail.radioserver2.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.gmail.radioserver2.R;
import com.gmail.radioserver2.adapter.LibraryPickerAdapter;
import com.gmail.radioserver2.adapter.OnListItemActionListener;
import com.gmail.radioserver2.data.Library;
import com.gmail.radioserver2.data.RecordedProgram;
import com.gmail.radioserver2.data.sqlite.ext.LibraryDBAdapter;
import com.gmail.radioserver2.data.sqlite.ext.RecordedProgramDBAdapter;
import com.gmail.radioserver2.utils.Constants;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.gmail.radioserver2.view.swipelistview.BaseSwipeListViewListener;
import com.gmail.radioserver2.view.swipelistview.SwipeListView;
import com.google.gson.Gson;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

/**
 * Created by luhonghai on 2/22/15.
 */
public class LibraryPickerActivity extends BaseActivity implements View.OnClickListener, OnListItemActionListener<Library> {

    private SwipeListView listView;

    private Button btnSave;

    private EditText txtLibraryName;

    private RecordedProgram recordedProgram;

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
            Collection<Library> libraries = dbAdapter.findAll();
            LibraryPickerAdapter adapter = new LibraryPickerAdapter(this);
            listView.setAdapter(adapter);
            adapter.setDataList(libraries);
            adapter.setOnListItemActionListener(this);
            listView.dismissSelected();
        } catch (Exception e) {
            SimpleAppLog.error("Could not load library", e);
        } finally {
            dbAdapter.close();
        }
    }

    private void save() {
        String newLibraryName = txtLibraryName.getText().toString();
        Library library = null;
        if (newLibraryName.length() != 0) {
            LibraryDBAdapter libraryDBAdapter = new LibraryDBAdapter(this);
            try {
                libraryDBAdapter.open();
                Collection<Library> libraries = libraryDBAdapter.findAll();
                for (Library item : libraries) {
                    if (item.getName().equalsIgnoreCase(newLibraryName)) {
                        library = item;
                        break;
                    }
                }
                if (library == null) {
                    library = new Library();
                    library.setName(newLibraryName);
                    library.setCreatedDate(new Date());
                    long libraryID = libraryDBAdapter.insert(library);
                    library.setId(libraryID);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                libraryDBAdapter.close();
            }
        } else {
            LibraryPickerAdapter libraryPickerAdapter = (LibraryPickerAdapter) listView.getAdapter();
            library = libraryPickerAdapter.getLastSelected();
        }
        if (library != null) {
            RecordedProgramDBAdapter recordedProgramDBAdapter = new RecordedProgramDBAdapter(this);
            try {
                recordedProgramDBAdapter.open();
                recordedProgramDBAdapter.addToLibrary(recordedProgram, library);
                Intent intent = new Intent(Constants.INTENT_FILTER_FRAGMENT_ACTION);
                intent.putExtra(Constants.FRAGMENT_ACTION_TYPE, Constants.ACTION_RELOAD_RECORDED_PROGRAM);
                sendBroadcast(intent);
                finish();
            } catch (Exception e) {
                SimpleAppLog.error("Could not mapping recorded program to library", e);
            } finally {
                recordedProgramDBAdapter.close();
            }
        } else {
            SimpleAppLog.debug("No library");
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
        //passed
    }

    @Override
    public void onEditItem(Library obj) {
        //passed
    }

    @Override
    public void onSelectIndex(int index) {

    }
}
