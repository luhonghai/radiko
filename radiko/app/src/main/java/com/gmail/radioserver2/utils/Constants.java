/*
 * FFmpegMediaPlayer: A unified interface for playing audio files and streams.
 *
 * Copyright 2014 William Seemann
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gmail.radioserver2.utils;

public class Constants {
    /**
     * Application fragment tab
     */

    public static final String SHARE_PREF = "share_pref";

    public static final String SEND_TO_BACK_GROUND = "send_to_back_ground";

    public static final String TAB_HOME = "TAB_HOME";

    public static final String TAB_PLAY_SCREEN = "TAB_PLAY_SCREEN";

    public static final String TAB_RECORDED_PROGRAM = "TAB_RECORDED_PROGRAM";

    public static final String TAB_LIBRARY = "TAB_LIBRARY";

    public static final String TAB_SETTING = "TAB_SETTING";

    public static final int TAB_HOME_ID = 0;

    public static final int TAB_PLAY_SCREEN_ID = 1;

    public static final int TAB_RECORDED_PROGRAM_ID = 2;

    public static final int TAB_LIBRARY_ID = 3;

    public static final int TAB_SETTING_ID = 4;

    public static final String PLAYBACK_VIEWER_INTENT = "com.gmail.radioserver2.PLAYBACK_VIEWER";

    /**
     * Fragment action intent
     */
    public static final String INTENT_FILTER_FRAGMENT_ACTION = "com.gmail.radioserver2.utils.Constants.INTENT_FILTER_FRAGMENT_ACTION";

    public static final String FRAGMENT_ACTION_TYPE = "FRAGMENT_ACTION_TYPE";

    public static final int ACTION_CLICK_BACK_PLAYER = 1;

    public static final int ACTION_SELECT_CHANNEL_ITEM = 2;

    public static final int ACTION_CALL_SELECT_TAB = 3;

    public static final int ACTION_SELECT_LIBRARY_ITEM = 4;

    public static final int ACTION_SELECT_RECORDED_PROGRAM_ITEM = 5;

    public static final int ACTION_RELOAD_LIST = 6;

    public static final int ACTION_RESET_FILTER_RECORDED_PROGRAM = 7;

    public static final int ACTION_RELOAD_RECORDED_PROGRAM = 8;
    public static final int ACTION_SHARE_FACEBOOK = 9;
    public static final int ACTION_LOGIN_FACEBOOK = 10;
    public static final String PARAMETER_SELECTED_TAB_ID = "PARAMETER_SELECTED_TAB_ID";

    public static final String PARAMETER_SELECTED_TAB = "PARAMETER_SELECTED_TAB";

    public static final String ARG_OBJECT = "ARG_OBJECT";
    public static final String ARG_TIMER_LIST = "ARG_TIMER_LIST";

    public static final String KEY_CHANGE_VOLUME = "key_need_change_volume";
    public static final String KEY_DEFAULT_VOLUME = "key_default_volume";


    public static final String KEY_USERNAME = "radiko_username";
    public static final String KEY_PASSWORD = "radiko_password";
    public  static final String KEY_PREMIUM = "is_premium";
}
