package com.gmail.radioserver2.adapter;

import android.content.Context;
import android.support.annotation.Nullable;
import android.widget.BaseAdapter;

import com.gmail.radioserver2.data.Indexable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Copyright
 */
public abstract class CallBackAdapter<T extends Indexable> extends BaseAdapter {

    private Context mContext;
    private ArrayList<T> mDataList;
    private OnListItemActionListener<T> onListItemActionListener;

    public CallBackAdapter(Context context) {
        this.mContext = context;
        mDataList = new ArrayList<>();
    }

    public Context getContext() {
        return mContext;
    }

    public ArrayList<T> getDataList() {
        return mDataList;
    }

    public void addItem(T item) {
        mDataList.add(item);
        notifyDataSetChanged();
    }

    public void addItems(Collection<T> list) {
        mDataList.addAll(list);
        notifyDataSetChanged();
    }

    public void setDataList(Collection<T> list) {
        mDataList.clear();
        addItems(list);
    }

    public void removeItem(T item) {
        mDataList.remove(item);
        notifyDataSetChanged();
    }

    public void removeItem(int pos) {
        mDataList.remove(pos);
        notifyDataSetChanged();
    }

    public void removeItemAtID(long id) {
        int index = indexOf(id);
        if (index >= 0) {
            removeItem(index);
        }
    }

    public void clear() {
        mDataList.clear();
        notifyDataSetChanged();
    }

    public OnListItemActionListener<T> getListItemActionListener() {
        return onListItemActionListener;
    }

    public void setOnListItemActionListener(OnListItemActionListener<T> onListItemActionListener) {
        this.onListItemActionListener = onListItemActionListener;
    }

    @Override
    public int getCount() {
        return mDataList.size();
    }

    @Override
    public T getItem(int position) {
        return mDataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mDataList.get(position).getUniqueID();
    }

    @Nullable
    public T getItemWidthID(long id) {
        int index = indexOf(id);
        if (index >= 0) {
            return getItem(index);
        }
        return null;
    }

    public int indexOf(Object o) {
        int index = -1;
        for (int i = 0; i < mDataList.size(); i++) {
            T item = mDataList.get(i);
            if (equal(item, o)) {
                index = i;
                break;
            }
        }
        return index;
    }

    private boolean equal(T item, Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof Indexable) {
            return ((Indexable) o).getUniqueID() == item.getUniqueID();
        }
        return o instanceof Number && ((Number) o).longValue() == item.getUniqueID();
    }
}
