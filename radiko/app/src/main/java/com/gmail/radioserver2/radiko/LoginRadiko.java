package com.gmail.radioserver2.radiko;

import android.text.TextUtils;
import android.util.Log;

import com.gmail.radioserver2.utils.AndroidUtil;
import com.gmail.radioserver2.utils.AppDelegate;
import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

public class LoginRadiko {
    private final String USER_AGENT = "Mozilla/5.0";

    public LoginRadiko() {
    }

    public boolean checkLogin(String userName, String password) {
        if (userName == null || userName.length() == 0 || password == null || password.length() == 0) {
            return false;
        }
        HttpURLConnection httpURLConnection = AndroidUtil.getNewHttpConnection("https://radiko.jp/ap/member/login/login");
        if (httpURLConnection != null) {
            CookieManager cookieManager = new CookieManager();
            CookieHandler.setDefault(cookieManager);
            try {
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setRequestProperty("User-Agent", USER_AGENT);
                httpURLConnection.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());
                String urlParameters = String.format("mail=%s&pass=%s", userName, password);
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();
                BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                String strRes = IOUtils.toString(in);
                boolean logedIn = false;
                List<HttpCookie> cookieList = cookieManager.getCookieStore().getCookies();
                for (HttpCookie cookie : cookieList) {
                    if (cookie.getName().equalsIgnoreCase("ssl_token")) {
                        logedIn = true;
                        Gson gson = new Gson();
                        String jsonStr = gson.toJson(cookieList);
                        AppDelegate.getInstance().setCookie(jsonStr);
                        break;
                    }
                }
                in.close();
                return logedIn;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                httpURLConnection.disconnect();
            }
        }
        return false;
    }
}
