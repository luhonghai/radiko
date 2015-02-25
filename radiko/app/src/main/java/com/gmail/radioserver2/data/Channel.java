package com.gmail.radioserver2.data;

import android.content.ContentValues;

/**
 * Created by luhonghai on 25/02/2015.
 */

public class Channel extends AbstractData<Channel> {

    private String name;

    private String key;

    private String type;

    private String url;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public ContentValues toContentValues() {
        return null;
    }
}
