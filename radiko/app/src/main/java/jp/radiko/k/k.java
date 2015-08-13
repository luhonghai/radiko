package jp.radiko.k;

import android.content.Context;

/**
 * Created by luhonghai on 5/4/15.
 */

public class k {
    static {
        System.loadLibrary("RadikoSmartPhoneExtra");
    }

    public static native byte[] getKeyNative(Context context, byte input[]);
}
