package com.swssm.waveloop.audio;

/**
 * Created by luhonghai on 3/21/15.
 */
public class OSLESMediaPlayer {

    static {
        System.loadLibrary("audio-tools");
    }

    public native void createEngine();

    public native void releaseEngine();

    public native boolean createAudioPlayer(String uri);

    public native void releaseAudioPlayer();

    public native void play();

    public native void stop();

    public native void pause();

    public native boolean isPlaying();

    public native void seekTo(int position);

    public native int getDuration();

    public native int getPosition();

    public native void setPitch(int rate);

    public native void setRate(int rate);

    public native int getRate();

    public native void setLoop(int startPos, int endPos);

    public native void setNoLoop();

    public interface OnCompletionListener {
        public void OnCompletion();
    }

    private OnCompletionListener mCompletionListener;

    public void SetOnCompletionListener(OnCompletionListener listener) {
        mCompletionListener = listener;
    }


    private void OnCompletion() {
        mCompletionListener.OnCompletion();

        int position = getPosition();
        int duration = getDuration();
        if (position != duration) {
            int a = 0;

        } else {
            int c = 0;

        }
    }
}
