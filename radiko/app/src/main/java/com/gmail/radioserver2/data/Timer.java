package com.gmail.radioserver2.data;

import android.content.ContentValues;

import com.gmail.radioserver2.data.sqlite.DBAdapter;
import com.gmail.radioserver2.data.sqlite.IDBAdapter;
import com.gmail.radioserver2.utils.DateHelper;

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
