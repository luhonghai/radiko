package com.gmail.radioserver2.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.fortysevendeg.swipelistview.SwipeListView;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.utils.Constants;

/**
 * Created by luhonghai on 2/17/15.
 */
public class ChannelAdapter extends ArrayAdapter<String> {

    static class ViewHolder {
        TextView txtTitle;
        Button btnDelete;
    }

    private Context mContext;

    private String[] objects;

    public ChannelAdapter(Context context, String[] objects) {
        super(context, R.layout.list_item_chanel, objects);
        mContext = context;
        this.objects = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
        String channel = objects[position];
        holder.txtTitle.setText(objects[position]);
        holder.txtTitle.setTag(channel);
        holder.txtTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(mContext, "Select chanel " + v.getTag(), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Constants.INTENT_FILTER_FRAGMENT_ACTION);
                intent.putExtra(Constants.FRAGMENT_ACTION_TYPE, Constants.ACTION_SELECT_CHANNEL_ITEM);
                mContext.sendBroadcast(intent);
            }
        });
        holder.btnDelete.setTag(channel);
        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, "Call delete chanel " + v.getTag(), Toast.LENGTH_SHORT).show();
            }
        });
        return convertView;
    }
}
