package com.gmail.radioserver2.data.sqlite.ext;

import android.content.Context;
import android.database.Cursor;

import com.gmail.radioserver2.data.Library;
import com.gmail.radioserver2.data.sqlite.DBAdapter;
import com.gmail.radioserver2.utils.DateHelper;

/**
 * Created by luhonghai on 25/02/2015.
 */
public class LibraryDBAdapter extends DBAdapter<Library> {

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

    @Override
    public Library toObject(Cursor cursor) {
        Library library = new Library();
        library.setId(cursor.getInt(cursor.getColumnIndex(KEY_ROW_ID)));
        library.setName(cursor.getString(cursor.getColumnIndex(KEY_NAME)));
        library.setCreatedDate(DateHelper.convertStringToDate(cursor.getString(cursor.getColumnIndex(KEY_CREATED_DATE))));
        return library;
    }
}
