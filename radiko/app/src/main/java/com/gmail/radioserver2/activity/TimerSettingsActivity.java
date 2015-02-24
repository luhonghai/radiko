package com.gmail.radioserver2.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.gmail.radioserver2.R;

/**
 * Created by luhonghai on 2/20/15.
 */
public class TimerSettingsActivity extends BaseActivity implements View.OnClickListener {

    private Button btnTimerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        btnTimerList = (Button) findViewById(R.id.btnTimerList);
        btnTimerList.setOnClickListener(this);

        ArrayAdapter<CharSequence> hourListAdapter = new ArrayAdapter<CharSequence>(this,R.layout.textview_spinner, getResources().getTextArray(R.array.hour_list));
        ArrayAdapter<CharSequence> minuteListAdapter = new ArrayAdapter<CharSequence>(this,R.layout.textview_spinner, getResources().getTextArray(R.array.minute_list));
        ((Spinner) findViewById(R.id.spinnerStartHour)).setAdapter(hourListAdapter);
        ((Spinner) findViewById(R.id.spinnerFinishHour)).setAdapter(hourListAdapter);
        ((Spinner) findViewById(R.id.spinnerFinishMinute)).setAdapter(minuteListAdapter);
        ((Spinner) findViewById(R.id.spinnerStartMinute)).setAdapter(minuteListAdapter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnTimerList:
                Intent intent = new Intent();
                intent.setClass(this,TimerListActivity.class);
                startActivity(intent);
                break;
        }
    }
}
