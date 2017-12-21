package com.b.android.openvpn60.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by b on 5/15/17.
 */

public class PreferencesUtil {

    public static SharedPreferences getSharedPreferencesMulti(String name, Context c) {
        return c.getSharedPreferences(name, Context.MODE_MULTI_PROCESS | Context.MODE_PRIVATE);
    }

    public static SharedPreferences getDefaultSharedPreferences(Context c) {
        return c.getSharedPreferences(c.getPackageName() + "_preferences", Context.MODE_MULTI_PROCESS | Context.MODE_PRIVATE);
    }
}