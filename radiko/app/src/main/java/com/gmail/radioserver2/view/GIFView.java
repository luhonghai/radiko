package com.gmail.radioserver2.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;

import com.gmail.radioserver2.R;

import java.io.InputStream;

/**
 * Created by luhonghai on 3/11/15.
 */
public class GIFView extends View {

    private Movie mMovie;
    private long movieStart;

    public GIFView(Context context) {
        super(context);
        initializeView();
    }

    public GIFView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAttrs(attrs);
    }

    public GIFView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setAttrs(attrs);
    }

    private int gifId;

    public void setGIFResource(int resId) {
        this.gifId = resId;
        initializeView();
    }

    public int getGIFResource() {
        return this.gifId;
    }

    private void initializeView() {
        if (gifId != 0) {
            InputStream is = getContext().getResources().openRawResource(gifId);
            mMovie = Movie.decodeStream(is);
            movieStart = 0;
            this.invalidate();
        }
    }

    private void setAttrs(AttributeSet attrs) {
        if (attrs != null) {
            setGIFResource(attrs.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "src", 0));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);
        super.onDraw(canvas);
        long now = android.os.SystemClock.uptimeMillis();
        if (movieStart == 0) {
            movieStart = now;
        }
        if (mMovie != null) {
            int relTime = (int) ((now - movieStart) % mMovie.duration());
            mMovie.setTime(relTime);
            mMovie.draw(canvas, getWidth() - mMovie.width(), getHeight() - mMovie.height());
            this.invalidate();
        }
    }
}
