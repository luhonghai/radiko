package com.gmail.radioserver2.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.fortysevendeg.swipelistview.SwipeListView;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.adapter.TimerAdapter;

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

        int count = 15;
        String[] items = new String[count];
        for (int i = 0; i < count; i++) {
            items[i] = getResources().getString(R.string.debug_timer_name, (i + 1));
        }
        TimerAdapter adapter = new TimerAdapter(this, items);
        listView.setAdapter(adapter);
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
