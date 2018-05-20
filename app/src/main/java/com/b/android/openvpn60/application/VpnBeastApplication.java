package com.b.android.openvpn60.application;

import android.app.Application;

import com.b.android.openvpn60.helper.LogHelper;
import com.b.android.openvpn60.listener.StatusListener;
import com.b.android.openvpn60.util.PRNGUtil;


public class VpnBeastApplication extends Application {
    private StatusListener statusListener;
    private LogHelper logHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        PRNGUtil.apply();
        logHelper = LogHelper.getLogHelper(getApplicationContext());
        statusListener = new StatusListener();
        statusListener.init(getApplicationContext());
        logHelper.logInfo("StatusListener created on VpnBeastApplication...");
    }
}
