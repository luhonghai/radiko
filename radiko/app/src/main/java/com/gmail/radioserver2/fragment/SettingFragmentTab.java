package com.gmail.radioserver2.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;

import com.gmail.radioserver2.R;
import com.gmail.radioserver2.data.Channel;
import com.gmail.radioserver2.data.Setting;
import com.gmail.radioserver2.data.sqlite.ext.ChannelDBAdapter;
import com.gmail.radioserver2.utils.Constants;
import com.gmail.radioserver2.utils.SimpleAppLog;

/**
 * Created by luhonghai on 2/17/15.
 */
public class SettingFragmentTab extends FragmentTab implements View.OnClickListener {

    private SeekBar seekBarFastLevel;
    private SeekBar seekBarSlowLevel;
    private SeekBar seekBarBackLength;

    private EditText txtChannelName;
    private EditText txtChannelURL;

    private Spinner spinnerTokenType;

    private Switch switchRegion;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_setting, container, false);
        v.findViewById(R.id.btnSave).setOnClickListener(this);
        seekBarFastLevel = (SeekBar) v.findViewById(R.id.seekBarFastLevel);
        seekBarSlowLevel = (SeekBar) v.findViewById(R.id.seekBarSlowLevel);
        seekBarBackLength = (SeekBar) v.findViewById(R.id.seekBarBackLength);
        txtChannelName = (EditText) v.findViewById(R.id.txtChannelName);
        txtChannelURL = (EditText) v.findViewById(R.id.txtChannelURL);
        switchRegion = (Switch) v.findViewById(R.id.switchRegion);
        spinnerTokenType = (Spinner) v.findViewById(R.id.spinnerTokenType);
        v.findViewById(R.id.lblTokenType).setVisibility(getResources().getBoolean(R.bool.is_debug_mode) ? View.VISIBLE : View.GONE);
        v.findViewById(R.id.rlTokenType).setVisibility(getResources().getBoolean(R.bool.is_debug_mode) ? View.VISIBLE : View.GONE);
        loadData();
        return v;
    }

    private void loadData() {
        Setting setting = new Setting(getActivity());
        setting.load();
        seekBarFastLevel.setProgress(setting.getFastLevelPercent());
        seekBarSlowLevel.setProgress(setting.getSlowLevelPercent());
        seekBarBackLength.setProgress(setting.getBackLengthPercent());
        switchRegion.setChecked(setting.isRegion());
        spinnerTokenType.setSelection(setting.getTokenType());
    }

    private void saveData() {
        Setting setting = new Setting(getActivity());
        setting.applyFastLevel(seekBarFastLevel.getProgress());
        setting.applySlowLevel(seekBarSlowLevel.getProgress());
        setting.applyBackLength(seekBarBackLength.getProgress());
        setting.setRegion(switchRegion.isChecked());
        setting.setTokenType(spinnerTokenType.getSelectedItemPosition());
        setting.save();
        String channelName = txtChannelName.getText().toString().trim();
        String channelURL = txtChannelURL.getText().toString().trim();
        // Remove old data
        txtChannelName.setText("");
        txtChannelURL.setText("");
        if (channelName.length() > 0 && channelURL.length() > 0) {
            ChannelDBAdapter adapter = new ChannelDBAdapter(getActivity());
            try {
                adapter.open();
                Channel channel = new Channel();
                channel.setName(channelName);
                channel.setUrl(channelURL);
                adapter.insert(channel);
            } catch (Exception ex) {
                SimpleAppLog.error("Could not save channel", ex);
            } finally {
                adapter.close();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        seekBarBackLength = null;
        seekBarFastLevel = null;
        seekBarSlowLevel = null;
        switchRegion = null;
        txtChannelName = null;
        txtChannelURL = null;
        spinnerTokenType = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSave:
                saveData();
                Intent intent = new Intent(Constants.INTENT_FILTER_FRAGMENT_ACTION);
                intent.putExtra(Constants.FRAGMENT_ACTION_TYPE, Constants.ACTION_CALL_SELECT_TAB);
                intent.putExtra(Constants.PARAMETER_SELECTED_TAB_ID, Constants.TAB_HOME_ID);
                getActivity().sendBroadcast(intent);
                break;
        }
    }
}
