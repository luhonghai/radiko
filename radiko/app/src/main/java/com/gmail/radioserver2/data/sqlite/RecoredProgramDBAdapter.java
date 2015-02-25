package com.gmail.radioserver2.data.sqlite;

import android.content.Context;
import android.database.Cursor;

import com.gmail.radioserver2.data.RecordedProgram;
import com.gmail.radioserver2.utils.DateHelper;


/**
 * Created by luhonghai on 25/02/2015.
 */
public class RecoredProgramDBAdapter extends DBAdapter<RecordedProgram> {

    public RecoredProgramDBAdapter(Context ctx) {
        super(ctx);
    }

    @Override
    public String getTableName() {
        return TABLE_RECORDED_PROGRAM;
    }

    @Override
    public String[] getAllColumns() {
        return new String[]{
                KEY_ROWID,
                KEY_NAME,
                KEY_CHANNEL_KEY,
                KEY_CHANNEL_NAME,
                KEY_FILE_PATH,
                KEY_LAST_PLAYED_TIME,
                KEY_START_TIME,
                KEY_END_TIME,
                KEY_CREATED_DATE
        };
    }

    @Override
    public Cursor getAll() throws Exception {
        return getAll(KEY_LAST_PLAYED_TIME + " DESC");
    }

    @Override
    public RecordedProgram toObject(Cursor cursor) {
        RecordedProgram rp = new RecordedProgram();
        rp.setId(cursor.getInt(cursor.getColumnIndex(KEY_ROWID)));
        rp.setName(cursor.getString(cursor.getColumnIndex(KEY_NAME)));
        rp.setChannelKey(cursor.getString(cursor.getColumnIndex(KEY_CHANNEL_KEY)));
        rp.setChannelName(cursor.getString(cursor.getColumnIndex(KEY_CHANNEL_NAME)));
        rp.setFilePath(cursor.getString(cursor.getColumnIndex(KEY_FILE_PATH)));
        rp.setLastPlayedTime(DateHelper.convertStringToDate(cursor.getString(cursor.getColumnIndex(KEY_LAST_PLAYED_TIME))));
        rp.setStartTime(DateHelper.convertStringToDate(cursor.getString(cursor.getColumnIndex(KEY_START_TIME))));
        rp.setEndTime(DateHelper.convertStringToDate(cursor.getString(cursor.getColumnIndex(KEY_END_TIME))));
        rp.setCreatedDate(DateHelper.convertStringToDate(cursor.getString(cursor.getColumnIndex(KEY_CREATED_DATE))));
        return rp;
    }
}
