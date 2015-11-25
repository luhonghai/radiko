package com.gmail.radioserver2.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.gmail.radioserver2.R;
import com.gmail.radioserver2.data.Channel;

/**
 * Copyright
 */
public class ChannelAdapter extends CallBackAdapter<Channel> {
    private View.OnClickListener mSelectClick;
    private View.OnClickListener mDeleteClick;

    public ChannelAdapter(Context context) {
        super(context);
        initComponent();
    }

    private void initComponent() {
        mSelectClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getListItemActionListener().onSelectItem((Channel) v.getTag());
            }
        };
        mDeleteClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getListItemActionListener().onDeleteItem((Channel) v.getTag());
            }
        };
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_chanel, parent, false);
            holder.txtTitle = (TextView) convertView.findViewById(R.id.txtChannelTitle);
            holder.btnDelete = (Button) convertView.findViewById(R.id.btnDelete);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Channel channel = getItem(position);
        holder.txtTitle.setText(channel.toPrettyString(getContext()));
        holder.txtTitle.setTag(channel);
        holder.btnDelete.setTag(channel);
        holder.txtTitle.setOnClickListener(mSelectClick);
        holder.btnDelete.setOnClickListener(mDeleteClick);
        return convertView;
    }


    class ViewHolder {
        TextView txtTitle;
        Button btnDelete;
    }
}
