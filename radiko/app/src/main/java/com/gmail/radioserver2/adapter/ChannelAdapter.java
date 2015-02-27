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
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (parent instanceof SwipeListView) {
            ((SwipeListView)parent).recycle(convertView, position);
        }
        Channel channel = getObjects()[position];
        holder.txtTitle.setText(channel.toString());
        holder.txtTitle.setTag(channel);
        holder.txtTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(mContext, "Select chanel " + v.getTag(), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Constants.INTENT_FILTER_FRAGMENT_ACTION);
                intent.putExtra(Constants.FRAGMENT_ACTION_TYPE, Constants.ACTION_SELECT_CHANNEL_ITEM);
                getContext().sendBroadcast(intent);
            }
        });
        holder.btnDelete.setTag(channel);
        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), getContext().getResources().getString(R.string.debug_delete,v.getTag()), Toast.LENGTH_SHORT).show();
            }
        });
        return convertView;
    }
}
