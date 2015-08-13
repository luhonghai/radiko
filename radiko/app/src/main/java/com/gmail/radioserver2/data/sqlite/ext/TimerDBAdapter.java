package com.gmail.radioserver2.data.sqlite.ext;

import android.content.Context;
import android.database.Cursor;

import com.gmail.radioserver2.data.Timer;
import com.gmail.radioserver2.data.sqlite.DBAdapter;
import com.gmail.radioserver2.utils.DateHelper;

import java.util.Collection;

/**
 * Created by luhonghai on 25/02/2015.
 */

public class TimerDBAdapter extends DBAdapter<Timer> {

    public TimerDBAdapter(Context ctx) {
        super(ctx);
    }

    @Override
    public Cursor getAll() throws Exception {
        return getAll(KEY_CREATED_DATE + " DESC");
    }

    @Override
    public String getTableName() {
        return TABLE_TIMER;
    }

    @Override
    public String[] getAllColumns() {
        return new String[]{
                KEY_ROW_ID,
                KEY_MODE,
                KEY_TYPE,
                KEY_CHANNEL_KEY,
                KEY_CHANNEL_NAME,
                KEY_EVENT_DATE,
                KEY_START_HOUR,
                KEY_START_MINUTE,
                KEY_FINISH_MINUTE,
                KEY_FINISH_HOUR,
                KEY_STATUS,
                KEY_CREATED_DATE
        };
    }

    @Override
    public Timer toObject(Cursor cursor) {
        Timer timer = new Timer();
        timer.setId(cursor.getInt(cursor.getColumnIndex(KEY_ROW_ID)));
        timer.setChannelName(cursor.getString(cursor.getColumnIndex(KEY_CHANNEL_NAME)));
        timer.setChannelKey(cursor.getString(cursor.getColumnIndex(KEY_CHANNEL_KEY)));
        timer.setMode(cursor.getInt(cursor.getColumnIndex(KEY_MODE)));
        timer.setType(cursor.getInt(cursor.getColumnIndex(KEY_TYPE)));
        timer.setStartHour(cursor.getInt(cursor.getColumnIndex(KEY_START_HOUR)));
        timer.setStartMinute(cursor.getInt(cursor.getColumnIndex(KEY_START_MINUTE)));
        timer.setFinishHour(cursor.getInt(cursor.getColumnIndex(KEY_FINISH_HOUR)));
        timer.setFinishMinute(cursor.getInt(cursor.getColumnIndex(KEY_FINISH_MINUTE)));
        timer.setStatus(cursor.getInt(cursor.getColumnIndex(KEY_STATUS)) == 1);
        timer.setEventDate(DateHelper.convertStringToDate(cursor.getString(cursor.getColumnIndex(KEY_EVENT_DATE))));
        timer.setCreatedDate(DateHelper.convertStringToDate(cursor.getString(cursor.getColumnIndex(KEY_CREATED_DATE))));
        return timer;
    }

    public Collection<Timer> findByChannelName(String name) throws Exception {
        return toCollection(getDB().query(getTableName(), getAllColumns(),
                KEY_CHANNEL_NAME + " = ?",
                new String[]{
                        name
                },
                null,
                null,
                KEY_CREATED_DATE + " DESC"));
    }

    @Override
    public Collection<Timer> search(String s) throws Exception {
        return findAll();
    }
}
