package com.gmail.radioserver2.adapter;

import android.content.Context;
import android.widget.ArrayAdapter;

/**
 * Created by luhonghai on 27/2/2015.
 */
public class DefaultAdapter<T> extends ArrayAdapter<T> {

    private final OnListItemActionListener<T> listItemAction;

    private final T[] objects;

    public DefaultAdapter(Context context, int resource, T[] objects, OnListItemActionListener<T> listItemAction) {
        super(context, resource, objects);
        this.listItemAction = listItemAction;
        this.objects = objects;
    }

    public OnListItemActionListener<T> getListItemAction() {
        return listItemAction;
    }

    public T[] getObjects() {
        return objects;
    }
}
