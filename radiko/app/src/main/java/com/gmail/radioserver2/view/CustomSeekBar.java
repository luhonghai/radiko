package com.gmail.radioserver2.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.SeekBar;

import com.gmail.radioserver2.R;

/**
 * Created by luhonghai on 3/25/15.
 */
public class CustomSeekBar extends SeekBar {

    private Paint paint;

    private String[] items;

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
    }


    @Override
    protected synchronized void onDraw(Canvas canvas) {
        int height = getHeight();
        int width = getWidth();
        int pointHeight = height / 4 + 5;
        int pointWidth = 6;
        int padding = 32;
        int seekLength = width - padding * 2;
        int textSize = 24;
        paint.setTextSize(textSize);

        canvas.drawRect(padding, height / 2 - pointWidth / 2, width - padding, height/ 2 + pointWidth / 2, paint);
        canvas.drawRect(padding - pointWidth / 2,
                (height - pointHeight) / 2,
                padding + pointWidth / 2,
                height - (height - pointHeight) / 2, paint);
        canvas.drawRect(- 0.2f + width - padding - pointWidth / 2,
                (height - pointHeight) / 2,
                - 0.2f + width - padding + pointWidth / 2,
                height - (height - pointHeight) / 2, paint);
        canvas.drawRect(- 1.4f + padding + seekLength / 3 - pointWidth / 2,
                (height - pointHeight) / 2,
                - 1.4f + padding + seekLength / 3 + pointWidth / 2,
                height - (height - pointHeight) / 2, paint);
        canvas.drawRect(- 3.7f + padding + 2 * seekLength / 3 - pointWidth / 2,
                (height - pointHeight) / 2,
                 -3.7f + padding + 2 * seekLength / 3 + pointWidth / 2,
                height - (height - pointHeight) / 2, paint);

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
