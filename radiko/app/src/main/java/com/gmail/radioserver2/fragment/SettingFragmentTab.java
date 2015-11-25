package com.gmail.radioserver2.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.activity.MainActivity;
import com.gmail.radioserver2.data.Channel;
import com.gmail.radioserver2.data.Setting;
import com.gmail.radioserver2.data.sqlite.ext.ChannelDBAdapter;
import com.gmail.radioserver2.utils.Constants;
import com.gmail.radioserver2.utils.SimpleAppLog;
import com.gmail.radioserver2.view.CustomSeekBar;
import com.gmail.radioserver2.view.CustomSubSeekBar;

import java.util.Date;

/**
 * Created by luhonghai on 2/17/15.
 */
public class SettingFragmentTab extends FragmentTab implements View.OnClickListener {

    private static final int MAX_PROCESS = 100;

    private CustomSeekBar seekBarFastLevel;
    private CustomSubSeekBar subSeekBarFastLevel;

    private CustomSeekBar seekBarSlowLevel;
    private CustomSubSeekBar subSeekBarSlowLevel;

    private CustomSeekBar seekBarBackLength;
    private CustomSubSeekBar subSeekBarLength;

    private CustomSeekBar seekBarDefaultVolume;
    private CustomSubSeekBar subSeekBarDefaultVolume;

    private EditText txtChannelName;
    private EditText txtChannelURL;
//    private EditText txtRadikoUser;
//    private EditText txtRadikoPassword;
//    private TextView btLogInLogOutPremium;

    private Spinner spinnerTokenType;

    private Switch switchRegion;

    private TextView btLoginFacebook;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_setting, container, false);
        v.findViewById(R.id.btnSave).setOnClickListener(this);
        seekBarFastLevel = (CustomSeekBar) v.findViewById(R.id.seekBarFastLevel);
        seekBarSlowLevel = (CustomSeekBar) v.findViewById(R.id.seekBarSlowLevel);
        seekBarBackLength = (CustomSeekBar) v.findViewById(R.id.seekBarBackLength);

        subSeekBarSlowLevel = (CustomSubSeekBar) v.findViewById(R.id.subSeekBarSlowLevel);
        subSeekBarFastLevel = (CustomSubSeekBar) v.findViewById(R.id.subSeekBarFastLevel);
        subSeekBarLength = (CustomSubSeekBar) v.findViewById(R.id.subSeekBarBackLength);

        seekBarDefaultVolume = (CustomSeekBar) v.findViewById(R.id.seekBarDefaultVolume);
        subSeekBarDefaultVolume = (CustomSubSeekBar) v.findViewById(R.id.subSeekBarDefaultVolume);
        txtChannelName = (EditText) v.findViewById(R.id.txtChannelName);
        txtChannelURL = (EditText) v.findViewById(R.id.txtChannelURL);
//        txtRadikoUser = (EditText) v.findViewById(R.id.txtUserName);
//        txtRadikoPassword = (EditText) v.findViewById(R.id.txtPassword);
//        btLogInLogOutPremium = (TextView) v.findViewById(R.id.btCheckLogin);
        switchRegion = (Switch) v.findViewById(R.id.switchRegion);
        spinnerTokenType = (Spinner) v.findViewById(R.id.spinnerTokenType);
        v.findViewById(R.id.lblTokenType).setVisibility(getResources().getBoolean(R.bool.is_debug_mode) ? View.VISIBLE : View.GONE);
        v.findViewById(R.id.rlTokenType).setVisibility(getResources().getBoolean(R.bool.is_debug_mode) ? View.VISIBLE : View.GONE);
