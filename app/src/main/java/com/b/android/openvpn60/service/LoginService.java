package com.b.android.openvpn60.service;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Toast;

import com.b.android.openvpn60.R;
import com.b.android.openvpn60.activity.LoginActivity;
import com.b.android.openvpn60.constant.AppConstants;
import com.b.android.openvpn60.constant.ServiceConstants;
import com.b.android.openvpn60.helper.LogHelper;
import com.b.android.openvpn60.model.User;
import com.b.android.openvpn60.util.ViewUtil;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
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


public class LoginService extends Service {

    private volatile HandlerThread handlerThread;
    private ServiceHandler serviceHandler;
    private LocalBroadcastManager localBroadcastManager;
    private Context context;
    private LogHelper logHelper;
    private Intent responseIntent;
    public static final String ACTION = "com.b.android.service.LoginService";


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
        handlerThread = new HandlerThread("LoginService.HandlerThread");
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
                // Send broadcast out with action filter and extras
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
        final List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair(AppConstants.USER_NAME.toString(), userName));
        nameValuePairs.add(new BasicNameValuePair(AppConstants.USER_PASS.toString(), userPass));
        HttpEntity entity = null;

        try {
            entity = new UrlEncodedFormEntity(nameValuePairs);
        }
        catch (UnsupportedEncodingException a) {
            logHelper.logException(a);
        }
        client.post(context, ServiceConstants.URL_LOGIN.toString(), entity, "application/x-www-form-urlencoded",
                new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        try {
                            if (response.getBoolean("status")) {
                                User user = new User();
                                user.setUserName(userName);
                                user.setUserPass(userPass);
                                logHelper.logInfo(context.getString(R.string.state_logged_in));

                                responseIntent.putExtra("status", "success");
                                responseIntent.putExtra(AppConstants.USER_NAME.toString(), userName);
                                responseIntent.putExtra(AppConstants.USER_PASS.toString(), userPass);
                                logHelper.logInfo("status = " + responseIntent.getStringExtra("status"));
                                logHelper.logInfo("username = " + responseIntent.getStringExtra(AppConstants.USER_NAME.toString()));
                                //context.startActivity(mainIntent);
                                //progressBar.setVisibility(View.GONE);
                            } else {
                                LoginActivity.errorCount++;
                                responseIntent.putExtra("status", "failure");
                            }

                        } catch (JSONException ex) {
                            Toast.makeText(context, context.getString(R.string.err_state_json),
                                    Toast.LENGTH_SHORT).show();
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

    private void stopService() {
        localBroadcastManager.sendBroadcast(responseIntent);
        stopSelf();
    }

}
