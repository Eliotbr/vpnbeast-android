package com.b.android.openvpn60.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;

import com.b.android.openvpn60.helper.LogHelper;


public abstract class MainService extends Service {

    public volatile HandlerThread handlerThread;
    public ServiceHandler serviceHandler;
    public LocalBroadcastManager localBroadcastManager;
    public Context context;
    public LogHelper logHelper;
    public Intent responseIntent;



    // Define how the handler will process messages
    public final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {

        }
    }


    @Override
    public void onDestroy() {
        handlerThread.quit();
    }


    // Binding is another way to communicate between service and activity
    // Not needed here, local broadcasts will be used instead
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public void stopService() {
        localBroadcastManager.sendBroadcast(responseIntent);
        stopSelf();
    }
}