//        v.findViewById(R.id.btCheckLogin).setOnClickListener(this);
        initSeekbar();
        loadData();
        btLoginFacebook = (TextView) v.findViewById(R.id.btLoginFacebook);
        btLoginFacebook.setOnClickListener(this);
        profileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile profile, Profile profile1) {
                if (profile1 != null) {
                    btLoginFacebook.setText(profile1.getName());
                } else {
                    btLoginFacebook.setText(getActivity().getString(R.string.label_login_with_facebook));
                }
            }
        };
        AccessToken token = AccessToken.getCurrentAccessToken();
        if (token != null) {
            Profile profile = Profile.getCurrentProfile();
            if (profile != null) {
                btLoginFacebook.setText(profile.getName());
            } else {
                btLoginFacebook.setText(getActivity().getString(R.string.label_login_with_facebook));
            }
        }
        profileTracker.startTracking();
        return v;
    }

    private ProfileTracker profileTracker;

    @Override
    public void onDestroy() {
        profileTracker.stopTracking();
        super.onDestroy();
    }

    private void initSeekbar() {
        String[] slowStep = new String[]{"x0.9", "x0.7", "x0.5", "x0.3"};
        String[] fastStep = new String[]{"x1.3", "x1.5", "x1.7", "x2.0"};
        String[] backStep = new String[]{"10s", "7s", "5s", "3s"};
        String[] volumeStep = new String[]{"0%", "33%", "66%", "100%"};
        seekBarSlowLevel.setMax(MAX_PROCESS);
        seekBarSlowLevel.setItems(slowStep);
        seekBarSlowLevel.setOnSeekBarChangeListener(mSeekListener);
        seekBarSlowLevel.invalidate();
        subSeekBarSlowLevel.setItems(slowStep);
        subSeekBarSlowLevel.invalidate();

        seekBarFastLevel.setMax(MAX_PROCESS);
        seekBarFastLevel.setItems(fastStep);
        seekBarFastLevel.setOnSeekBarChangeListener(mSeekListener);
        seekBarFastLevel.invalidate();
        subSeekBarFastLevel.setItems(fastStep);
        subSeekBarFastLevel.invalidate();

        seekBarBackLength.setMax(MAX_PROCESS);
        seekBarBackLength.setItems(backStep);
        seekBarBackLength.setOnSeekBarChangeListener(mSeekListener);
        seekBarBackLength.invalidate();
        subSeekBarLength.setItems(backStep);
        subSeekBarLength.invalidate();

        seekBarDefaultVolume.setMax(MAX_PROCESS);
        seekBarDefaultVolume.setItems(volumeStep);
        seekBarDefaultVolume.setOnSeekBarChangeListener(mSeekListener);
        seekBarDefaultVolume.invalidate();
        subSeekBarDefaultVolume.setItems(volumeStep);
        subSeekBarDefaultVolume.invalidate();
    }

    private void loadData() {
        Setting setting = new Setting(getActivity());
        setting.load();
        seekBarFastLevel.setProgress(setting.getFastLevelPercent());
        seekBarSlowLevel.setProgress(setting.getSlowLevelPercent());
        seekBarBackLength.setProgress(setting.getBackLengthPercent());
        seekBarDefaultVolume.setProgress(setting.getDefaultVolume());
        switchRegion.setChecked(setting.isRegion());
        spinnerTokenType.setSelection(setting.getTokenType());
//        txtRadikoUser.setText(setting.getRadioUser());
//        txtRadikoPassword.setText(setting.getRadikoPassword());
//        if (setting.isPremium()) {
//            AppDelegate.getInstance().setPremium(true);
//            txtRadikoUser.setClickable(false);
//            txtRadikoUser.setEnabled(false);
//            txtRadikoPassword.setVisibility(View.GONE);
//            btLogInLogOutPremium.setText(R.string.log_out);
//        } else {
//            AppDelegate.getInstance().setPremium(false);
//            txtRadikoUser.setClickable(true);
//            txtRadikoUser.setEnabled(true);
//            txtRadikoPassword.setVisibility(View.VISIBLE);
//            btLogInLogOutPremium.setText(R.string.login);
//        }
    }

    private void saveData() {
        Setting setting = new Setting(getActivity());
        setting.applyFastLevel(seekBarFastLevel.getProgress());
        setting.applySlowLevel(seekBarSlowLevel.getProgress());
        setting.applyBackLength(seekBarBackLength.getProgress());
        setting.setRegion(switchRegion.isChecked());
        setting.setTokenType(spinnerTokenType.getSelectedItemPosition());
        setting.setDefaultVolume(seekBarDefaultVolume.getProgress());
//        setting.setRadioUser(txtRadikoUser.getText().toString());
//        setting.setRadikoPassword(txtRadikoPassword.getText().toString());
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
                channel.setLastPlayedTime(new Date(System.currentTimeMillis()));
                adapter.insert(channel);
            } catch (Exception ex) {
                SimpleAppLog.error("Could not save channel", ex);
            } finally {
                adapter.close();
            }
        }
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateChannels(null);
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
            case R.id.btLoginFacebook:
                Intent intent1 = new Intent(Constants.INTENT_FILTER_FRAGMENT_ACTION);
                intent1.putExtra(Constants.FRAGMENT_ACTION_TYPE, Constants.ACTION_LOGIN_FACEBOOK);
                getActivity().sendBroadcast(intent1);
                break;
            case R.id.btCheckLogin: {
//                final Setting setting = new Setting(getContext());
//                setting.load();
//                if (setting.isPremium()) {
//                    AppDelegate.getInstance().setPremium(false);
//                    AppDelegate.getInstance().setPassword("");
//                    AppDelegate.getInstance().setUserName("");
//                    txtRadikoUser.setClickable(true);
//                    txtRadikoUser.setEnabled(true);
//                    txtRadikoPassword.setVisibility(View.VISIBLE);
//                    btLogInLogOutPremium.setText(R.string.login);
//                    setting.setRadikoPassword("");
//                    setting.setRadioUser("");
//                    setting.setPremium(false);
//                    setting.save();
//                    Activity activity = getActivity();
//                    if (activity instanceof BaseFragmentActivity) {
//                        try {
//                            ((BaseFragmentActivity) activity).updateData();
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                } else {
//                    final ClientTokenFetcher tokenFetcher = new ClientTokenFetcher(null, null, txtRadikoUser.getText().toString(), txtRadikoPassword.getText().toString());
//                    new AsyncTask<Void, Void, Boolean>() {
//                        @Override
//                        protected void onPreExecute() {
//                            Toast.makeText(getContext(), R.string.start_check_radiko_account, Toast.LENGTH_LONG).show();
//                        }
//
//                        @Override
//                        protected Boolean doInBackground(Void... params) {
//                            return tokenFetcher.checkLogin();
//                        }
//
//                        @Override
//                        protected void onPostExecute(Boolean aBoolean) {
//                            if (aBoolean != null) {
//                                setting.load();
//                                if (aBoolean) {
//                                    AppDelegate.getInstance().setPremium(true);
//                                    AppDelegate.getInstance().setUserName(txtRadikoUser.getText().toString());
//                                    AppDelegate.getInstance().setPassword(txtRadikoPassword.getText().toString());
//                                    Toast.makeText(getContext(), R.string.login_radiko_success, Toast.LENGTH_LONG).show();
//                                    setting.setRadioUser(txtRadikoUser.getText().toString());
//                                    setting.setRadikoPassword(txtRadikoPassword.getText().toString());
//                                    setting.setPremium(true);
//                                    setting.save();
//                                    txtRadikoUser.setClickable(false);
//                                    txtRadikoUser.setEnabled(false);
//                                    txtRadikoPassword.setVisibility(View.GONE);
//                                    btLogInLogOutPremium.setText(R.string.log_out);
//                                    Activity activity = getActivity();
//                                    if (activity instanceof BaseFragmentActivity) {
//                                        try {
//                                            ((BaseFragmentActivity) activity).updateData();
//                                        } catch (Exception e) {
//                                            e.printStackTrace();
//                                        }
//                                    }
//                                } else {
//                                    Toast.makeText(getContext(), R.string.login_radiko_failed, Toast.LENGTH_LONG).show();
//                                    setting.setRadioUser("");
//                                    setting.setRadikoPassword("");
//                                    setting.setPremium(false);
//                                    setting.save();
//                                }
//                            }
//                        }
//                    }.execute();
//                }
            }
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
