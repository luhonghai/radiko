package com.gmail.radioserver2.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.gmail.radioserver2.R;

/**
 * Created by luhonghai on 2/17/15.
 *
 * Base fragment tab
 */
public class FragmentTab extends SherlockFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_layout, container, false);
        TextView tv = (TextView) v.findViewById(R.id.text);
        tv.setText(this.getTag() + " content.");
        return v;
    }
}
