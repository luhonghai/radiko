package com.gmail.radioserver2.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.radioserver2.data.Timer;
import com.gmail.radioserver2.view.swipelistview.SwipeListView;
import com.gmail.radioserver2.R;

/**
 * Created by luhonghai on 2/17/15.
 */
public class TimerAdapter extends DefaultAdapter<Timer> {
    static class ViewHolder {
        TextView txtTitle;
        Button btnDelete;
    }

    public TimerAdapter(Context context, Timer[] objects, OnListItemActionListener<Timer> onListItemActionListener) {
        super(context, R.layout.list_item_timer, objects, onListItemActionListener);
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
            holder.txtTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getListItemAction().onSelectItem((Timer) v.getTag());
                }
            });
            holder.btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getListItemAction().onDeleteItem((Timer) v.getTag());
                }
            });
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (parent instanceof SwipeListView) {
            ((SwipeListView)parent).recycle(convertView, position);
        }
        Timer timer = getObjects()[position];
        holder.txtTitle.setText(timer.toPrettyString(getContext()));
        holder.txtTitle.setTag(timer);

        holder.btnDelete.setTag(timer);

        return convertView;
    }
}
