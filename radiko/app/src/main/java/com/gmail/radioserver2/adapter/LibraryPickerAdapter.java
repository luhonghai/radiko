package com.gmail.radioserver2.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;

import com.gmail.radioserver2.data.Library;
import com.gmail.radioserver2.view.swipelistview.SwipeListView;
import com.gmail.radioserver2.R;

import java.util.List;

/**
 * Created by luhonghai on 2/17/15.
 */
public class LibraryPickerAdapter extends DefaultAdapter<Library> {

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        Library seLibrary = getObjects()[selectedIndex];
        if (selectedItems.contains(seLibrary)) {
            selectedItems.remove(seLibrary);
        } else {
            selectedItems.add(seLibrary);
        }
        getListItemAction().onSelectItem(seLibrary);
        getListItemAction().onSelectIndex(selectedIndex);
        this.selectedIndex = selectedIndex;
    }

    static class ViewHolder {
        TextView txtTitle;
        Button btnDelete;
        CheckBox cbxSelectLib;
    }

    private int selectedIndex = -1;

    private final List<Library> selectedItems;

    public LibraryPickerAdapter(Context context,
                                    Library[] objects,
                                    List<Library> selectedItems,
                                    OnListItemActionListener<Library> onListItemActionListener) {
        super(context, R.layout.list_item_library_picker, objects, onListItemActionListener);
        this.selectedItems = selectedItems;
    }

    public List<Library> getSelectedItems() {
        return selectedItems;
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
            holder.cbxSelectLib = (CheckBox) convertView.findViewById(R.id.cbxSelectLib);

            holder.txtTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setSelectedIndex((Integer) v.getTag());
                    notifyDataSetInvalidated();
                }
            });
            holder.cbxSelectLib.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setSelectedIndex((Integer) v.getTag());
                    notifyDataSetInvalidated();
                }
            });
            holder.btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Library lib = (Library) v.getTag();
                    if (selectedItems.contains(lib)) {
                        selectedItems.remove(lib);
                    }
                    getListItemAction().onDeleteItem(lib);
                }
            });


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

        holder.btnDelete.setTag(object);

        holder.cbxSelectLib.setChecked(selectedItems.contains(object));
        holder.cbxSelectLib.setTag(position);

        return convertView;
    }
}
