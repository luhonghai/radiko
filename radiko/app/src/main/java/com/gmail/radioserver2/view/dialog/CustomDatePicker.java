package com.gmail.radioserver2.view.dialog;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by luhonghai on 2/28/15.
 */
public class CustomDatePicker extends DialogFragment {

    private Calendar c = Calendar.getInstance();
    private int startYear = c.get(Calendar.YEAR);
    private int startMonth = c.get(Calendar.MONTH);
    private int startDay = c.get(Calendar.DAY_OF_MONTH);

    public void setData(DatePickerDialog.OnDateSetListener onDateSetListener) {
        this.onDateSetListener = onDateSetListener;
    }

    public void setData(DatePickerDialog.OnDateSetListener onDateSetListener, Date date) {
        if (date != null) {
            c = Calendar.getInstance();
            c.setTime(date);
            setData(onDateSetListener, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        } else {
            setData(onDateSetListener);
        }
    }

    public void setData(DatePickerDialog.OnDateSetListener onDateSetListener, int year, int month, int day) {
        setData(onDateSetListener);
        this.startYear = year;
        this.startMonth = month;
        this.startDay = day;
    }

    private DatePickerDialog.OnDateSetListener onDateSetListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        DatePickerDialog dialog = new DatePickerDialog(getActivity(), onDateSetListener, startYear, startMonth, startDay);
        return dialog;

    }
}
