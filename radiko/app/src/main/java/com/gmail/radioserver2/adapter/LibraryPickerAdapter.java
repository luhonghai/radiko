package com.gmail.radioserver2.adapter;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.gmail.radioserver2.R;
import com.gmail.radioserver2.data.Library;
import com.gmail.radioserver2.data.RecordedProgram;
import com.gmail.radioserver2.view.swipelistview.SwipeListView;

/**
 * Copyright
 */
public class LibraryPickerAdapter extends CallBackAdapter<Library> {

    private View.OnClickListener mSelectClick;
    private View.OnClickListener mDeleteClick;
    private long mSelectedID;

    public LibraryPickerAdapter(Context context) {
        super(context);
        initComponent();
    }

    private void initComponent() {
        mSelectedID = -1;
        mSelectClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLastSelected((Long) v.getTag());
            }
        };

        mDeleteClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long itemID = (long) v.getTag();
                if (itemID == mSelectedID) {
                    mSelectedID = -1;
                }
                removeItemAtID(itemID);
            }
        };
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = li.inflate(R.layout.list_item_library_picker, parent, false);
            holder = new ViewHolder();
            holder.txtTitle = (TextView) convertView.findViewById(R.id.txtTitle);
            holder.btnDelete = (Button) convertView.findViewById(R.id.btnDelete);
            holder.cbxSelectLib = (CheckBox) convertView.findViewById(R.id.cbxSelectLib);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (parent instanceof SwipeListView) {
            ((SwipeListView) parent).recycle(convertView, position);
        }
        if (mSelectedID == -1) {
            mSelectedID = getItemId(0);
        }
        Library object = getItem(position);
        holder.txtTitle.setText(object.toPrettyString(getContext()));
        holder.txtTitle.setTag(object.getId());
        holder.btnDelete.setTag(object.getId());
        holder.cbxSelectLib.setTag(object.getId());
        holder.cbxSelectLib.setChecked(mSelectedID == getItemId(position));
        holder.txtTitle.setOnClickListener(mSelectClick);
        holder.cbxSelectLib.setOnClickListener(mSelectClick);
        holder.btnDelete.setOnClickListener(mDeleteClick);
        return convertView;
    }

    public void setLastSelected(long selected) {
        mSelectedID = selected;
        notifyDataSetChanged();
    }

    @Nullable
    public Library getLastSelected() {
        return getItemWidthID(mSelectedID);
    }

    class ViewHolder {
        TextView txtTitle;
        Button btnDelete;
        CheckBox cbxSelectLib;
    }
}
