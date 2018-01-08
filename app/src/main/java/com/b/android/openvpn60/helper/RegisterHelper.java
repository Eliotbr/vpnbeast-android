package com.b.android.openvpn60.helper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Toast;
import com.b.android.openvpn60.R;
import com.b.android.openvpn60.activity.RegisterActivity;
import com.b.android.openvpn60.constant.AppConstants;
import com.b.android.openvpn60.constant.ServiceConstants;
import com.b.android.openvpn60.model.User;
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

/**
 * Created by b on 12/30/2017.
 */

public class RegisterHelper implements Runnable {
    private RegisterActivity registerActivity;
    private String userName;
    private String userPass;
    private Intent loginIntent;
    private LogHelper logHelper;


    public RegisterHelper(Context context, Intent intent, String userName, String userPass) {
        registerActivity = (RegisterActivity) context;
        this.userName = userName;
        this.userPass = userPass;
        this.loginIntent = intent;
        logHelper = LogHelper.getLogHelper(RegisterHelper.class.toString());
    }


    @Override
    public void run() {
        invokeWS(registerActivity, userName, userPass);
    }

    public void invokeWS(final Context context, final String userName, final String userPass) {
        AsyncHttpClient client = new AsyncHttpClient();
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
                                registerActivity.getProgressBar().setVisibility(View.GONE);
                                Toast.makeText(context, context.getString(R.string.state_register) ,
                                        Toast.LENGTH_SHORT).show();
                                loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                loginIntent.putExtra(AppConstants.TEMP_USER.toString(), user);
                                loginIntent.putExtra(AppConstants.USER_NAME.toString(), userName);
                                loginIntent.putExtra(AppConstants.USER_PASS.toString(), userPass);
                                saveInfos();
                                context.startActivity(loginIntent);
                            } else {
                                registerActivity.getProgressBar().setVisibility(View.GONE);
                                Toast.makeText(context, context.getString(R.string.err_state_register),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException ex) {
                            Toast.makeText(context, context.getString(R.string.err_state_json),
                                    Toast.LENGTH_SHORT).show();
                            logHelper.logException(ex);
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, String res, Throwable throwable) {
                        if(statusCode == 404) {
                            Toast.makeText(context, context.getString(R.string.err_server_404),
                                    Toast.LENGTH_SHORT).show();
                            logHelper.logWarning(context.getString(R.string.err_server_404), throwable);
                        } else if(statusCode == 500) {
                            Toast.makeText(context, context.getString(R.string.err_server_500),
                                    Toast.LENGTH_SHORT).show();
                            logHelper.logWarning(context.getString(R.string.err_server_500), throwable);
                        } else {
                            Toast.makeText(context, context.getString(R.string.err_server_else),
                                    Toast.LENGTH_SHORT).show();
                            logHelper.logWarning(context.getString(R.string.err_server_else), throwable);
                        }
                    }
                });
    }

    private void saveInfos() {
        SharedPreferences sharedPreferences = registerActivity.getSharedPreferences(AppConstants.SHARED_PREFS.toString(),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor;
        editor = sharedPreferences.edit();
        editor.putString(AppConstants.USER_NAME.toString(), userName);
        editor.putString(AppConstants.USER_PASS.toString(), null);
        editor.apply();
        editor.commit();
    }
}
