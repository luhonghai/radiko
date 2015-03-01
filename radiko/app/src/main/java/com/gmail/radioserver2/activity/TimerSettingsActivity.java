package com.gmail.radioserver2.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import com.gmail.radioserver2.R;
import com.gmail.radioserver2.data.Timer;
import com.gmail.radioserver2.data.sqlite.ext.TimerDBAdapter;
import com.gmail.radioserver2.utils.DateHelper;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.gmail.radioserver2.view.dialog.CustomDatePicker;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by luhonghai on 2/20/15.
 */
public class TimerSettingsActivity extends BaseActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener, DatePickerDialog.OnDateSetListener{

    private static final String CUSTOM_DATE_PICKER_TAG = "custom_date_picker";

    private Button btnTimerList;

    private Button btnSave;

    private Spinner spinnerStartHour;
    private Spinner spinnerFinishHour;
    private Spinner spinnerFinishMinute;
    private Spinner spinnerStartMinute;

    private EditText txtTimePicker;

    private Spinner spinnerTimerType;
    private Spinner spinnerModeType;

    private Spinner spinnerTimePicker;

    private RelativeLayout relativeLayoutTimePicker;

    private Date selectedDate = new Date(System.currentTimeMillis());

    private int selectedMode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        btnTimerList = (Button) findViewById(R.id.btnTimerList);
        btnTimerList.setOnClickListener(this);

        btnSave = (Button) findViewById(R.id.btnSave);
        btnSave.setOnClickListener(this);

        spinnerStartHour = (Spinner) findViewById(R.id.spinnerStartHour);
        spinnerStartHour.setOnItemSelectedListener(this);
        spinnerFinishHour = (Spinner) findViewById(R.id.spinnerFinishHour);
        spinnerFinishHour.setOnItemSelectedListener(this);
        spinnerFinishMinute = (Spinner) findViewById(R.id.spinnerFinishMinute);
        spinnerFinishMinute.setOnItemSelectedListener(this);
        spinnerStartMinute = (Spinner) findViewById(R.id.spinnerStartMinute);
        spinnerStartMinute.setOnItemSelectedListener(this);

        txtTimePicker = (EditText) findViewById(R.id.txtTimePicker);
        txtTimePicker.setOnClickListener(this);
        txtTimePicker.setFocusable(false);

        spinnerTimerType = (Spinner) findViewById(R.id.spinnerTimerType);
        spinnerTimerType.setOnItemSelectedListener(this);
        spinnerModeType = (Spinner) findViewById(R.id.spinnerModeType);
        spinnerModeType.setOnItemSelectedListener(this);

        spinnerTimePicker = (Spinner) findViewById(R.id.spinnerTimePicker);
        relativeLayoutTimePicker = (RelativeLayout) findViewById(R.id.relativeLayoutTimePicker);

