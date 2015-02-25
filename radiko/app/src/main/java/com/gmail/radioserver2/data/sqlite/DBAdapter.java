package com.gmail.radioserver2.data.sqlite;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.gmail.radioserver2.data.AbstractData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by luhonghai on 25/02/2015.
 */
public abstract class DBAdapter<T> {

    public static final String KEY_ROWID = "_id";

    public static final String KEY_CREATED_DATE = "created_date";

    public static final String KEY_NAME = "name";

    public static final String KEY_CHANNEL_NAME = "channel_name";

    public static final String KEY_CHANNEL_KEY = "channel_key";

    public static final String KEY_FILE_PATH = "file_path";

    public static final String KEY_START_TIME = "start_time";

    public static final String KEY_END_TIME = "end_time";

    public static final String KEY_LAST_PLAYED_TIME = "last_played_time";

    private static final String TAG = "DBAdapter";

    private static final String DATABASE_NAME = "main_db";

    private static final int DATABASE_VERSION = 1;

    /**
     *  Recorded program table
     */
    protected static final String TABLE_RECORDED_PROGRAM = "recorded_program";

    private static final String[] DATABASE_TABLE_CREATE = new String[] {
                    "create table " + TABLE_RECORDED_PROGRAM
                        + " ("
                        + KEY_ROWID +" integer primary key autoincrement, "
                        + KEY_NAME + " text not null, "
                        + KEY_CHANNEL_NAME + " text, "
                        + KEY_CHANNEL_KEY + " text, "
                        + KEY_FILE_PATH + " text not null, "
                        + KEY_START_TIME + " date, "
                        + KEY_END_TIME + " date, "
                        + KEY_LAST_PLAYED_TIME + " date, "
                        + KEY_CREATED_DATE + " date not null"
                        + ");"
    };

    private final Context context;

    private DatabaseHelper DBHelper;

    private SQLiteDatabase db;


    public DBAdapter(Context ctx)
    {
        this.context = ctx;
        DBHelper = new DatabaseHelper(getContext());
    }

    public Context getContext() {
        return context;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper
    {
        private Context currentContext;

        DatabaseHelper(Context context)
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            this.currentContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            for (String query : DATABASE_TABLE_CREATE) {
                db.execSQL(query);
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion,
                              int newVersion)
        {
            //Log.w(TAG, "Upgrading database from version " + oldVersion
            //        + " to "
            //        + newVersion + ", which will destroy all old data");
            //db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
            //onCreate(db);
        }
    }

    protected SQLiteDatabase getDB() {
        return db;
    }

    //---opens the database---
    public SQLiteDatabase open() throws SQLException
    {
        db = DBHelper.getWritableDatabase();
        return db;
    }

    //---closes the database---
    public void close()
    {
        DBHelper.close();
    }

    public abstract Cursor getAll() throws Exception;

    public abstract String getTableName();

    public abstract String[] getAllColumns();

    public long insert(T obj) throws Exception {
        if (obj instanceof AbstractData) {
            return getDB().insert(getTableName(), null, ((AbstractData) obj).toContentValues());
        } else {
            return -1;
        }
    }

    public boolean update(T obj) throws Exception {
        if (obj instanceof AbstractData) {
            AbstractData data = (AbstractData) obj;
            return getDB().update(TABLE_RECORDED_PROGRAM, data.toContentValues(),
                    KEY_ROWID + "=" + data.getId(), null) > 0;
        } else {
            return false;
        }
    }

    public Cursor getAll(String orderBy) throws Exception {
        return getDB().query(getTableName(), getAllColumns(),
                null,
                null,
                null,
                null,
                orderBy);
    }

    public Cursor get(long rowId) throws Exception {
        Cursor mCursor =
                getDB().query(true, getTableName(), getAllColumns(),
                        KEY_ROWID + "=" + rowId,
                        null,
                        null,
                        null,
                        null,
                        null);

        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public boolean delete(long rowId) throws Exception {
        return getDB().delete(TABLE_RECORDED_PROGRAM, KEY_ROWID + "=" + rowId, null) > 0;
    }

    public Collection<T> toCollection(Cursor cursor) {
        Collection<T> list = new ArrayList<T>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            list.add(toObject(cursor));
            cursor.moveToNext();
        }
        return list;
    }

    public abstract T toObject(Cursor cursor);

    public T find(long rowId) throws Exception {
        return toObject(get(rowId));
    }

    public Collection<T> findAll() throws Exception {
        return toCollection(getAll());
    }
}
