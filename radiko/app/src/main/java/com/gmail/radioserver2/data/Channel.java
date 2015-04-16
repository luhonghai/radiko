package com.gmail.radioserver2.data;

import android.content.ContentValues;
import android.content.Context;

import com.dotohsoft.radio.data.RadioProgram;
import com.gmail.radioserver2.data.sqlite.DBAdapter;
import com.gmail.radioserver2.utils.DateHelper;
import com.gmail.radioserver2.utils.StringUtil;

import java.util.Date;

/**
 * Created by luhonghai on 25/02/2015.
 */

public class Channel extends AbstractData<Channel> {

    private String name;

    private String key;

    private String type;

    private String description;

    private Date lastPlayedTime;

    private String url;

    private RadioProgram.Program currentProgram;

    public String getRecordedName() {
        if (currentProgram == null) {
            return name + "-" + key +"-" + type;
        } else {
            return name + "-" + key +"-" + type + "-" + currentProgram.getTitle();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof Channel) {
            return this.getUrl().equalsIgnoreCase(((Channel) o).getUrl());
        }
        return super.equals(o);
    }

    @Override
    public String toPrettyString(Context context) {
        return name;
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(DBAdapter.KEY_NAME, getName());
        cv.put(DBAdapter.KEY_CHANNEL_KEY, getKey());
        cv.put(DBAdapter.KEY_TYPE, getType());
        cv.put(DBAdapter.KEY_DESCRIPTION, getDescription());
        cv.put(DBAdapter.KEY_URL, getUrl());
        if (getLastPlayedTime() != null)
            cv.put(DBAdapter.KEY_LAST_PLAYED_TIME, DateHelper.convertDateToString(getLastPlayedTime()));
        if (getCreatedDate() != null)
            cv.put(DBAdapter.KEY_CREATED_DATE, DateHelper.convertDateToString(getCreatedDate()));
        return cv;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = StringUtil.escapeJapanSpecialChar(name);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getType() {
        return type;
    }

    public boolean isType(String radioType) {
        if (this.type == null) return false;
        return this.type.equalsIgnoreCase(radioType);
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        if (url == null) return "";
        return url;
    }

    public void setUrl(String url) {
        if (url != null) url = url.trim();
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getLastPlayedTime() {
        return lastPlayedTime;
    }

    public void setLastPlayedTime(Date lastPlayedTime) {
        this.lastPlayedTime = lastPlayedTime;
    }

    public RadioProgram.Program getCurrentProgram() {
        return currentProgram;
    }

    public void setCurrentProgram(RadioProgram.Program currentProgram) {
        this.currentProgram = currentProgram;
    }
}
