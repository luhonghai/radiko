package com.gmail.radioserver2.data.sqlite.ext;

import android.content.Context;
import android.database.Cursor;

import com.gmail.radioserver2.data.Library;
import com.gmail.radioserver2.data.RecordedProgram;
import com.gmail.radioserver2.data.sqlite.DBAdapter;
import com.gmail.radioserver2.utils.DateHelper;

import java.util.Collection;

/**
 * Created by luhonghai on 25/02/2015.
 */
public class LibraryDBAdapter extends DBAdapter<Library> {

    private static final String QUERY_SELECT_LIBRARY_BY_RECORDED_PROGRAM = "select l." + KEY_ROW_ID
            + ", l." + KEY_NAME
            + ", l." + KEY_CREATED_DATE
            + " from " +
            " ((" + TABLE_RECORDED_PROGRAM + " as rp " +
            "inner join " + TABLE_RECORDED_PROGRAM_LIBRARY + " as rpl " +
            "on rp." + KEY_ROW_ID + "=rpl." + KEY_PRIMARY_MAPPING + ")" +
            " inner join " + TABLE_LIBRARY + " as l on rpl." + KEY_SECONDARY_MAPPING + "=l." + KEY_ROW_ID + ")" +
            " where rp." + KEY_ROW_ID + "=?";

    public LibraryDBAdapter(Context ctx) {
        super(ctx);
    }

    @Override
    public Cursor getAll() throws Exception {
        return getAll(KEY_NAME + " ASC");
    }

    @Override
    public String getTableName() {
        return TABLE_LIBRARY;
    }

    @Override
    public String[] getAllColumns() {
        return new String[] {
                KEY_ROW_ID,
                KEY_NAME,
                KEY_CREATED_DATE
        };
    }

    public Library findByRecordedProgram(RecordedProgram program) {
        Cursor cursor = getDB().rawQuery(QUERY_SELECT_LIBRARY_BY_RECORDED_PROGRAM,
                new String[] {
                        Long.toString(program.getId())
                });
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            return toObject(cursor);
        }
        return null;
    }

    @Override
    public Library toObject(Cursor cursor) {
        Library library = new Library();
        library.setId(cursor.getInt(cursor.getColumnIndex(KEY_ROW_ID)));
        library.setName(cursor.getString(cursor.getColumnIndex(KEY_NAME)));
        library.setCreatedDate(DateHelper.convertStringToDate(cursor.getString(cursor.getColumnIndex(KEY_CREATED_DATE))));
        return library;
    }

    @Override
    public Collection<Library> search(String s) throws Exception {
        if (s == null || s.length() == 0) return findAll();
        return toCollection(getDB().query(getTableName(), getAllColumns(),
                KEY_NAME + " like ?",
                new String[] {
                        "%" + s + "%"
                },
                null,
                null,
                KEY_NAME + " ASC"));
    }

    @Override
    public long insert(Library obj) throws Exception {
        Cursor cursor = getDB().query(getTableName(), getAllColumns(),
                KEY_NAME + "=?",
                new String[]{
                        obj.getName()
                },
                null,
                null,
                null);
        if (cursor.getCount() > 0) {
            long oldId =cursor.getLong(cursor.getColumnIndex(KEY_ROW_ID));
            Library oldObject = find(oldId);
            obj.setId(oldId);
            obj.setCreatedDate(oldObject.getCreatedDate());
            update(obj);
            return oldId;
        }
        return super.insert(obj);
    }
}
