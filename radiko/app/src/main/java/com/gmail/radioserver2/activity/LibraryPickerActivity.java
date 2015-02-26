package com.gmail.radioserver2.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.gmail.radioserver2.data.Library;
import com.gmail.radioserver2.data.sqlite.ext.LibraryDBAdapter;
import com.gmail.radioserver2.view.swipelistview.SwipeListView;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.adapter.LibraryPickerAdapter;

import java.util.Collection;

/**
 * Created by luhonghai on 2/22/15.
 */
public class LibraryPickerActivity extends BaseActivity implements View.OnClickListener {

    private SwipeListView listView;

    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library_picker_list);

        btnSave = (Button) findViewById(R.id.btnSave);
        btnSave.setOnClickListener(this);

        listView = (SwipeListView) findViewById(R.id.list_library);

        LibraryDBAdapter dbAdapter = new LibraryDBAdapter(this);
        try {
            Collection<Library> libraries = dbAdapter.findAll();
            if (libraries != null && libraries.size() > 0) {
                Library[] items = new Library[libraries.size()];
                libraries.toArray(items);
                LibraryPickerAdapter adapter = new LibraryPickerAdapter(this, items);
                adapter.setRadioItemSelectedListener(new LibraryPickerAdapter.OnRadioItemSelectListener() {
                    @Override
                    public void onRadioItemSelected(int position) {
                        listView.setSelection(position);
                    }
                });
                listView.setAdapter(adapter);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSave:
                onBackPressed();
                break;
        }
    }
}
