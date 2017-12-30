package com.b.android.openvpn60.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.b.android.openvpn60.constant.AppConstants;

/**
 * Created by b on 5/15/17.
 */

public class PreferencesUtil {

    public static SharedPreferences getDefaultSharedPreferences(Context context) {
        return context.getSharedPreferences(AppConstants.SHARED_PREFS.toString(), Context.MODE_PRIVATE);
    }
}