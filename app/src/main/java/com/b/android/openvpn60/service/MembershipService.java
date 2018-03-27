package com.b.android.openvpn60.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;


public class MembershipService extends Service {

    private volatile HandlerThread handlerThread;
    private ServiceHandler serviceHandler;

    // Define how the handler will process messages
    private final class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        // Define how to handle any incoming messages here
        @Override
        public void handleMessage(Message message) {
            // ...
            // When needed, stop the service with
            // stopSelf();
        }
    }

    // Fires when a service is first initialized
    public void onCreate() {
        super.onCreate();
        // An Android handler thread internally operates on a looper.
        handlerThread = new HandlerThread("MembershipService.HandlerThread");
        handlerThread.start();
        // An Android service handler is a handler running on a specific background thread.
        serviceHandler = new ServiceHandler(handlerThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Send empty message to background thread
        serviceHandler.sendEmptyMessageDelayed(0, 500);
        // or run code in background
        serviceHandler.post(new Runnable() {
            @Override
            public void run() {
                // Do something here in background!
                // ...
                // If desired, stop the service
                stopSelf();
            }
        });
        // Keep service around "sticky"
        return START_STICKY;
    }

    // Defines the shutdown sequence
    @Override
    public void onDestroy() {
        // Cleanup service before destruction
        handlerThread.quit();
    }

    // Binding is another way to communicate between service and activity
    // Not needed here, local broadcasts will be used instead
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