        loadStartTime();
        loadFinishHour();
        loadFinishMinute(true);
        showDate();
        showTimePicker();
    }

    private void showTimePicker() {
        selectedMode = spinnerTimerType.getSelectedItemPosition();
        if (selectedMode == Timer.MODE_WEEKLY) {
            txtTimePicker.setVisibility(View.GONE);
            relativeLayoutTimePicker.setVisibility(View.VISIBLE);
        } else if (selectedMode == Timer.MODE_ONE_TIME) {
            txtTimePicker.setVisibility(View.VISIBLE);
            relativeLayoutTimePicker.setVisibility(View.GONE);
        } else {
            txtTimePicker.setVisibility(View.INVISIBLE);
            relativeLayoutTimePicker.setVisibility(View.GONE);
        }
    }

    private void loadStartTime() {
        ArrayAdapter<CharSequence> adapterH = new ArrayAdapter<CharSequence>(this, R.layout.textview_spinner, DateHelper.getTimeList(0, 23));
        spinnerStartHour.setAdapter(adapterH);
        adapterH.notifyDataSetChanged();
        ArrayAdapter<CharSequence> adapterM = new ArrayAdapter<CharSequence>(this, R.layout.textview_spinner, DateHelper.getTimeList(0, 59));
        spinnerStartMinute.setAdapter(adapterM);
        adapterM.notifyDataSetChanged();
    }

    private void loadFinishHour() {
        int startHour = Integer.parseInt(spinnerStartHour.getSelectedItem().toString());
        loadFinishHour(startHour);
    }

    private void loadFinishHour(int startHour) {
        ArrayAdapter<CharSequence> adapter =new ArrayAdapter<CharSequence>(this, R.layout.textview_spinner, DateHelper.getTimeList(startHour, 23));
        spinnerFinishHour.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private void loadFinishMinute(boolean doInit) {
        int startMinute = Integer.parseInt(spinnerStartMinute.getSelectedItem().toString());
        int startHour = Integer.parseInt(spinnerStartHour.getSelectedItem().toString());
        int finishHour = Integer.parseInt(spinnerFinishHour.getSelectedItem().toString());

        if (finishHour <= startHour) {
            ArrayAdapter<CharSequence>  adapter = new ArrayAdapter<CharSequence>(this, R.layout.textview_spinner, DateHelper.getTimeList(startMinute + 1, 59));
            spinnerFinishMinute.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        } else if (doInit) {
            ArrayAdapter<CharSequence>  adapter = new ArrayAdapter<CharSequence>(this, R.layout.textview_spinner, DateHelper.getTimeList(0, 59));
            spinnerFinishMinute.setAdapter(adapter);
            adapter.notifyDataSetChanged();

        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnTimerList:
                Intent intent = new Intent();
                intent.setClass(this,TimerListActivity.class);
                startActivity(intent);
                break;
            case R.id.txtTimePicker:
                CustomDatePicker datePicker = new CustomDatePicker();
                datePicker.setData(this, selectedDate);
                datePicker.show(getFragmentManager(), CUSTOM_DATE_PICKER_TAG);
                break;
            case R.id.btnSave:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        saveTimer();
                    }
                }).start();
                onBackPressed();
                break;
        }
    }

    private void saveTimer() {

        TimerDBAdapter dbAdapter = new TimerDBAdapter(this);
        try {
            dbAdapter.open();
            Timer timer = new Timer();
            //TODO need to add channel info
            timer.setChannelKey("");
            timer.setChannelName(getString(R.string.default_channel_name));

            int mode = spinnerTimerType.getSelectedItemPosition();
            int type = spinnerModeType.getSelectedItemPosition();
            if (mode == Timer.MODE_WEEKLY) {
                Calendar c = Calendar.getInstance();
                int dayOfWeek;
                switch (spinnerTimePicker.getSelectedItemPosition()) {
                    case 0: dayOfWeek = Calendar.MONDAY; break;
                    case 1:dayOfWeek = Calendar.TUESDAY; break;
                    case 2: dayOfWeek = Calendar.WEDNESDAY; break;
                    case 3: dayOfWeek = Calendar.THURSDAY; break;
                    case 4: dayOfWeek = Calendar.FRIDAY; break;
                    case 5:dayOfWeek = Calendar.SATURDAY; break;
                    case 6:
                    default:dayOfWeek = Calendar.SUNDAY; break;
                }
                c.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                timer.setEventDate(c.getTime());
            } else {
                timer.setEventDate(selectedDate);
            }
            timer.setMode(mode);
            timer.setType(type);

            timer.setStartHour(Integer.parseInt(spinnerStartHour.getSelectedItem().toString()));
            timer.setStartMinute(Integer.parseInt(spinnerStartMinute.getSelectedItem().toString()));
            timer.setFinishHour(Integer.parseInt(spinnerFinishHour.getSelectedItem().toString()));
            timer.setFinishMinute(Integer.parseInt(spinnerFinishMinute.getSelectedItem().toString()));

            dbAdapter.insert(timer);
        } catch (Exception ex) {
            SimpleAppLog.error("Could not save timer",ex);
        } finally {
            dbAdapter.close();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == null) return;
        switch (parent.getId()) {
            case R.id.spinnerFinishHour: {
                loadFinishMinute(false);
                break;
            }
            case R.id.spinnerFinishMinute: {
                break;
            }
            case R.id.spinnerStartHour: {
                loadFinishHour();
                loadFinishMinute(false);
                break;
            }
            case R.id.spinnerStartMinute: {
                loadFinishMinute(false);
                break;
            }
            case R.id.spinnerModeType: {
                break;
            }
            case R.id.spinnerTimerType: {
                showTimePicker();
                break;
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private void showDate() {
        txtTimePicker.setText(DateHelper.convertDateToString(selectedDate, getResources().getString(R.string.default_date_format)));
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, monthOfYear);
        c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        selectedDate = c.getTime();
        showDate();
    }
}
