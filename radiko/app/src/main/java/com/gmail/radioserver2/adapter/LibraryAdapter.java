package com.gmail.radioserver2.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.radioserver2.data.Library;
import com.gmail.radioserver2.view.swipelistview.SwipeListView;
import com.gmail.radioserver2.R;

/**
 * Created by luhonghai on 2/17/15.
 */
public class LibraryAdapter extends DefaultAdapter<Library> {
    static class ViewHolder {
        TextView txtTitle;
        Button btnDelete;
    }

    public LibraryAdapter(Context context, Library[] objects, OnListItemActionListener<Library> onListItemActionListener) {
        super(context, R.layout.list_item_library, objects, onListItemActionListener);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = li.inflate(R.layout.list_item_library, parent, false);
            holder = new ViewHolder();
            holder.txtTitle = (TextView) convertView.findViewById(R.id.txtTitle);
            holder.btnDelete = (Button) convertView.findViewById(R.id.btnDelete);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (parent instanceof SwipeListView) {
            ((SwipeListView)parent).recycle(convertView, position);
        }
        Library object = getObjects()[position];
        holder.txtTitle.setText(object.toString());
        holder.txtTitle.setTag(object);
        holder.txtTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), getContext().getResources().getString(R.string.debug_select,v.getTag()), Toast.LENGTH_SHORT).show();
            }
        });
        holder.btnDelete.setTag(object);
        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), getContext().getResources().getString(R.string.debug_delete,v.getTag()), Toast.LENGTH_SHORT).show();
            }
        });
        return convertView;
    }
}
