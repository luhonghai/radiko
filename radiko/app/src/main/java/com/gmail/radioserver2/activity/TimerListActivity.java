package com.gmail.radioserver2.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.gmail.radioserver2.data.Timer;
import com.gmail.radioserver2.data.sqlite.ext.TimerDBAdapter;
import com.gmail.radioserver2.view.swipelistview.SwipeListView;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.adapter.TimerAdapter;

import java.util.Collection;

/**
 * Created by luhonghai on 2/21/15.
 */
public class TimerListActivity extends BaseActivity implements View.OnClickListener{

    private Button btnBack;

    private SwipeListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer_list);

        btnBack = (Button) findViewById(R.id.btnBack);
        btnBack.setOnClickListener(this);

        listView = (SwipeListView) findViewById(R.id.list_timer);

        TimerDBAdapter dbAdapter = new TimerDBAdapter(this);
        try {
            Collection<Timer> timers = dbAdapter.findAll();
            if (timers != null && timers.size() > 0) {
                Timer[] items = new Timer[timers.size()];
                timers.toArray(items);
                TimerAdapter adapter = new TimerAdapter(this, items);
                listView.setAdapter(adapter);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
}
