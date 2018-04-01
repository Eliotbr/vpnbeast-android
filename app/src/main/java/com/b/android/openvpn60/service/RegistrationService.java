package com.b.android.openvpn60.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Toast;

import com.b.android.openvpn60.R;
import com.b.android.openvpn60.constant.AppConstants;
import com.b.android.openvpn60.constant.ServiceConstants;
import com.b.android.openvpn60.helper.LogHelper;
import com.b.android.openvpn60.model.Member;
import com.b.android.openvpn60.model.User;
import com.b.android.openvpn60.util.PreferencesUtil;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.message.BasicNameValuePair;


public class RegistrationService extends Service {

    private volatile HandlerThread handlerThread;
    private RegistrationService.ServiceHandler serviceHandler;
    private LocalBroadcastManager localBroadcastManager;
    private Context context;
    private LogHelper logHelper;
    private Intent responseIntent;
    public static final String ACTION = "com.b.android.service.RegistrationService";


    // Define how the handler will process messages
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {

        }
    }

    // Fires when a service is first initialized
    public void onCreate() {
        super.onCreate();
        // An Android handler thread internally operates on a looper.
        handlerThread = new HandlerThread("RegistrationService.HandlerThread");
        handlerThread.start();
        context = getApplicationContext();
        logHelper = LogHelper.getLogHelper(LoginService.class.getName());
        // An Android service handler is a handler running on a specific background thread.
        serviceHandler = new ServiceHandler(handlerThread.getLooper());
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    // Fires when a service is started up
    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        serviceHandler.post(new Runnable() {
            @Override
            public void run() {
                responseIntent = new Intent(ACTION);
                String username = intent.getStringExtra(AppConstants.USER_NAME.toString());
                String userpass = intent.getStringExtra(AppConstants.USER_PASS.toString());
                invokeWS(username, userpass);
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


    public void invokeWS(final String userName, final String userPass) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.setTimeout(8000);
        final User user = new User(userName, userPass);
        final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair(AppConstants.USER_NAME.toString(), userName));
        nameValuePairs.add(new BasicNameValuePair(AppConstants.USER_PASS.toString(), userPass));
        nameValuePairs.add(new BasicNameValuePair(AppConstants.USER_UUID.toString(), user.getUuid().toString()));
        //client.addHeader("Content-Type", "application/json");
        HttpEntity entity = null;
        try {
            entity = new UrlEncodedFormEntity(nameValuePairs);
        }
        catch (UnsupportedEncodingException a) {
            logHelper.logException(a);
        }
        client.post(context, ServiceConstants.URL_REGISTER.toString(), entity, "application/x-www-form-urlencoded",
                new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        try {
                            if (response.getBoolean("status")) {
                                //loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                responseIntent.putExtra("status", "success");
                                responseIntent.putExtra(AppConstants.TEMP_USER.toString(), user);
                                responseIntent.putExtra(AppConstants.USER_NAME.toString(), userName);
                                responseIntent.putExtra(AppConstants.USER_PASS.toString(), userPass);
                                saveInfos(userName, userPass);
                            } else {
                                responseIntent.putExtra("status", "failure");
                            }
                        } catch (JSONException ex) {
                            logHelper.logException(ex);
                        }
                        stopService();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        if(statusCode == 404) {
                            logHelper.logException(context.getString(R.string.err_server_404), throwable);
                            responseIntent.putExtra("status", "err_server_404");
                        } else if(statusCode == 500) {
                            logHelper.logException(context.getString(R.string.err_server_500), throwable);
                            responseIntent.putExtra("status", "err_server_500");
                        } else {
                            logHelper.logException(context.getString(R.string.err_server_else), throwable);
                            responseIntent.putExtra("status", "err_server_else");
                        }
                        stopService();
                    }

                });
    }

    private void saveInfos(String userName, String password) {
        SharedPreferences sharedPreferences = PreferencesUtil.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor;
        editor = sharedPreferences.edit();
        editor.putString(AppConstants.USER_NAME.toString(), userName);
        editor.putString(AppConstants.USER_PASS.toString(), null);
        editor.apply();
        editor.commit();
    }

    private void stopService() {
        localBroadcastManager.sendBroadcast(responseIntent);
        stopSelf();
    }

}
