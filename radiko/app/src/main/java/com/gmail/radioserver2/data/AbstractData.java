package com.gmail.radioserver2.data;

import android.content.ContentValues;
import android.content.Context;

import java.util.Date;

/**
 * Created by luhonghai on 25/02/2015.
 */
public abstract class AbstractData<T> implements Indexable {

    private long id;

    private Date createdDate;

    public abstract String toPrettyString(Context context);

    public abstract ContentValues toContentValues();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public long getUniqueID() {
        return getId();
    }
}
