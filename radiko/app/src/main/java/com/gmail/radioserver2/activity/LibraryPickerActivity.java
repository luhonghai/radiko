package com.gmail.radioserver2.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.fortysevendeg.swipelistview.SwipeListView;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.adapter.LibraryPickerAdapter;

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

        int count = 15;
        String[] items = new String[count];
        for (int i = 0; i < count; i++) {
            items[i] = getResources().getString(R.string.debug_library_name,(i + 1));
        }

        LibraryPickerAdapter adapter = new LibraryPickerAdapter(this, items);
        adapter.setRadioItemSelectedListener(new LibraryPickerAdapter.OnRadioItemSelectListener() {
            @Override
            public void onRadioItemSelected(int position) {
                listView.setSelection(position);
            }
        });
        listView.setAdapter(adapter);


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
