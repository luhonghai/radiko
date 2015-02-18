package com.gmail.radioserver2.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gmail.radioserver2.R;
import com.gmail.radioserver2.utils.Constants;

/**
 * Created by luhonghai on 2/16/15.
 */
public class PlayerFragmentTab extends FragmentTab {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_player_tab, container, false);
        v.findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Constants.INTENT_FILTER_FRAGMENT_ACTION);
                intent.putExtra(Constants.FRAGMENT_ACTION_TYPE, Constants.ACTION_CLICK_BACK_PLAYER);
                getActivity().sendBroadcast(intent);
            }
        });
        return v;
    }
}
