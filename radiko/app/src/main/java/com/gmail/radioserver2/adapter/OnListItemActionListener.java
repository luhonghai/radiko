package com.gmail.radioserver2.adapter;

import android.util.SparseArray;

import java.util.Map;

/**
 * Created by luhonghai on 27/2/2015.
 */
public interface OnListItemActionListener<T> {

    public void onDeleteItem(T obj);

    public void onSelectItem(T obj);

    public void onEditItem(T obj);

    void onSelectItems(SparseArray<T> items);
}
