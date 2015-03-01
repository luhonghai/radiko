package com.gmail.radioserver2.data;

import android.content.ContentValues;
import android.content.Context;

import com.gmail.radioserver2.R;
import com.gmail.radioserver2.data.sqlite.DBAdapter;
import com.gmail.radioserver2.utils.DateHelper;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by luhonghai on 24/02/2015.
 */
public class RecordedProgram extends AbstractData<RecordedProgram> {

    private String name;

    private String channelName;

    private String channelKey;

    private String filePath;

    private Date startTime;

    private Date endTime;

    private Date lastPlayedTime;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Date getLastPlayedTime() {
        return lastPlayedTime;
    }

    public void setLastPlayedTime(Date lastPlayedTime) {
        this.lastPlayedTime = lastPlayedTime;
    }

    public String getChannelKey() {
        return channelKey;
    }

    public void setChannelKey(String channelKey) {
        this.channelKey = channelKey;
    }

    @Override
    public String toPrettyString(Context context) {
        StringBuffer sb = new StringBuffer();
        sb.append(getChannelName()).append(" - ");
        sb.append(getName()).append(" - ");
        sb.append(DateHelper.convertDateToString(getStartTime(), context.getString(R.string.default_date_format))).append(" ");
        sb.append(DateHelper.convertDateToHourMinuteString(getStartTime(), getEndTime()));
        return sb.toString();
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(DBAdapter.KEY_NAME, this.getName());
        cv.put(DBAdapter.KEY_CHANNEL_KEY, this.getChannelKey());
        cv.put(DBAdapter.KEY_CHANNEL_NAME, this.getChannelName());
        cv.put(DBAdapter.KEY_FILE_PATH, this.getFilePath());
        if (this.getStartTime() != null)
            cv.put(DBAdapter.KEY_START_TIME, DateHelper.convertDateToString(this.getStartTime()));
        if (this.getStartTime() != null)
            cv.put(DBAdapter.KEY_END_TIME, DateHelper.convertDateToString(this.getEndTime()));
        if (this.getLastPlayedTime() != null)
            cv.put(DBAdapter.KEY_LAST_PLAYED_TIME, DateHelper.convertDateToString(this.getLastPlayedTime()));
        if (this.getCreatedDate() != null)
            cv.put(DBAdapter.KEY_CREATED_DATE, DateHelper.convertDateToString(this.getCreatedDate()));
        return cv;
    }
}
