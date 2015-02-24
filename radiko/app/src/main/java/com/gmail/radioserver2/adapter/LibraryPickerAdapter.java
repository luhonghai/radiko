package com.gmail.radioserver2.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.fortysevendeg.swipelistview.SwipeListView;
import com.gmail.radioserver2.R;

/**
 * Created by luhonghai on 2/17/15.
 */
public class LibraryPickerAdapter extends ArrayAdapter<String> {

    public OnRadioItemSelectListener getRadioItemSelectedListener() {
        return radioItemSelectedListener;
    }

    public void setRadioItemSelectedListener(OnRadioItemSelectListener radioItemSelectedListener) {
        this.radioItemSelectedListener = radioItemSelectedListener;
    }

    public interface OnRadioItemSelectListener {
        public void onRadioItemSelected(int position);
    }

    static class ViewHolder {
        TextView txtTitle;
        Button btnDelete;
        RadioButton rdSelect;
    }

    private Context mContext;

    private String[] objects;

    private int selectedIndex = 0;

    private OnRadioItemSelectListener radioItemSelectedListener;

    public LibraryPickerAdapter(Context context, String[] objects) {
        super(context, R.layout.list_item_library_picker, objects);
        mContext = context;
        this.objects = objects;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = li.inflate(R.layout.list_item_library_picker, parent, false);
            holder = new ViewHolder();
            holder.txtTitle = (TextView) convertView.findViewById(R.id.txtTitle);
            holder.btnDelete = (Button) convertView.findViewById(R.id.btnDelete);
            holder.rdSelect = (RadioButton) convertView.findViewById(R.id.rdSelect);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (parent instanceof SwipeListView) {
            ((SwipeListView)parent).recycle(convertView, position);
        }
        String channel = objects[position];
        holder.txtTitle.setText(objects[position]);
        holder.txtTitle.setTag(position);
        holder.txtTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedIndex =(Integer) v.getTag();
                notifyDataSetInvalidated();
                if (radioItemSelectedListener != null) {
                    radioItemSelectedListener.onRadioItemSelected(selectedIndex);
                }
            }
        });
        holder.btnDelete.setTag(channel);
        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, mContext.getResources().getString(R.string.debug_delete,v.getTag()), Toast.LENGTH_SHORT).show();
            }
        });
        holder.rdSelect.setChecked(position == selectedIndex);
        holder.rdSelect.setTag(position);
        holder.rdSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedIndex =(Integer) v.getTag();
                notifyDataSetInvalidated();
                if (radioItemSelectedListener != null) {
                    radioItemSelectedListener.onRadioItemSelected(selectedIndex);
                }
            }
        });

        return convertView;
    }
}