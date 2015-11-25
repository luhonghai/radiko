package com.gmail.radioserver2.adapter;

import java.util.Map;

/**
 * Created by luhonghai on 27/2/2015.
 */
public interface OnListItemActionListener<T> {

    public void onDeleteItem(T obj);

    public void onSelectItem(T obj);

    public void onEditItem(T obj);

    public void onSelectIndex(int index);
}
