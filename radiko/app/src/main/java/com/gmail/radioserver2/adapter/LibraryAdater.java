package com.gmail.radioserver2.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.gmail.radioserver2.R;
import com.gmail.radioserver2.data.Library;
import com.gmail.radioserver2.view.swipelistview.SwipeListView;

/**
 * Copyright
 */
public class LibraryAdater extends CallBackAdapter<Library> {


    private View.OnClickListener mDeleteClick;
    private View.OnClickListener mSelectClick;
    public LibraryAdater(Context context) {
        super(context);
        initComponent();
    }

    private void initComponent() {
        mDeleteClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getListItemActionListener().onDeleteItem((Library) v.getTag());
            }
        };
        mSelectClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getListItemActionListener().onSelectItem((Library) v.getTag());
            }
        };
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
            ((SwipeListView) parent).recycle(convertView, position);
        }
        Library object = getItem(position);
        holder.txtTitle.setText(object.toPrettyString(getContext()));
        holder.txtTitle.setTag(object);
        holder.btnDelete.setTag(object);
        holder.btnDelete.setOnClickListener(mDeleteClick);
        holder.txtTitle.setOnClickListener(mSelectClick);
        return convertView;
    }

    class ViewHolder {
        TextView txtTitle;
        Button btnDelete;
    }
}
