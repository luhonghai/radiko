package com.gmail.radioserver2.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by luhonghai on 2/22/15.
 */
public class FileHelper {

    private static final String TOKEN_FILE = "token.txt";
    private static final String TOKEN_TMP_FILE = "token.tmp";

    private static final String DEFAULT_TEMP_FLV = "dump.flv";

    private final Context context;

    public FileHelper(Context context) {
        this.context = context;
    }

    public File getTempFile(String fileName) {
        PackageManager m = context.getPackageManager();
        String s = context.getPackageName();
        try {
            PackageInfo p = m.getPackageInfo(s, 0);
            s = p.applicationInfo.dataDir;
            return new File(s, fileName);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public File getTempFile() {
        return getTempFile(DEFAULT_TEMP_FLV);
    }

    public String getTokenString() {
        String token = "";
        File tmp = getTokenFile();
        if (tmp.exists()) {
            try {
                token = FileUtils.readFileToString(tmp).trim();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return token;
    }

    public File getTokenFile() {
        PackageManager m = context.getPackageManager();
        String s = context.getPackageName();
        try {
            PackageInfo p = m.getPackageInfo(s, 0);
            s = p.applicationInfo.dataDir;
            return new File(s, TOKEN_FILE);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public File getTmpTokenFile() {
        PackageManager m = context.getPackageManager();
        String s = context.getPackageName();
        try {
            PackageInfo p = m.getPackageInfo(s, 0);
            s = p.applicationInfo.dataDir;
            return new File(s, TOKEN_TMP_FILE);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
