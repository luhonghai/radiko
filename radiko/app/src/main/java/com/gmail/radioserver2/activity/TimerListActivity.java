package com.gmail.radioserver2.activity;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Toast;

import com.gmail.radioserver2.adapter.OnListItemActionListener;
import com.gmail.radioserver2.data.Timer;
import com.gmail.radioserver2.data.sqlite.ext.TimerDBAdapter;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.gmail.radioserver2.view.swipelistview.SwipeListView;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.adapter.TimerAdapter;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

/**
 * Created by luhonghai on 2/21/15.
 */
public class TimerListActivity extends BaseActivity implements View.OnClickListener, OnListItemActionListener<Timer>{

    private Button btnBack;

    private SwipeListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer_list);

        btnBack = (Button) findViewById(R.id.btnBack);
        btnBack.setOnClickListener(this);

        listView = (SwipeListView) findViewById(R.id.list_timer);

        loadData();
    }

    private void loadData() {
        TimerDBAdapter dbAdapter = new TimerDBAdapter(this);
        try {
            dbAdapter.open();
            Collection<Timer> timers = dbAdapter.findAll();
            Timer[] items;
            if (timers != null && timers.size() > 0) {
                items = new Timer[timers.size()];
                timers.toArray(items);
            } else {
                items = new Timer[] {};
            }
            TimerAdapter adapter = new TimerAdapter(this, items, this);
            listView.setAdapter(adapter);
            listView.dismissSelected();
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            SimpleAppLog.error("Could not load timer", e);
        } finally {
            dbAdapter.close();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnBack:
                onBackPressed();
                break;
        }
    }

    @Override
    public void onDeleteItem(Timer obj) {
        TimerDBAdapter adapter = new TimerDBAdapter(this);
        try {
            adapter.open();
            adapter.delete(obj);
            loadData();
        } catch (Exception ex) {
            SimpleAppLog.error("Could not delete timer " + obj.toPrettyString(getApplicationContext()), ex);
        } finally {
            adapter.close();
        }
    }

    @Override
    public void onSelectItem(Timer obj) {

    }

    @Override
    public void onEditItem(Timer obj) {

    }

    @Override
    public void onSelectIndex(int index) {

    }


}
