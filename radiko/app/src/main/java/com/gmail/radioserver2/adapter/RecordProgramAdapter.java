package com.gmail.radioserver2.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.gmail.radioserver2.R;
import com.gmail.radioserver2.data.RecordedProgram;
import com.gmail.radioserver2.view.swipelistview.SwipeListView;

/**
 * Copyright
 */
public class RecordProgramAdapter extends CallBackAdapter<RecordedProgram> {
    private View.OnClickListener mSelectClick;
    private View.OnClickListener mDeleteClick;
    private View.OnClickListener mEditClick;

    public RecordProgramAdapter(Context context) {
        super(context);
        initComponent();
    }

    private void initComponent() {
        mSelectClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getListItemActionListener().onSelectItem((RecordedProgram) v.getTag());
            }
        };
        mEditClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getListItemActionListener().onEditItem((RecordedProgram) v.getTag());
            }
        };
        mDeleteClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getListItemActionListener().onDeleteItem((RecordedProgram) v.getTag());
            }
        };
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = li.inflate(R.layout.list_item_recorded_program, parent, false);
            holder = new ViewHolder();
            holder.txtTitle = (TextView) convertView.findViewById(R.id.txtTitle);
            holder.btnDelete = (Button) convertView.findViewById(R.id.btnDelete);
            holder.btnEdit = (Button) convertView.findViewById(R.id.btnEdit);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (parent instanceof SwipeListView) {
            ((SwipeListView) parent).recycle(convertView, position);
        }
        RecordedProgram object = getItem(position);
        holder.txtTitle.setText(object.toPrettyString(getContext()));
        holder.txtTitle.setTag(object);
        holder.btnDelete.setTag(object);
        holder.btnEdit.setTag(object);
        holder.btnDelete.setOnClickListener(mDeleteClick);
        holder.txtTitle.setOnClickListener(mSelectClick);
        holder.btnEdit.setOnClickListener(mEditClick);
        return convertView;
    }

    class ViewHolder {
        TextView txtTitle;
        Button btnDelete;
        Button btnEdit;
    }
}
