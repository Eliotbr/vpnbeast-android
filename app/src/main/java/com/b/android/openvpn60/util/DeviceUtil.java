package com.b.android.openvpn60.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;

import com.b.android.openvpn60.helper.LogHelper;

public class DeviceUtil {

    private static LogHelper logHelper;


    /**
     * Below method created to debug for creating different views for different devices
     */
    public static void getDeviceInfos(Activity activity) {
        logHelper = LogHelper.getLogHelper(DeviceUtil.class.getName());
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        logHelper.logInfo("width = " + width);
        logHelper.logInfo("height = " + height);
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int heightPixels = metrics.heightPixels;
        int widthPixels = metrics.widthPixels;
        int densityDpi = metrics.densityDpi;
        float xdpi = metrics.xdpi;
        float ydpi = metrics.ydpi;
        logHelper.logInfo("widthPixels = " + widthPixels);
        logHelper.logInfo("heightPixels = " + heightPixels);
        logHelper.logInfo("densityDpi = " + densityDpi);
    }

}
