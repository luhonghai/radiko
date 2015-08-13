package com.gmail.radioserver2.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.radioserver2.data.Channel;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.gmail.radioserver2.view.swipelistview.SwipeListView;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.utils.Constants;

/**
 * Created by luhonghai on 2/17/15.
 */
public class ChannelAdapter extends DefaultAdapter<Channel> {

    static class ViewHolder {
        TextView txtTitle;
        Button btnDelete;
    }

    public ChannelAdapter(Context context, Channel[] objects, OnListItemActionListener<Channel> listItemActionListener) {
        super(context, R.layout.list_item_chanel, objects, listItemActionListener);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = li.inflate(R.layout.list_item_chanel, parent, false);
            holder = new ViewHolder();
            holder.txtTitle = (TextView) convertView.findViewById(R.id.txtChannelTitle);
            holder.btnDelete = (Button) convertView.findViewById(R.id.btnDelete);
            holder.txtTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SimpleAppLog.info("click channel row");
                    getListItemAction().onSelectItem((Channel) v.getTag());
                }
            });
            holder.btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getListItemAction().onDeleteItem((Channel) v.getTag());
                }
            });

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (parent instanceof SwipeListView) {
            ((SwipeListView) parent).recycle(convertView, position);
        }
        Channel channel = getObjects()[position];
        holder.txtTitle.setText(channel.toPrettyString(getContext()));
        holder.txtTitle.setTag(channel);
        holder.btnDelete.setTag(channel);
        return convertView;
    }
}
