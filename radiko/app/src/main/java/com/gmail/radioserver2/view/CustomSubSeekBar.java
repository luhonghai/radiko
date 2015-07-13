package com.gmail.radioserver2.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;

import com.gmail.radioserver2.R;

/**
 * Created by luhonghai on 3/25/15.
 */
public class CustomSubSeekBar extends View {

    private Paint paint;

    private String[] items;

    private int thumbWidth = 0;

    private int thumbHeight = 0;

    public CustomSubSeekBar(Context context) {
        super(context);
        init();
    }

    public CustomSubSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomSubSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.DEFAULT_BOLD);

        paint.setColor(getContext().getResources().getColor(R.color.default_button_color));
        Drawable thumbDrawable =  getContext().getResources().getDrawable(R.drawable.apptheme_scrubber_control_normal_holo);
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
        int textSize = 2 * height / 3;
        paint.setTextSize(textSize);

        if (items != null) {
            for (int i = 0; i < items.length; i++) {
                String text = items[i];
                canvas.drawText(text,
                        padding + (i * seekLength / 3),
                        height,
                        paint);
            }
        }
        super.onDraw(canvas);
    }

    public void setItems(String[] items) {
        this.items = items;
    }
}
