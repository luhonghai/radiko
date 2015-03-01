package com.gmail.radioserver2.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TabHost;

/**
 * Created by luhonghai on 2/28/15.
 */
public class ReclickableTabHost extends TabHost {

    public void setOnReclickTabListener(OnReclickTabListener onReclickTabListener) {
        this.onReclickTabListener = onReclickTabListener;
    }

    public static interface OnReclickTabListener {
        public void onReclick(int index);
    }

    public ReclickableTabHost(Context context) {
        super(context);
    }

    public ReclickableTabHost(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private OnReclickTabListener onReclickTabListener;

    @Override
    public void setCurrentTab(int index) {
        if (index == getCurrentTab()) {
            if (onReclickTabListener != null) {
                onReclickTabListener.onReclick(index);
            }
        } else {
            super.setCurrentTab(index);
        }
    }
}
