package com.gmail.radioserver2.utils;

/**
 * Created by luhonghai on 4/1/15.
 */
public class StringUtil {
    public static String escapeJapanSpecialChar(String input) {
        if (input == null || input.length() == 0) return input;
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            int c = (int) chars[i];
            if ((c >= 65313 && c <= 65338) || (c >= 65345 && c <= 65370))
                chars[i] = (char) (c - 65248);
        }
        return new String(chars);
    }
}
