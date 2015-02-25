package com.gmail.radioserver2.data;

import android.content.ContentValues;

import java.util.Date;

/**
 * Created by luhonghai on 25/02/2015.
 */
public class Library extends AbstractData<Library> {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public ContentValues toContentValues() {
        return null;
    }
}
