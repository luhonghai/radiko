package com.gmail.radioserver2.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.SeekBar;

import com.gmail.radioserver2.R;

/**
 * Created by luhonghai on 3/25/15.
 */
public class CustomSeekBar extends SeekBar {

    private Paint paint;

    private String[] items;

    private int thumbWidth = 0;

    private int thumbHeight = 0;

    private float posA = -1;

    private float posB = -1;

    public CustomSeekBar(Context context) {
        super(context);
        init();
    }

    public CustomSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.DEFAULT_BOLD);

        paint.setColor(getContext().getResources().getColor(R.color.default_button_color));
        Drawable thumbDrawable = getContext().getResources().getDrawable(R.drawable.apptheme_scrubber_control_normal_holo);
        if (thumbDrawable != null) {
            thumbWidth = thumbDrawable.getIntrinsicHeight();
            thumbHeight = thumbDrawable.getIntrinsicWidth();
        }
    }


    @Override
    protected synchronized void onDraw(Canvas canvas) {
        int height = getHeight();
        int width = getWidth();
        int pointHeight = height / 4 + 5;
        int pointWidth = 6;
        int padding = thumbWidth / 2;
        int seekLength = width - padding * 2;
        if (items != null && items.length > 0) {
            canvas.drawRect(padding, height / 2 - pointWidth / 2, width - padding, height / 2 + pointWidth / 2, paint);
            canvas.drawRect(padding - pointWidth / 2,
                    (height - pointHeight) / 2,
                    padding + pointWidth / 2,
                    height - (height - pointHeight) / 2, paint);
            canvas.drawRect(width - padding - pointWidth / 2,
                    (height - pointHeight) / 2,
                    width - padding + pointWidth / 2,
                    height - (height - pointHeight) / 2, paint);
            canvas.drawRect(padding + seekLength / 3 - pointWidth / 2,
                    (height - pointHeight) / 2,
                    padding + seekLength / 3 + pointWidth / 2,
                    height - (height - pointHeight) / 2, paint);
            canvas.drawRect(padding + 2 * seekLength / 3 - pointWidth / 2,
                    (height - pointHeight) / 2,
                    padding + 2 * seekLength / 3 + pointWidth / 2,
                    height - (height - pointHeight) / 2, paint);
        }
        if (posA != -1) {
            canvas.drawRect(padding + seekLength * posA - pointWidth / 2,
                    (height - pointHeight) / 2,
                    padding + seekLength * posA + pointWidth / 2,
                    height - (height - pointHeight) / 2, paint);
        }
        if (posB != -1) {
            canvas.drawRect(padding + seekLength * posB - pointWidth / 2 + 5,
                    (height - pointHeight) / 2,
                    padding + seekLength * posB + pointWidth / 2 + 5,
                    height - (height - pointHeight) / 2, paint);
        }

        super.onDraw(canvas);
    }

    public void setItems(String[] items) {
        this.items = items;
    }

    public float getPosA() {
        return posA;
    }

    public void setPosA(float posA) {
        this.posA = posA;
    }

    public float getPosB() {
        return posB;
    }

    public void setPosB(float posB) {
        this.posB = posB;
    }
}
