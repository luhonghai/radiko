package com.gmail.radioserver2.data;

import android.content.ContentValues;
import android.content.Context;

import com.gmail.radioserver2.R;
import com.gmail.radioserver2.data.sqlite.DBAdapter;
import com.gmail.radioserver2.data.sqlite.IDBAdapter;
import com.gmail.radioserver2.utils.DateHelper;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by luhonghai on 25/02/2015.
 */
public class Timer extends AbstractData<Timer> {

    public static final int MODE_DAILY = 0;

    public static final int MODE_WEEKLY = 1;

    public static final int MODE_ONE_TIME = 2;

    public static final int TYPE_RECORDING = 0;

    public static final int TYPE_ALARM = 1;

    public static final int TYPE_SLEEP = 2;

    private int mode;

    private int type;

    private String channelName;

    private String channelKey;

    private Date eventDate;

    private int startHour;

    private int startMinute;

    private int finishHour;

    private int finishMinute;

    private boolean status;

    @Override
    public String toPrettyString(Context context) {
        StringBuffer sb = new StringBuffer();
        sb.append(context.getResources().getStringArray(R.array.mode_type)[type]).append(" - ");
        sb.append(context.getResources().getStringArray(R.array.timer_type)[mode]);
        if (mode == MODE_ONE_TIME) {
            sb.append(" ").append(DateHelper.convertDateToString(eventDate, context.getString(R.string.default_date_format)));
        } else if (mode == MODE_WEEKLY) {
            Calendar c = Calendar.getInstance();
            c.setTime(eventDate);
            int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
            int dowIndex = 0;
            switch (dayOfWeek) {
                case Calendar.MONDAY: dowIndex = 0; break;
                case Calendar.TUESDAY: dowIndex = 1; break;
                case Calendar.WEDNESDAY: dowIndex = 2; break;
                case Calendar.THURSDAY: dowIndex = 3;  break;
                case Calendar.FRIDAY: dowIndex = 4; break;
                case Calendar.SATURDAY: dowIndex = 5; break;
                case Calendar.SUNDAY: dowIndex = 6; break;
            }
            sb.append(" ").append(context.getResources().getStringArray(R.array.day_of_week)[dowIndex]);
        }
        sb.append(" ").append(DateHelper.toTimeNumberString(startHour)).append(":").append(DateHelper.toTimeNumberString(startMinute));
        sb.append(" - ");
        sb.append(DateHelper.toTimeNumberString(finishHour)).append(":").append(DateHelper.toTimeNumberString(finishMinute));
        sb.append(" ");
        sb.append(channelName);
        //sb.append(" | ").append(status ? context.getString(R.string.switch_text_on) : context.getString(R.string.switch_text_off));
        return sb.toString();
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(DBAdapter.KEY_MODE, this.getMode());
        cv.put(DBAdapter.KEY_TYPE, this.getType());
        cv.put(DBAdapter.KEY_CHANNEL_KEY, this.getChannelKey());
        cv.put(DBAdapter.KEY_CHANNEL_NAME, this.getChannelName());
        cv.put(DBAdapter.KEY_START_HOUR, this.getStartHour());
        cv.put(DBAdapter.KEY_START_MINUTE, this.getStartMinute());
        cv.put(DBAdapter.KEY_FINISH_HOUR, this.getFinishHour());
        cv.put(DBAdapter.KEY_FINISH_MINUTE, this.getFinishMinute());
        cv.put(DBAdapter.KEY_STATUS, this.isStatus() ? 1 : 0);
        if (this.getEventDate() != null)
            cv.put(DBAdapter.KEY_EVENT_DATE, DateHelper.convertDateToString(this.getEventDate()));
        if (this.getCreatedDate() != null)
            cv.put(DBAdapter.KEY_CREATED_DATE, DateHelper.convertDateToString(this.getCreatedDate()));
        return cv;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelKey() {
        return channelKey;
    }

    public void setChannelKey(String channelKey) {
        this.channelKey = channelKey;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    public int getStartHour() {
        return startHour;
    }

    public void setStartHour(int startHour) {
        this.startHour = startHour;
    }

    public int getStartMinute() {
        return startMinute;
    }

    public void setStartMinute(int startMinute) {
        this.startMinute = startMinute;
    }

    public int getFinishHour() {
        return finishHour;
    }

    public void setFinishHour(int finishHour) {
        this.finishHour = finishHour;
    }

    public int getFinishMinute() {
        return finishMinute;
    }

    public void setFinishMinute(int finishMinute) {
        this.finishMinute = finishMinute;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}
