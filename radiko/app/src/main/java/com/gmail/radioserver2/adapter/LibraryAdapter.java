package com.gmail.radioserver2.adapter;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.gmail.radioserver2.R;
import com.gmail.radioserver2.data.Library;
import com.gmail.radioserver2.view.swipelistview.SwipeListView;

/**
 * Copyright
 */
public class LibraryAdapter extends CallBackAdapter<Library> {


    private View.OnClickListener mDeleteClick;
    private View.OnClickListener mSelectClick;
    private View.OnClickListener mCheckClick;

    public LibraryAdapter(Context context) {
        super(context);
        initComponent();
    }

    private void initComponent() {
        mSelected = new SparseArray<>();
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
        mCheckClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox cb = (CheckBox) v;
                Library library = (Library) v.getTag();
                if (cb.isChecked()) {
                    addToSelected(library);
                } else {
                    removeFromSelected(library);
                }
            }
        };
    }

    private SparseArray<Library> mSelected;

    public void resetSelected() {
        mSelected.clear();
        getListItemActionListener().onSelectItems(mSelected);
    }

    public void addToSelected(Library library) {
        mSelected.append((int) library.getUniqueID(), library);
        notifyDataSetChanged();
        getListItemActionListener().onSelectItems(mSelected);
    }

    public void removeFromSelected(Library library) {
        mSelected.remove((int) library.getUniqueID());
        notifyDataSetChanged();
        getListItemActionListener().onSelectItems(mSelected);
    }

    public SparseArray<Library> getSelected() {
        return mSelected;
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
            holder.cbCheck = (CheckBox) convertView.findViewById(R.id.cbCheck);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (parent instanceof SwipeListView) {
            ((SwipeListView) parent).recycle(convertView, position);
        }
        Library object = getItem(position);
        holder.txtTitle.setText(object.toPrettyString(getContext()));
        holder.cbCheck.setChecked(mSelected.indexOfKey((int) object.getUniqueID()) >= 0);
        holder.txtTitle.setTag(object);
        holder.btnDelete.setTag(object);
        holder.cbCheck.setTag(object);
        holder.btnDelete.setOnClickListener(mDeleteClick);
        holder.txtTitle.setOnClickListener(mSelectClick);
        holder.cbCheck.setOnClickListener(mCheckClick);
        return convertView;
    }

    class ViewHolder {
        CheckBox cbCheck;
        TextView txtTitle;
        Button btnDelete;
    }
}
