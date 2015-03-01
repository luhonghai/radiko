package com.gmail.radioserver2.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import com.gmail.radioserver2.data.Library;
import com.gmail.radioserver2.view.swipelistview.SwipeListView;
import com.gmail.radioserver2.R;

/**
 * Created by luhonghai on 2/17/15.
 */
public class LibraryPickerAdapter extends DefaultAdapter<Library> {

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
    }

    static class ViewHolder {
        TextView txtTitle;
        Button btnDelete;
        RadioButton rdSelect;
    }

    private int selectedIndex = 0;

    public LibraryPickerAdapter(Context context, Library[] objects, OnListItemActionListener<Library> onListItemActionListener) {
        super(context, R.layout.list_item_library_picker, objects, onListItemActionListener);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = li.inflate(R.layout.list_item_library_picker, parent, false);
            holder = new ViewHolder();
            holder.txtTitle = (TextView) convertView.findViewById(R.id.txtTitle);
            holder.btnDelete = (Button) convertView.findViewById(R.id.btnDelete);
            holder.rdSelect = (RadioButton) convertView.findViewById(R.id.rdSelect);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (parent instanceof SwipeListView) {
            ((SwipeListView)parent).recycle(convertView, position);
        }
        Library object = getObjects()[position];
        holder.txtTitle.setText(object.toPrettyString(getContext()));
        holder.txtTitle.setTag(position);
        holder.txtTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedIndex((Integer) v.getTag());
                notifyDataSetInvalidated();
                getListItemAction().onSelectItem(getObjects()[getSelectedIndex()]);
                getListItemAction().onSelectIndex(getSelectedIndex());
            }
        });
        holder.btnDelete.setTag(object);
        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getListItemAction().onDeleteItem((Library) v.getTag());
            }
        });
        holder.rdSelect.setChecked(position == getSelectedIndex());
        holder.rdSelect.setTag(position);
        holder.rdSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedIndex((Integer) v.getTag());
                notifyDataSetInvalidated();
                getListItemAction().onSelectItem(getObjects()[getSelectedIndex()]);
                getListItemAction().onSelectIndex(getSelectedIndex());
            }
        });

        return convertView;
    }
}
