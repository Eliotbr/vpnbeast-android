package com.b.android.openvpn60.service;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.HandlerThread;
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
    private static final String USER_NAME;
    private static final String USER_PASS;
    private static final String USER_UUID;
    private static final String STATUS;
    private static final String SUCCESS;
    private static final String FAILURE;
    private static final String CONTENT_TYPE;
    private static final String ERR_SERVER_404;
    private static final String ERR_SERVER_500;
    private static final String ERR_SERVER_ELSE;

    static {
        LOG_HELPER = LogHelper.getLogHelper(UserService.class.getName());
        USER_NAME = AppConstants.USER_NAME.toString();
        USER_PASS = AppConstants.USER_PASS.toString();
        USER_UUID = AppConstants.USER_UUID.toString();
        STATUS = AppConstants.STATUS.toString();
        SUCCESS = AppConstants.SUCCESS.toString();
        FAILURE = AppConstants.FAILURE.toString();
        CONTENT_TYPE = AppConstants.CONTENT_TYPE.toString();
        ERR_SERVER_404 = AppConstants.ERR_SERVER_404.toString();
        ERR_SERVER_500 = AppConstants.ERR_SERVER_500.toString();
        ERR_SERVER_ELSE = AppConstants.ERR_SERVER_ELSE.toString();
    }


    public void onCreate() {
        super.onCreate();
        // An Android handler thread internally operates on a looper.
        handlerThread = new HandlerThread("UserService.HandlerThread");
        handlerThread.start();
        context = getApplicationContext();
        // An Android service handler is a handler running on a specific background thread.
        serviceHandler = new ServiceHandler(handlerThread.getLooper());
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        serviceHandler.post(new Runnable() {
            @Override
            public void run() {
                String username = intent.getStringExtra(USER_NAME);
                String userpass = intent.getStringExtra(USER_PASS);
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
        nameValuePairs.add(new BasicNameValuePair(USER_NAME, userName));
        nameValuePairs.add(new BasicNameValuePair(USER_PASS, userPass));
        nameValuePairs.add(new BasicNameValuePair(USER_UUID, user.getUuid().toString()));
        //client.addHeader("Content-Type", "application/json");
        HttpEntity entity = null;
        try {
            entity = new UrlEncodedFormEntity(nameValuePairs);
        } catch (UnsupportedEncodingException a) {
            LOG_HELPER.logException(a);
        }
        client.post(context, ServiceConstants.URL_REGISTER.toString(), entity, CONTENT_TYPE, new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        try {
                            if (response.getBoolean(STATUS)) {
                                //loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                responseIntent.putExtra(STATUS, SUCCESS);
                                responseIntent.putExtra(AppConstants.TEMP_USER.toString(), user);
                                responseIntent.putExtra(USER_NAME, userName);
                                responseIntent.putExtra(USER_PASS, userPass);
                                saveInfos(userName);
                            } else {
                                responseIntent.putExtra(STATUS, FAILURE);
                            }
                        } catch (JSONException ex) {
                            LOG_HELPER.logException(ex);
                        }
                        stopService();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        if(statusCode == 404) {
                            LOG_HELPER.logException(context.getString(R.string.err_server_404), throwable);
                            responseIntent.putExtra(STATUS, ERR_SERVER_404);
                        } else if(statusCode == 500) {
                            LOG_HELPER.logException(context.getString(R.string.err_server_500), throwable);
                            responseIntent.putExtra(STATUS, ERR_SERVER_500);
                        } else {
                            LOG_HELPER.logException(context.getString(R.string.err_server_else), throwable);
                            responseIntent.putExtra(STATUS, ERR_SERVER_ELSE);
                        }
                        stopService();
                    }
                });
    }

    public void updateLastLogin(String userName) {
        AsyncHttpClient client = new AsyncHttpClient();
        final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair(USER_NAME, userName));
        HttpEntity entity = null;
        try {
            entity = new UrlEncodedFormEntity(nameValuePairs);
        } catch (UnsupportedEncodingException a) {
            LOG_HELPER.logException(a);
        }
        client.put(getApplicationContext(), ServiceConstants.URL_PUT.toString(), entity, "application/x-www-form-urlencoded",
                new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        try {
                            if (response.getBoolean(STATUS)) {
                                LOG_HELPER.logInfo("Update last login date = " + "OK");
                                responseIntent.putExtra(STATUS, SUCCESS);
                            } else {
                                LOG_HELPER.logInfo("Update last login date = " + "Failed");
                                responseIntent.putExtra(STATUS, FAILURE);
                            }
                        } catch (Exception ex) {
                            LOG_HELPER.logException(ex);
                        }
                        stopService();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        if(statusCode == 404) {
                            LOG_HELPER.logException(context.getString(R.string.err_server_404), throwable);
                            responseIntent.putExtra(STATUS, ERR_SERVER_404);
                        } else if(statusCode == 500) {
                            LOG_HELPER.logException(context.getString(R.string.err_server_500), throwable);
                            responseIntent.putExtra(STATUS, ERR_SERVER_500);
                        } else {
                            LOG_HELPER.logException(context.getString(R.string.err_server_else), throwable);
                            responseIntent.putExtra(STATUS, ERR_SERVER_ELSE);
                        }
                        stopService();
                    }
                });
    }

    public void doLogin(final String userName, final String userPass) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.setTimeout(8000);
        final List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair(USER_NAME, userName));
        nameValuePairs.add(new BasicNameValuePair(USER_PASS, userPass));
        HttpEntity entity = null;
        try {
            entity = new UrlEncodedFormEntity(nameValuePairs);
        } catch (UnsupportedEncodingException a) {
            LOG_HELPER.logException(a);
        }
        client.post(context, ServiceConstants.URL_LOGIN.toString(), entity, CONTENT_TYPE, new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        try {
                            if (response.getBoolean(STATUS)) {
                                User user = new User();
                                user.setUserName(userName);
                                user.setUserPass(userPass);
                                LOG_HELPER.logInfo(context.getString(R.string.state_logged_in));
                                responseIntent.putExtra(STATUS, SUCCESS);
                                responseIntent.putExtra(USER_NAME, userName);
                                responseIntent.putExtra(USER_PASS, userPass);
                                LOG_HELPER.logInfo("status = " + responseIntent.getStringExtra(STATUS));
                                LOG_HELPER.logInfo("username = " + responseIntent.getStringExtra(USER_NAME));
                                //context.startActivity(mainIntent);
                                //progressBar.setVisibility(View.GONE);
                            } else {
                                LoginActivity.errorCount++;
                                responseIntent.putExtra(STATUS, FAILURE);
                            }
                        } catch (JSONException ex) {
                            Toast.makeText(context, context.getString(R.string.err_state_json),
                                    Toast.LENGTH_SHORT).show();
                            LOG_HELPER.logException(ex);
                        }
                        stopService();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        if(statusCode == 404) {
                            LOG_HELPER.logException(context.getString(R.string.err_server_404), throwable);
                            responseIntent.putExtra(STATUS, ERR_SERVER_404);
                        } else if(statusCode == 500) {
                            LOG_HELPER.logException(context.getString(R.string.err_server_500), throwable);
                            responseIntent.putExtra(STATUS, ERR_SERVER_500);
                        } else {
                            LOG_HELPER.logException(context.getString(R.string.err_server_else), throwable);
                            responseIntent.putExtra(STATUS, ERR_SERVER_ELSE);
                        }
                        stopService();
                    }

                });
    }

    private void saveInfos(String userName) {
        SharedPreferences sharedPreferences = PreferencesUtil.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor;
        editor = sharedPreferences.edit();
        editor.putString(USER_NAME, userName);
        editor.putString(USER_PASS, null);
        editor.apply();
        editor.commit();
    }

}
