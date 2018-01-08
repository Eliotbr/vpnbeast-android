package com.b.android.openvpn60.helper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.b.android.openvpn60.R;
import com.b.android.openvpn60.activity.LoginActivity;
import com.b.android.openvpn60.activity.MainActivity;
import com.b.android.openvpn60.activity.RegisterActivity;
import com.b.android.openvpn60.constant.AppConstants;
import com.b.android.openvpn60.constant.ServiceConstants;
import com.b.android.openvpn60.model.User;
import com.b.android.openvpn60.util.ViewUtil;
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

public class LoginHelper implements Runnable {
    private LoginActivity loginActivity;
    private String userName;
    private String userPass;
    private Intent mainIntent;
    private ProgressBar progressBar;
    private static LogHelper logHelper;
    private Context context;
    private EmailHelper emailHelper;


    public LoginHelper(Context context, Intent intent, String userName, String userPass) {
        loginActivity = (LoginActivity) context;
        this.userName = userName;
        this.userPass = userPass;
        this.mainIntent = intent;
        progressBar = (ProgressBar) ((LoginActivity) context).findViewById(R.id.progressBar);
        logHelper = LogHelper.getLogHelper(LoginHelper.class.toString());
        this.context = context;
    }

    @Override
    public void run() {
        loginActivity.getIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        loginActivity.getIntent().putExtra(AppConstants.USER_NAME.toString(), userName);
        loginActivity.getIntent().putExtra(AppConstants.USER_PASS.toString(), userPass);
        //loginActivity.invokeWS(userName, userPass);
        invokeWS(userName, userPass);
        //loginActivity.getProgressBar().setVisibility(View.GONE);
        //loginActivity.startActivity(mainIntent);
    }

    public void invokeWS(final String userName, final String userPass) {
        AsyncHttpClient client = new AsyncHttpClient();
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
                                    final Intent intent = new Intent(context, MainActivity.class);
                                    context.startActivity(intent);
                                    //progressBar.setVisibility(View.GONE);
                                } else {
                                    LoginActivity.errorCount++;
                                    AlertDialog.Builder alertDialog = ViewUtil.showErrorDialog(context,
                                            context.getString(R.string.err_state_login));
                                    alertDialog.show();
                                    progressBar.setVisibility(View.INVISIBLE);
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
                                    Toast.LENGTH_LONG).show();
                            logHelper.logWarning(context.getString(R.string.err_server_404), throwable);
                        } else if(statusCode == 500) {
                            Toast.makeText(context.getApplicationContext(), context.getString(R.string.err_server_500),
                                    Toast.LENGTH_LONG).show();
                            logHelper.logWarning(context.getString(R.string.err_server_500), throwable);
                        } else {
                            Toast.makeText(context.getApplicationContext(), context.getString(R.string.err_server_else),
                                    Toast.LENGTH_LONG).show();
                            logHelper.logWarning(context.getString(R.string.err_server_else), throwable);
                        }
                    }
                });
    }

    private void sendEmail() {
        emailHelper = new EmailHelper(context, "bilalccaliskan@gmail.com", "piranha93");
        emailHelper.execute();
    }
}
