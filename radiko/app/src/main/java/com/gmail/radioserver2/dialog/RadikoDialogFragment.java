package com.gmail.radioserver2.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.gmail.radioserver2.R;
import com.gmail.radioserver2.activity.BaseFragmentActivity;
import com.gmail.radioserver2.radiko.LoginRadiko;
import com.gmail.radioserver2.utils.AppDelegate;
import com.gmail.radioserver2.utils.Constants;

public class RadikoDialogFragment extends DialogFragment implements View.OnClickListener {
    private CheckBox cbDontAskAgain;
    private EditText edtUser, edtPass;
    private View btClose;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View contentView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_login_radiko, null, false);
        Dialog dialog = new Dialog(getContext(), R.style.dialog_style);
        dialog.setContentView(contentView);
        cbDontAskAgain = (CheckBox) contentView.findViewById(R.id.cbDontDisplayAgain);
        edtUser = (EditText) contentView.findViewById(R.id.txtUserName);
        edtPass = (EditText) contentView.findViewById(R.id.txtPassword);
        contentView.findViewById(R.id.btCheckLogin).setOnClickListener(this);
        btClose = contentView.findViewById(R.id.btClose);
        btClose.setOnClickListener(this);
        setCancelable(false);
        return dialog;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btCheckLogin: {
                didCheckLogin();
            }
            break;
            case R.id.btClose: {
                AppDelegate.getInstance().setShowDialog(!cbDontAskAgain.isChecked());
                dismiss();
            }
            break;
        }
    }

    private void didCheckLogin() {
        btClose.setEnabled(false);
        final LoginRadiko loginRadiko = new LoginRadiko(getActivity());
        new AsyncTask<String, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                Toast.makeText(getContext(), R.string.start_check_radiko_account, Toast.LENGTH_LONG).show();
            }

            @Override
            protected Boolean doInBackground(String... params) {
                return loginRadiko.checkLogin(params[0], params[1]);
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                if (aBoolean != null) {
                    if (aBoolean) {
                        AppDelegate.getInstance().setPremium(true);
                        SharedPreferences preferences = getActivity().getSharedPreferences(Constants.SHARE_PREF, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("Cookie", AppDelegate.getInstance().getCookie());
                        editor.putBoolean(Constants.KEY_PREMIUM, true);
                        editor.apply();
                        Toast.makeText(getContext(), R.string.login_radiko_success, Toast.LENGTH_LONG).show();
                        Activity activity = getActivity();
                        if (activity != null) {
                            if (activity instanceof BaseFragmentActivity) {
                                try {
                                    ((BaseFragmentActivity) activity).updateData();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            Intent intent = new Intent();
                            intent.setAction("sadhkahd.recreate_ui");
                            activity.sendBroadcast(intent);
                            if (!isCancelled() && !activity.isFinishing()) {
                                try {
                                    dismiss();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        return;
                    }
                }
                Log.d("login", "login failed");
                try {
                    Toast.makeText(getActivity(), R.string.login_radiko_failed, Toast.LENGTH_LONG).show();
                } catch (Resources.NotFoundException | NullPointerException e) {
                    e.printStackTrace();
                }
                btClose.setEnabled(true);
            }
        }.execute(edtUser.getText().toString(), edtPass.getText().toString());
    }
}
