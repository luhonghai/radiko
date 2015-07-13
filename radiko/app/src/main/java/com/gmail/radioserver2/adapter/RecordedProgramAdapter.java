package com.gmail.radioserver2.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.radioserver2.view.swipelistview.SwipeListView;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.activity.LibraryPickerActivity;
import com.gmail.radioserver2.data.RecordedProgram;

/**
 * Created by luhonghai on 2/18/15.
 */

public class RecordedProgramAdapter extends DefaultAdapter<RecordedProgram> {

    static class ViewHolder {
        TextView txtTitle;
        Button btnDelete;
        Button btnEdit;
    }

    public RecordedProgramAdapter(Context context, RecordedProgram[] objects, OnListItemActionListener<RecordedProgram> onListItemActionListener) {
        super(context, R.layout.list_item_recorded_program, objects, onListItemActionListener);
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
            holder.btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getListItemAction().onDeleteItem((RecordedProgram) v.getTag());
                }
            });
            holder.txtTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getListItemAction().onSelectItem((RecordedProgram) v.getTag());
                }

            });
            holder.btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getListItemAction().onEditItem((RecordedProgram) v.getTag());
                }
            });
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (parent instanceof SwipeListView) {
            ((SwipeListView)parent).recycle(convertView, position);
        }
        RecordedProgram object = getObjects()[position];
        holder.txtTitle.setText(object.toPrettyString(getContext()));
        holder.txtTitle.setTag(object);
        holder.btnDelete.setTag(object);
        holder.btnEdit.setTag(object);
        return convertView;
    }
}
