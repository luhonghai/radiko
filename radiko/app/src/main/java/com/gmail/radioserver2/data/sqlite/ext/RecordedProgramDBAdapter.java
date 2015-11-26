package com.gmail.radioserver2.data.sqlite.ext;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.gmail.radioserver2.data.Library;
import com.gmail.radioserver2.data.RecordedProgram;
import com.gmail.radioserver2.data.sqlite.DBAdapter;
import com.gmail.radioserver2.utils.DateHelper;

import java.util.Collection;
import java.util.Date;

/**
 * Created by luhonghai on 25/02/2015.
 */
public class RecordedProgramDBAdapter extends DBAdapter<RecordedProgram> {

    private static final String QUERY_SELECT_RECORDED_PROGRAM_BY_LIBRARY = "select rp." + KEY_ROW_ID
            + ", rp." + KEY_NAME
            + ", rp." + KEY_CHANNEL_KEY
            + ", rp." + KEY_CHANNEL_NAME
            + ", rp." + KEY_FILE_PATH
            + ", rp." + KEY_LAST_PLAYED_TIME
            + ", rp." + KEY_START_TIME
            + ", rp." + KEY_END_TIME
            + ", rp." + KEY_CREATED_DATE
            + " from " +
            " ((" + TABLE_RECORDED_PROGRAM + " as rp " +
            "inner join " + TABLE_RECORDED_PROGRAM_LIBRARY + " as rpl " +
            "on rp." + KEY_ROW_ID + "=rpl." + KEY_PRIMARY_MAPPING + ")" +
            " inner join " + TABLE_LIBRARY + " as l on rpl." + KEY_SECONDARY_MAPPING + "=l." + KEY_ROW_ID + ")" +
            " where l." + KEY_ROW_ID + "=?";

    private static final String QUERY_SELECT_RECORDED_PROGRAM_NOT_BY_LIBRARY = "select rp." + KEY_ROW_ID
            + ", rp." + KEY_NAME
            + ", rp." + KEY_CHANNEL_KEY
            + ", rp." + KEY_CHANNEL_NAME
            + ", rp." + KEY_FILE_PATH
            + ", rp." + KEY_LAST_PLAYED_TIME
            + ", rp." + KEY_START_TIME
            + ", rp." + KEY_END_TIME
            + ", rp." + KEY_CREATED_DATE
            + " from (" + TABLE_RECORDED_PROGRAM + " as rp " +
            "left join " + TABLE_RECORDED_PROGRAM_LIBRARY + " as rpl "
            + "on rp." + KEY_ROW_ID + "=rpl." + KEY_PRIMARY_MAPPING + ")" +
            " where rpl." + KEY_PRIMARY_MAPPING + " is null";

    public RecordedProgramDBAdapter(Context ctx) {
        super(ctx);
    }

    @Override
    public String getTableName() {
        return TABLE_RECORDED_PROGRAM;
    }

    @Override
    public String[] getAllColumns() {
        return new String[]{
                KEY_ROW_ID,
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
        return getAll(KEY_LAST_PLAYED_TIME + " DESC, " + KEY_CREATED_DATE + " DESC");
    }

    public Collection<RecordedProgram> findByLibrary(Library library, String search) throws Exception {
        String query = QUERY_SELECT_RECORDED_PROGRAM_BY_LIBRARY;
        String[] args;
        if (search != null && search.length() > 0) {
            query += " and (rp." + KEY_NAME + " like ? or rp." + KEY_CHANNEL_NAME + " like ?)";
            args = new String[]{
                    Long.toString(library.getId()),
                    "%" + search + "%",
                    "%" + search + "%"
            };
        } else {
            args = new String[]{
                    Long.toString(library.getId())
            };
        }
        query += " order by rp." + KEY_LAST_PLAYED_TIME + " DESC, rp." + KEY_CREATED_DATE + " DESC";
        return toCollection(getDB().rawQuery(
                query,
                args));
    }

    public void deleteAllMapping(RecordedProgram program) throws Exception {
        getDB().delete(TABLE_RECORDED_PROGRAM_LIBRARY,
                KEY_PRIMARY_MAPPING + "=?",
                new String[]{
                        Long.toString(program.getId())
                });
    }

    public boolean addToLibrary(RecordedProgram program, Library library) throws Exception {
        // Insert new mapping
        ContentValues cv = new ContentValues();
        cv.put(KEY_PRIMARY_MAPPING, program.getId());
        cv.put(KEY_SECONDARY_MAPPING, library.getId());
        cv.put(KEY_CREATED_DATE, DateHelper.convertDateToString(new Date(System.currentTimeMillis())));
        return getDB().insert(TABLE_RECORDED_PROGRAM_LIBRARY, null, cv) != -1;
    }

    @Override
    public RecordedProgram toObject(Cursor cursor) {
        RecordedProgram rp = new RecordedProgram();
        rp.setId(cursor.getInt(cursor.getColumnIndex(KEY_ROW_ID)));
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

    @Override
    public Collection<RecordedProgram> search(String s) throws Exception {
        String query = QUERY_SELECT_RECORDED_PROGRAM_NOT_BY_LIBRARY;
        String[] args;

        if (s != null && s.length() > 0) {
            query += " and (rp." + KEY_NAME + " like ? or rp." + KEY_CHANNEL_NAME + " like ?)";
            args = new String[]{"%" + s + "%", "%" + s + "%"};
        } else {
            args = null;
        }
        query += " order by rp." + KEY_LAST_PLAYED_TIME + " DESC, rp." + KEY_CREATED_DATE + " DESC";
        Cursor cs = getDB().rawQuery(query, args);
        Log.d("SEE", cs.toString());
        return toCollection(cs);
    }

    public RecordedProgram findByFilePath(String filePath) throws Exception {
        Cursor mCursor =
                getDB().query(true, getTableName(), getAllColumns(),
                        KEY_FILE_PATH + "=?",
                        new String[]{filePath},
                        null,
                        null,
                        null,
                        null);
        if (mCursor != null) {
            mCursor.moveToFirst();
            return toObject(mCursor);
        } else {
            return null;
        }
    }
}
