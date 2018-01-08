package com.b.android.openvpn60.helper;

import android.content.Context;
import android.content.Intent;

import com.b.android.openvpn60.activity.LoginActivity;
import com.b.android.openvpn60.constant.AppConstants;

/**
 * Created by b on 12/30/2017.
 */

public class LoginHelper implements Runnable {
    private LoginActivity loginActivity;
    private String userName;
    private String userPass;
    private Intent mainIntent;


    public LoginHelper(Context context, Intent intent, String userName, String userPass) {
        loginActivity = (LoginActivity) context;
        this.userName = userName;
        this.userPass = userPass;
        this.mainIntent = intent;
    }

    @Override
    public void run() {
        loginActivity.getIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        loginActivity.getIntent().putExtra(AppConstants.USER_NAME.toString(), userName);
        loginActivity.getIntent().putExtra(AppConstants.USER_PASS.toString(), userPass);
        loginActivity.invokeWS(userName, userPass);
        //loginActivity.getProgressBar().setVisibility(View.GONE);
        //loginActivity.startActivity(mainIntent);
    }
}
