package com.gmail.radioserver2.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.gmail.radioserver2.R;
import com.gmail.radioserver2.data.Timer;
import com.gmail.radioserver2.view.swipelistview.SwipeListView;

/**
 * Copyright
 */
public class TimerAdapter extends CallBackAdapter<Timer> {
    private View.OnClickListener mSelectClick;
    private View.OnClickListener mDeleteClick;

    public TimerAdapter(Context context) {
        super(context);
        initComponent();
    }

    private void initComponent() {
        mSelectClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getListItemActionListener().onSelectItem((Timer) v.getTag());
            }
        };
        mDeleteClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getListItemActionListener().onDeleteItem((Timer) v.getTag());
            }
        };
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = li.inflate(R.layout.list_item_timer, parent, false);
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
        Timer timer = getItem(position);
        holder.txtTitle.setText(timer.toPrettyString(getContext()));
        holder.txtTitle.setTag(timer);
        holder.btnDelete.setTag(timer);
        holder.txtTitle.setOnClickListener(mSelectClick);
        holder.btnDelete.setOnClickListener(mDeleteClick);
        return convertView;
    }

    class ViewHolder {
        TextView txtTitle;
        Button btnDelete;
    }
}
