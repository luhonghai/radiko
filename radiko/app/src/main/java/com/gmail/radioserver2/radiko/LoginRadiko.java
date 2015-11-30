package com.gmail.radioserver2.radiko;

import android.content.Context;
import android.provider.Settings;

import com.gmail.radioserver2.utils.AndroidUtil;
import com.gmail.radioserver2.utils.AppDelegate;
import com.gmail.radioserver2.utils.FileHelper;
import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LoginRadiko {
    private final String USER_AGENT = "Mozilla/5.0";
    private Context context;

    public LoginRadiko(Context context) {
        this.context = context;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public boolean checkLogin(String userName, String password) {
        if (userName == null || userName.length() == 0 || password == null || password.length() == 0) {
            return false;
        }
        HttpURLConnection httpURLConnection = AndroidUtil.getNewHttpConnection("https://radiko.jp/ap/member/login/login");
        if (httpURLConnection != null) {
            try {
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestProperty("User-Agent", USER_AGENT);
                String urlParameters = String.format("mail=%s&pass=%s", userName, password);
                IOUtils.write(urlParameters, httpURLConnection.getOutputStream());
                IOUtils.toString(httpURLConnection.getInputStream());
                boolean isLogin = false;
                Map<String, List<String>> map = httpURLConnection.getHeaderFields();
                List<String> cookies = map.get("Set-Cookie");
                for (String cookie : cookies) {
                    if (cookie.contains("ssl_token")) {
                        isLogin = true;
                        Gson gson = new Gson();
                        String cookieStr = gson.toJson(cookies);
                        AppDelegate.getInstance().setCookie(cookieStr);
                    }
                }
                int status = httpURLConnection.getResponseCode();
                boolean redirect = false;
                if (status != HttpURLConnection.HTTP_OK) {
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP
                            || status == HttpURLConnection.HTTP_MOVED_PERM
                            || status == HttpURLConnection.HTTP_SEE_OTHER) {
                        redirect = true;
                    }
                }
                if (redirect) {
                    // get redirect url from "location" header field
                    String newUrl = httpURLConnection.getHeaderField("Location");
                    // get the cookie if need, for login
                    String cookieStr = httpURLConnection.getHeaderField("Set-Cookie");
                    // open the new connnection again
                    httpURLConnection = (HttpURLConnection) new URL(newUrl).openConnection();
                    httpURLConnection.setRequestProperty("Cookie", cookieStr);
                    httpURLConnection.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
                }
                String strRes = IOUtils.toString(httpURLConnection.getInputStream());
                FileHelper fileHelper = new FileHelper(context);
                File logFile = new File(fileHelper.getRecordedProgramFolder(), "record_log.txt");
//                sendLogInResult(strRes, readLast1000Line(logFile), isLogin ? 1 : 0);
                return isLogin;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                httpURLConnection.disconnect();
            }
        }
        return false;
    }

    private String readLast1000Line(File file) {
        ArrayList<String> allFile = new ArrayList<>();
        String s = "";
        if (file.exists()) {
            try {
                FileReader fileReader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    allFile.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            Collections.reverse(allFile);
            int maxLine = allFile.size();
            for (int i = 0; i < (maxLine < 1000 ? maxLine : 1000); i++) {
                s += s.concat(allFile.get(i) + "\n");
            }
        }

        return s;
    }

    private void sendLogInResult(String strRes, String log, int loginRes) {
        HttpURLConnection httpURLConnection = AndroidUtil.getNewHttpConnection("http://stest.dotohsoft.com/~quantv/projects/radioserver/logapi/postlog.php");
        if (httpURLConnection != null) {
            try {
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);
                String udid = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
                String urlParameters = String.format("udid=%s&radlog=%s&recordlog=%s&loginret=%d", udid, strRes, log, loginRes);
                IOUtils.write(urlParameters, httpURLConnection.getOutputStream(), "UTF-8");
                System.out.println(httpURLConnection.getResponseCode());
                System.out.println(IOUtils.toString(httpURLConnection.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                httpURLConnection.disconnect();
            }
        }
    }
}
