package com.gmail.radioserver2.data;

import android.content.ContentValues;

import com.gmail.radioserver2.data.sqlite.DBAdapter;
import com.gmail.radioserver2.utils.DateHelper;

import java.util.Collection;

/**
 * Created by luhonghai on 25/02/2015.
 */
public class Library extends AbstractData<Library> {

    private String name;

    private Collection<RecordedProgram> recordedPrograms;

    @Override
    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(DBAdapter.KEY_NAME, getName());
        if (getCreatedDate() != null)
            cv.put(DBAdapter.KEY_CREATED_DATE, DateHelper.convertDateToString(getCreatedDate()));
        return cv;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Collection<RecordedProgram> getRecordedPrograms() {
        return recordedPrograms;
    }

    public void setRecordedPrograms(Collection<RecordedProgram> recordedPrograms) {
        this.recordedPrograms = recordedPrograms;
    }
}
