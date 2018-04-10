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
import android.widget.Toast;

import com.b.android.openvpn60.R;
import com.b.android.openvpn60.activity.LoginActivity;
import com.b.android.openvpn60.constant.AppConstants;
import com.b.android.openvpn60.constant.ServiceConstants;
import com.b.android.openvpn60.helper.LogHelper;
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


public class UserService extends MainService {

    public static final String ACTION = "com.b.android.service.UserService";



    public void onCreate() {
        super.onCreate();
        // An Android handler thread internally operates on a looper.
        handlerThread = new HandlerThread("UserService.HandlerThread");
        handlerThread.start();
        context = getApplicationContext();
        logHelper = LogHelper.getLogHelper(UserService.class.getName());
        // An Android service handler is a handler running on a specific background thread.
        serviceHandler = new ServiceHandler(handlerThread.getLooper());
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
    }


    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        serviceHandler.post(new Runnable() {
            @Override
            public void run() {
                String username = intent.getStringExtra(AppConstants.USER_NAME.toString());
                String userpass = intent.getStringExtra(AppConstants.USER_PASS.toString());
                String action = intent.getAction();
                if (action != null) {
                    if (action.equals(AppConstants.INSERT_USER.toString())) {
                        responseIntent = new Intent(AppConstants.INSERT_USER.toString());
                        insertUser(username, userpass);
                    } else if (action.equals(AppConstants.DO_LOGIN.toString())) {
                        responseIntent = new Intent(AppConstants.DO_LOGIN.toString());
                        doLogin(username, userpass);
                    } else if (action.equals(AppConstants.UPDATE_LAST_LOGIN.toString())) {
                        responseIntent = new Intent(AppConstants.UPDATE_LAST_LOGIN.toString());
                        updateLastLogin(username);
                    }
                }
            }
        });
        return START_STICKY;
    }


    public void insertUser(final String userName, final String userPass) {
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
                            responseIntent.putExtra("status", "errServer404");
                        } else if(statusCode == 500) {
                            logHelper.logException(context.getString(R.string.err_server_500), throwable);
                            responseIntent.putExtra("status", "errServer500");
                        } else {
                            logHelper.logException(context.getString(R.string.err_server_else), throwable);
                            responseIntent.putExtra("status", "errServerElse");
                        }
                        stopService();
                    }

                });
    }


    public void updateLastLogin(String userName) {
        AsyncHttpClient client = new AsyncHttpClient();
        final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair(AppConstants.USER_NAME.toString(), userName));
        HttpEntity entity = null;
        try {
            entity = new UrlEncodedFormEntity(nameValuePairs);
        } catch (UnsupportedEncodingException a) {
            logHelper.logException(a);
        }
        client.put(getApplicationContext(), ServiceConstants.URL_PUT.toString(), entity, "application/x-www-form-urlencoded",
                new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        try {
                            if (response.getBoolean("status")) {
                                logHelper.logInfo("Update last login date = " + "OK");
                                responseIntent.putExtra("status", "success");
                            } else {
                                logHelper.logInfo("Update last login date = " + "Failed");
                                responseIntent.putExtra("status", "failure");
                            }
                        } catch (Exception ex) {
                            logHelper.logException(ex);
                        }
                        stopService();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        if(statusCode == 404) {
                            logHelper.logException(context.getString(R.string.err_server_404), throwable);
                            responseIntent.putExtra("status", "errServer404");
                        } else if(statusCode == 500) {
                            logHelper.logException(context.getString(R.string.err_server_500), throwable);
                            responseIntent.putExtra("status", "errServer500");
                        } else {
                            logHelper.logException(context.getString(R.string.err_server_else), throwable);
                            responseIntent.putExtra("status", "errServerElse");
                        }
                        stopService();
                    }
                });
    }


    public void doLogin(final String userName, final String userPass) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.setTimeout(8000);
        final List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair(AppConstants.USER_NAME.toString(), userName));
        nameValuePairs.add(new BasicNameValuePair(AppConstants.USER_PASS.toString(), userPass));
        HttpEntity entity = null;
        try {
            entity = new UrlEncodedFormEntity(nameValuePairs);
        } catch (UnsupportedEncodingException a) {
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
                            responseIntent.putExtra("status", "errServer404");
                        } else if(statusCode == 500) {
                            logHelper.logException(context.getString(R.string.err_server_500), throwable);
                            responseIntent.putExtra("status", "errServer500");
                        } else {
                            logHelper.logException(context.getString(R.string.err_server_else), throwable);
                            responseIntent.putExtra("status", "errServerElse");
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

}
