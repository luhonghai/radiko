package com.gmail.radioserver2.data.sqlite;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.gmail.radioserver2.data.AbstractData;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * Created by luhonghai on 25/02/2015.
 */
public abstract class DBAdapter<T> implements IDBAdapter<T> {

    private static final String TAG = "DBAdapter";

    private static final String DATABASE_NAME = "main_db";

    private static final int DATABASE_VERSION = 2;

    private static final String[] DATABASE_TABLE_CREATE = new String[]{
            "create table " + TABLE_RECORDED_PROGRAM
                    + " ("
                    + KEY_ROW_ID + " integer primary key autoincrement, "
                    + KEY_NAME + " text not null, "
                    + KEY_CHANNEL_NAME + " text, "
                    + KEY_CHANNEL_KEY + " text, "
                    + KEY_FILE_PATH + " text not null, "
                    + KEY_START_TIME + " date, "
                    + KEY_END_TIME + " date, "
                    + KEY_LAST_PLAYED_TIME + " date, "
                    + KEY_CREATED_DATE + " date not null"
                    + ");"
            ,
            "create table " + TABLE_TIMER
                    + " ("
                    + KEY_ROW_ID + " integer primary key autoincrement, "
                    + KEY_MODE + " integer,"
                    + KEY_TYPE + " integer,"
                    + KEY_CHANNEL_NAME + " text, "
                    + KEY_CHANNEL_KEY + " text, "
                    + KEY_EVENT_DATE + " date, "
                    + KEY_START_HOUR + " integer,"
                    + KEY_START_MINUTE + " integer,"
                    + KEY_FINISH_HOUR + " integer,"
                    + KEY_FINISH_MINUTE + " integer,"
                    + KEY_STATUS + " integer default 1,"
                    + KEY_CREATED_DATE + " date not null"
                    + ");"
            ,
            "create table " + TABLE_LIBRARY
                    + " ("
                    + KEY_ROW_ID + " integer primary key autoincrement, "
                    + KEY_NAME + " text not null, "
                    + KEY_CREATED_DATE + " date not null"
                    + ");"
            ,
            "create table " + TABLE_CHANNEL
                    + " ("
                    + KEY_ROW_ID + " integer primary key autoincrement, "
                    + KEY_NAME + " text not null, "
                    + KEY_CHANNEL_KEY + " text, "
                    + KEY_TYPE + " text, "
                    + KEY_DESCRIPTION + " text, "
                    + KEY_URL + " text, "
                    + KEY_LAST_PLAYED_TIME + " date, "
                    + KEY_CREATED_DATE + " date not null, "
                    + KEY_RADIKO_AREA_ID + " text, "
                    + KEY_REGION_ID + " text"
                    + ");"
            ,
            "create table " + TABLE_RECORDED_PROGRAM_LIBRARY
                    + " ("
                    + KEY_ROW_ID + " integer primary key autoincrement, "
                    + KEY_PRIMARY_MAPPING + " integer not null, "
                    + KEY_SECONDARY_MAPPING + " integer not null, "
                    + KEY_CREATED_DATE + " date not null"
                    + ");"
    };

    private final Context context;

    private DatabaseHelper DBHelper;

    private SQLiteDatabase db;


    public DBAdapter(Context ctx) {
        this.context = ctx;
        DBHelper = new DatabaseHelper(getContext());
    }

    public Context getContext() {
        return context;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private Context currentContext;

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            this.currentContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            for (String query : DATABASE_TABLE_CREATE) {
                db.execSQL(query);
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            switch (oldVersion) {
                case 1:
                    db.execSQL("ALTER TABLE " + TABLE_CHANNEL + " ADD " + KEY_RADIKO_AREA_ID + " text");
                    db.execSQL("ALTER TABLE " + TABLE_CHANNEL + " ADD " + KEY_REGION_ID + " text");
                    break;
            }
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
    public SQLiteDatabase open() throws SQLException {
        db = DBHelper.getWritableDatabase();
        return db;
    }

    //---closes the database---
    public void close() {
        try {
            DBHelper.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public long insert(T obj) throws Exception {
        if (obj instanceof AbstractData) {
            AbstractData data = (AbstractData) obj;
            data.setCreatedDate(new Date(System.currentTimeMillis()));
            return getDB().insert(getTableName(), null, data.toContentValues());
        } else {
            return -1;
        }
    }

    public boolean update(T obj) throws Exception {
        if (obj instanceof AbstractData) {
            AbstractData data = (AbstractData) obj;
            return getDB().update(getTableName(), data.toContentValues(),
                    KEY_ROW_ID + "=" + data.getId(), null) > 0;
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
                        KEY_ROW_ID + "=" + rowId,
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

    public boolean delete(T obj) throws Exception {
        if (obj instanceof AbstractData) {
            return delete(((AbstractData) obj).getId());
        }
        return false;
    }

    public boolean delete(long rowId) throws Exception {
        return getDB().delete(getTableName(), KEY_ROW_ID + "=" + rowId, null) > 0;
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

    public T find(long rowId) throws Exception {
        return toObject(get(rowId));
    }

    public Collection<T> findAll() throws Exception {
        return toCollection(getAll());
    }
}
