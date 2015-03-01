package com.gmail.radioserver2.data.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.sql.SQLException;
import java.util.Collection;

/**
 * Created by luhonghai on 25/02/2015.
 */
public interface IDBAdapter<T> {
    /**
     *  FIELD
     */
    public static final String KEY_ROW_ID = "_id";

    public static final String KEY_CREATED_DATE = "created_date";

    public static final String KEY_NAME = "name";

    public static final String KEY_CHANNEL_NAME = "channel_name";

    public static final String KEY_CHANNEL_KEY = "channel_key";

    public static final String KEY_FILE_PATH = "file_path";

    public static final String KEY_START_TIME = "start_time";

    public static final String KEY_END_TIME = "end_time";

    public static final String KEY_MODE = "mode";

    public static final String KEY_TYPE = "type";

    public static final String KEY_EVENT_DATE = "event_date";

    public static final String KEY_START_HOUR = "start_hour";

    public static final String KEY_START_MINUTE = "start_minute";

    public static final String KEY_FINISH_HOUR = "finish_hour";

    public static final String KEY_FINISH_MINUTE = "finish_minute";

    public static final String KEY_STATUS = "status";

    public static final String KEY_DESCRIPTION = "description";

    public static final String KEY_URL = "url";

    public static final String KEY_LAST_PLAYED_TIME = "last_played_time";

    public static final String KEY_PRIMARY_MAPPING = "primary_mapping";

    public static final String KEY_SECONDARY_MAPPING = "secondary_mapping";

    /**
     *  TABLE
     */
    public static final String TABLE_RECORDED_PROGRAM = "recorded_program";

    public static final String TABLE_TIMER = "timer";

    public static final String TABLE_LIBRARY = "library";

    public static final String TABLE_CHANNEL = "channel";

    public static final String TABLE_RECORDED_PROGRAM_LIBRARY = "recorded_program_library";

    public SQLiteDatabase open() throws SQLException;

    public void close();

    public Cursor getAll() throws Exception;

    public String getTableName();

    public String[] getAllColumns();

    public long insert(T obj) throws Exception;

    public boolean update(T obj) throws Exception;

    public Cursor getAll(String orderBy) throws Exception;

    public Cursor get(long rowId) throws Exception;

    public boolean delete(long rowId) throws Exception;

    public boolean delete(T obj) throws Exception;

    public Collection<T> toCollection(Cursor cursor);

    public abstract T toObject(Cursor cursor);

    public T find(long rowId) throws Exception;

    public Collection<T> findAll() throws Exception;

    public Collection<T> search(String s) throws Exception;
}
