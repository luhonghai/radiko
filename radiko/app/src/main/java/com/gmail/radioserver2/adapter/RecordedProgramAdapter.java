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

import com.fortysevendeg.swipelistview.SwipeListView;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.activity.LibraryPickerActivity;

/**
 * Created by luhonghai on 2/18/15.
 */
public class RecordedProgramAdapter extends ArrayAdapter<String> {
    static class ViewHolder {
        TextView txtTitle;
        Button btnDelete;
        Button btnEdit;
    }

    private Context mContext;

    private String[] objects;

    public RecordedProgramAdapter(Context context, String[] objects) {
        super(context, R.layout.list_item_recorded_program, objects);
        mContext = context;
        this.objects = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
            ((SwipeListView)parent).recycle(convertView, position);
        }
        String channel = objects[position];
        holder.txtTitle.setText(objects[position]);
        holder.txtTitle.setTag(channel);
        holder.txtTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, mContext.getResources().getString(R.string.debug_select,v.getTag()), Toast.LENGTH_SHORT).show();
            }
        });
        holder.btnDelete.setTag(channel);
        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, mContext.getResources().getString(R.string.debug_delete,v.getTag()), Toast.LENGTH_SHORT).show();
            }
        });
        holder.btnEdit.setTag(channel);
        holder.btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(mContext, LibraryPickerActivity.class);
                mContext.startActivity(intent);
            }
        });
        return convertView;
    }
}