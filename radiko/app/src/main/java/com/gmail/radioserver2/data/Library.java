package com.gmail.radioserver2.data;

import android.content.ContentValues;
import android.content.Context;

import com.gmail.radioserver2.data.sqlite.DBAdapter;
import com.gmail.radioserver2.utils.DateHelper;
import com.gmail.radioserver2.utils.StringUtil;

import java.util.Collection;

/**
 * Created by luhonghai on 25/02/2015.
 */
public class Library extends AbstractData<Library> {

    private String name;

    private Collection<RecordedProgram> recordedPrograms;

    @Override
    public String toPrettyString(Context context) {
        return name;
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(DBAdapter.KEY_NAME, getName());
        if (getCreatedDate() != null)
            cv.put(DBAdapter.KEY_CREATED_DATE, DateHelper.convertDateToString(getCreatedDate()));
        return cv;
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof Library) {
            return this.getName().equalsIgnoreCase(((Library) o).getName());
        }
        return super.equals(o);
    }

    public String getName() {
        if (name == null) return "";
        return name;
    }

    public void setName(String name) {
        this.name = StringUtil.escapeJapanSpecialChar(name);
    }

    public Collection<RecordedProgram> getRecordedPrograms() {
        return recordedPrograms;
    }

    public void setRecordedPrograms(Collection<RecordedProgram> recordedPrograms) {
        this.recordedPrograms = recordedPrograms;
    }
}
