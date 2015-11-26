package com.gmail.radioserver2.service;

import com.gmail.radioserver2.data.Channel;

/**
 * Created by Trinh Quan on 016 16/6/2015.
 */
public interface OnRecordStateChangeListener {
    void showNotification(Channel channel);

    void refresh(boolean isRecord);
}
