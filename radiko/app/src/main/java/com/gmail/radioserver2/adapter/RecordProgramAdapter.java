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
import com.gmail.radioserver2.data.RecordedProgram;
import com.gmail.radioserver2.view.swipelistview.SwipeListView;

import java.util.HashMap;
import java.util.Map;

/**
 * Copyright
 */
public class RecordProgramAdapter extends CallBackAdapter<RecordedProgram> {
    private View.OnClickListener mSelectClick;
    private View.OnClickListener mDeleteClick;
    private View.OnClickListener mEditClick;
    private View.OnClickListener mCheckClick;

    public RecordProgramAdapter(Context context) {
        super(context);
        initComponent();
    }

    private void initComponent() {
        mSelected = new SparseArray<>();
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
        mCheckClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox cb = (CheckBox) v;
                RecordedProgram recordedProgram = (RecordedProgram) v.getTag();
                if (cb.isChecked()) {
                    addToSelected(recordedProgram);
                } else {
                    removeFromSelected(recordedProgram);
                }
            }
        };
    }

    private SparseArray<RecordedProgram> mSelected;

    public void resetSelected() {
        mSelected.clear();
        getListItemActionListener().onSelectItems(mSelected);
    }

    public void addToSelected(RecordedProgram recordedProgram) {
        mSelected.append((int) recordedProgram.getUniqueID(), recordedProgram);
        notifyDataSetChanged();
        getListItemActionListener().onSelectItems(mSelected);
    }

    public void removeFromSelected(RecordedProgram recordedProgram) {
        mSelected.remove((int) recordedProgram.getUniqueID());
        notifyDataSetChanged();
        getListItemActionListener().onSelectItems(mSelected);
    }

    public SparseArray<RecordedProgram> getSelected() {
        return mSelected;
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
            holder.cbCheck = (CheckBox) convertView.findViewById(R.id.cbCheck);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (parent instanceof SwipeListView) {
            ((SwipeListView) parent).recycle(convertView, position);
        }
        RecordedProgram object = getItem(position);
        holder.txtTitle.setText(object.toPrettyString(getContext()));
        holder.cbCheck.setChecked(mSelected.indexOfKey((int) object.getUniqueID()) >= 0);
        holder.txtTitle.setTag(object);
        holder.btnDelete.setTag(object);
        holder.btnEdit.setTag(object);
        holder.cbCheck.setTag(object);
        holder.btnDelete.setOnClickListener(mDeleteClick);
        holder.txtTitle.setOnClickListener(mSelectClick);
        holder.btnEdit.setOnClickListener(mEditClick);
        holder.cbCheck.setOnClickListener(mCheckClick);
        return convertView;
    }

    class ViewHolder {
        TextView txtTitle;
        Button btnDelete;
        Button btnEdit;
        CheckBox cbCheck;
    }
}
