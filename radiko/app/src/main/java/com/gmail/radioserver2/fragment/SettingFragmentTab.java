package com.gmail.radioserver2.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;
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
import com.gmail.radioserver2.view.CustomSeekBar;
import com.infteh.comboseekbar.ComboSeekBar;

import java.util.Arrays;
import java.util.List;

/**
 * Created by luhonghai on 2/17/15.
 */
public class SettingFragmentTab extends FragmentTab implements View.OnClickListener {

    private static final int MAX_PROCESS = 100;

    private CustomSeekBar seekBarFastLevel;
    private CustomSeekBar seekBarSlowLevel;
    private CustomSeekBar seekBarBackLength;

    private EditText txtChannelName;
    private EditText txtChannelURL;

    private Spinner spinnerTokenType;

    private Switch switchRegion;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_setting, container, false);
        v.findViewById(R.id.btnSave).setOnClickListener(this);
        seekBarFastLevel = (CustomSeekBar) v.findViewById(R.id.seekBarFastLevel);
        seekBarSlowLevel = (CustomSeekBar) v.findViewById(R.id.seekBarSlowLevel);
        seekBarBackLength = (CustomSeekBar) v.findViewById(R.id.seekBarBackLength);
        txtChannelName = (EditText) v.findViewById(R.id.txtChannelName);
        txtChannelURL = (EditText) v.findViewById(R.id.txtChannelURL);
        switchRegion = (Switch) v.findViewById(R.id.switchRegion);
        spinnerTokenType = (Spinner) v.findViewById(R.id.spinnerTokenType);
        v.findViewById(R.id.lblTokenType).setVisibility(getResources().getBoolean(R.bool.is_debug_mode) ? View.VISIBLE : View.GONE);
        v.findViewById(R.id.rlTokenType).setVisibility(getResources().getBoolean(R.bool.is_debug_mode) ? View.VISIBLE : View.GONE);
        initSeekbar();
        loadData();
        return v;
    }

    private void initSeekbar() {
        String[] slowStep = new String[]{"x0.9", "x0.7", "x0.5", "x0.3"};
        String[] fastStep = new String[]{"x1.3", "x1.5", "x1.7", "x2.0"};
        String[] backStep = new String[]{"10s", "7s", "5s", "3s"};
        seekBarSlowLevel.setMax(MAX_PROCESS);
        seekBarSlowLevel.setItems(slowStep);
        seekBarSlowLevel.setOnSeekBarChangeListener(mSeekListener);
        seekBarSlowLevel.invalidate();

        seekBarFastLevel.setMax(MAX_PROCESS);
        seekBarFastLevel.setItems(fastStep);
        seekBarFastLevel.setOnSeekBarChangeListener(mSeekListener);
        seekBarFastLevel.invalidate();

        seekBarBackLength.setMax(MAX_PROCESS);
        seekBarBackLength.setItems(backStep);
        seekBarBackLength.setOnSeekBarChangeListener(mSeekListener);
        seekBarBackLength.invalidate();
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


    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {

        public void onStartTrackingTouch(SeekBar bar) {

        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (fromuser) {
                int pos = progress % (MAX_PROCESS / 3);
                int desPros = (pos > MAX_PROCESS / 6) ? progress + ((MAX_PROCESS / 3) - pos) : progress - pos;
                if (desPros < 0) desPros = 0;
                if (desPros > MAX_PROCESS) desPros = MAX_PROCESS;
                bar.setProgress(desPros);
            }
        }
        public void onStopTrackingTouch(SeekBar bar) {

        }
    };
}
