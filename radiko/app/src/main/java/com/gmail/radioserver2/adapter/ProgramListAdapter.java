package com.gmail.radioserver2.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dotohsoft.radio.data.RadioProgram;
import com.gmail.radioserver2.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class ProgramListAdapter extends BaseAdapter{

    static class ViewHolder {
        TextView txtProgram;
        TextView txtTime;
        View splitLine;
        LinearLayout programLine;
    }

    private Context context;
    private LayoutInflater inflater;
    private List<RadioProgram.Program> objects = new ArrayList<>();

    public void setList(List<RadioProgram.Program> objects) {
        this.objects = objects;
    }

    public ProgramListAdapter(Context context) {
        this.context = context;
        this.inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return objects.size();
    }

    @Override
    public Object getItem(int i) {
        return objects.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.program_item_list, parent, false);
            holder = new ViewHolder();
            holder.txtProgram = (TextView) convertView.findViewById(R.id.txtProgram);
            holder.txtTime = (TextView) convertView.findViewById(R.id.txtTime);
            holder.splitLine = (View) convertView.findViewById(R.id.splitLine);
            holder.programLine = (LinearLayout) convertView.findViewById(R.id.programLine);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        SimpleDateFormat date = new SimpleDateFormat("HH:mm");
        RadioProgram.Program object = (RadioProgram.Program) getItem(position);
        if (object.getFromTime() == 0 && object.getToTime() == 0) {
            holder.txtProgram.setText(object.getTitle());
            holder.txtTime.setText("");
            holder.splitLine.setVisibility(View.INVISIBLE);
            holder.programLine.setBackgroundColor(context.getResources().getColor(android.R.color.white));
        } else {
            holder.splitLine.setVisibility(View.VISIBLE);
            StringBuffer s = new StringBuffer();
            s.append(date.format(new Date(object.getFromTime())))
                    .append(" - ")
                    .append(date.format(new Date(object.getToTime())));
            holder.txtProgram.setText(object.getTitle());
            holder.txtTime.setText(s);
            TimeZone tz = TimeZone.getTimeZone("GMT+9");
            Calendar calNow = Calendar.getInstance(tz);

            Calendar calFrom = Calendar.getInstance(tz);
            calFrom.setTimeInMillis(object.getFromTime());
            Calendar calTo = Calendar.getInstance(tz);
            calTo.setTimeInMillis(object.getToTime());

            if (calNow.getTimeInMillis() >= calFrom.getTimeInMillis() && calNow.getTimeInMillis() <= calTo.getTimeInMillis()) {
                holder.txtProgram.setTextColor(context.getResources().getColor(R.color.text_light_theme));
                holder.txtProgram.setTextAppearance(context, R.style.text_bold);
                holder.txtTime.setTextColor(context.getResources().getColor(R.color.text_light_theme));
                holder.txtTime.setTextAppearance(context, R.style.text_bold);
                holder.programLine.setBackgroundColor(context.getResources().getColor(R.color.default_button_highlight_color));
            } else {
                if (position % 2 == 0) {
                    holder.programLine.setBackgroundResource(R.drawable.table_border_odd_item);
                } else {
                    holder.programLine.setBackgroundResource(R.drawable.table_border_even_item);
                }
                holder.txtProgram.setTextColor(context.getResources().getColor(R.color.text_dark_theme));
                holder.txtProgram.setTextAppearance(context, R.style.text_normal);
                holder.txtTime.setTextColor(context.getResources().getColor(R.color.text_dark_theme));
                holder.txtTime.setTextAppearance(context, R.style.text_normal);
            }
        }
        return convertView;
    }
}
