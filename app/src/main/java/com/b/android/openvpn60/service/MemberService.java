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

import com.b.android.openvpn60.R;
import com.b.android.openvpn60.constant.AppConstants;
import com.b.android.openvpn60.constant.ServiceConstants;
import com.b.android.openvpn60.helper.LogHelper;
import com.b.android.openvpn60.model.Member;
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


public class MemberService extends MainService {

    public static final String ACTION = "com.b.android.service.MemberService";


    // Fires when a service is first initialized
    public void onCreate() {
        super.onCreate();
        // An Android handler thread internally operates on a looper.
        handlerThread = new HandlerThread("MemberService.HandlerThread");
        handlerThread.start();
        context = getApplicationContext();
        logHelper = LogHelper.getLogHelper(MemberService.class.getName());
        // An Android service handler is a handler running on a specific background thread.
        serviceHandler = new ServiceHandler(handlerThread.getLooper());
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
    }


    // Fires when a service is started up
    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        final String action = intent.getAction();
        serviceHandler.post(new Runnable() {
            @Override
            public void run() {
                if (action != null && action.equals(AppConstants.INSERT_MEMBER.toString())) {
                    responseIntent = new Intent(AppConstants.INSERT_MEMBER.toString());
                    String username = intent.getStringExtra(AppConstants.USER_NAME.toString());
                    String firstName = intent.getStringExtra(AppConstants.FIRST_NAME.toString());
                    String lastName = intent.getStringExtra(AppConstants.LAST_NAME.toString());
                    String email = intent.getStringExtra(AppConstants.EMAIL.toString());
                    insertMember(username, firstName, lastName, email);
                } else if (action != null && action.equals(AppConstants.CHECK_MEMBER.toString())) {
                    responseIntent = new Intent(AppConstants.CHECK_MEMBER.toString());
                    String username = intent.getStringExtra(AppConstants.USER_NAME.toString());
                    checkMembership(username);
                }
            }
        });
        // Keep service around "sticky"
        return START_STICKY;
    }


    public void insertMember(final String userName, final String firstName, final String lastName, final String email) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.setTimeout(8000);
        final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair(AppConstants.USER_NAME.toString(), userName));
        nameValuePairs.add(new BasicNameValuePair(AppConstants.FIRST_NAME.toString(), firstName));
        nameValuePairs.add(new BasicNameValuePair(AppConstants.LAST_NAME.toString(), lastName));
        nameValuePairs.add(new BasicNameValuePair(AppConstants.EMAIL.toString(), email));
        HttpEntity entity = null;
        try {
            entity = new UrlEncodedFormEntity(nameValuePairs);
        } catch (UnsupportedEncodingException a) {
            logHelper.logException(a);
        }
        client.post(getApplicationContext(), ServiceConstants.URL_REGISTER_MEMBER.toString(), entity,
                "application/x-www-form-urlencoded", new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        try {
                            if (response.getBoolean("status")) {
                                Member member = new Member();
                                member.setUserName(userName);
                                member.setFirstName(firstName);
                                member.setLastName(lastName);
                                member.setEmail(email);
                                logHelper.logInfo(context.getString(R.string.state_logged_in));
                                responseIntent.putExtra("status", "success");
                                responseIntent.putExtra(AppConstants.USER_NAME.toString(), userName);
                                responseIntent.putExtra(AppConstants.FIRST_NAME.toString(), firstName);
                                responseIntent.putExtra(AppConstants.LAST_NAME.toString(), lastName);
                                responseIntent.putExtra(AppConstants.EMAIL.toString(), email);
                                logHelper.logInfo("status = " + responseIntent.getStringExtra("status"));
                                logHelper.logInfo("userName = " + userName);
                                logHelper.logInfo("firstName = " + firstName);
                                logHelper.logInfo("lastName = " + lastName);
                                logHelper.logInfo("email = " + email);
                                saveInfos(userName, firstName, lastName, email);
                            } else
                                responseIntent.putExtra("status", "failure");
                        } catch (JSONException ex) {
                            logHelper.logException(ex);
                        }
                        stopService();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        if(statusCode == 404) {
                            logHelper.logException(context.getString(R.string.err_server_404), throwable);
                            responseIntent.putExtra("status", "errServer400");
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


    private void checkMembership(String userName){
        AsyncHttpClient client = new AsyncHttpClient();
        final List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair(AppConstants.USER_NAME.toString(), userName));
        HttpEntity entity = null;
        try {
            entity = new UrlEncodedFormEntity(nameValuePairs);
        }
        catch (UnsupportedEncodingException a) {
            logHelper.logException(a);
        }
        client.post(getApplicationContext(), ServiceConstants.URL_CHECK_MEMBERS.toString(), entity,
                "application/x-www-form-urlencoded", new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        try {
                            //if (response.length() != 0) {
                            if (response.isNull("memberStatus") || response.getInt("memberStatus") == 0) {
                                responseIntent.putExtra("status", "failure");
                                logHelper.logInfo(responseIntent.getAction() + " - " + "status = success");
                            } else if (response.getInt("memberStatus") == 1) {
                                responseIntent.putExtra("status", "success");
                                logHelper.logInfo(responseIntent.getAction() + " - " + "status = success");
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


    private void saveInfos(String userName, String firstName, String lastName, String email) {
        SharedPreferences sharedPreferences = PreferencesUtil.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor;
        editor = sharedPreferences.edit();
        editor.putString(AppConstants.USER_NAME.toString(), userName);
        editor.putString(AppConstants.FIRST_NAME.toString(), firstName);
        editor.putString(AppConstants.LAST_NAME.toString(), lastName);
        editor.putString(AppConstants.EMAIL.toString(), email);
        editor.apply();
        editor.commit();
    }

}
