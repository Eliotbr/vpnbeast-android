package com.b.android.openvpn60.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.b.android.openvpn60.R;
import com.b.android.openvpn60.constant.AppConstants;
import com.b.android.openvpn60.constant.ServiceConstants;
import com.b.android.openvpn60.helper.LoginHelper;
import com.b.android.openvpn60.model.User;
import com.b.android.openvpn60.helper.EmailHelper;
import com.b.android.openvpn60.helper.LogHelper;
import com.b.android.openvpn60.util.PreferencesUtil;
import com.crashlytics.android.Crashlytics;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.message.BasicNameValuePair;


public class LoginActivity extends ActionBarActivity {
    private static final String SHARED_PREFS = AppConstants.SHARED_PREFS.toString();
    private static final String USER_NAME = AppConstants.USER_NAME.toString();
    private static final String USER_PASS = AppConstants.USER_PASS.toString();
    private static final String USER_CHOICE = AppConstants.USER_CHOICE.toString();
    private static final String SERVICE_URL_GET = ServiceConstants.URL_LOGIN.toString();
    private static final String CLASS_TAG = AppConstants.CLASS_TAG_ACTIVITY.toString() + LoginActivity.class.toString();

    private AlertDialog.Builder alertDialog;
    private Button btnSubmit;
    private Button btnClear;
    private EditText edtUsername;
    private EditText edtPass;
    private CheckBox chkRemember;
    private SharedPreferences sharedPreferences;
    private Intent intent;
    private Intent intentSignup;
    private TextView txtForget;
    private TextView txtSignup;
    private ProgressBar progressBar;
    private boolean isConnected = false;
    private int errorCount = 0;
    private LogHelper logHelper;
    private EmailHelper emailHelper;
    private TextView txtUsername, txtPassword;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        init();
        if (!isNetworkAvailable(getApplicationContext())) {
            showErrorDialog();
            errorCount++;
        } else
            isConnected = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.GONE);
        if (!isNetworkAvailable(getApplicationContext())) {
            showErrorDialog();
            isConnected = false;
        } else {
            isConnected = true;
            edtUsername.setText(sharedPreferences.getString(AppConstants.USER_NAME.toString(), null));
            edtPass.setText(sharedPreferences.getString(AppConstants.USER_PASS.toString(), null));
        }

    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    private void init() {
                logHelper = LogHelper.getLogHelper(this);
                edtUsername = (EditText) this.findViewById(R.id.edtUser);
                edtPass = (EditText) this.findViewById(R.id.edtPass);
                chkRemember = (CheckBox) this.findViewById(R.id.chkRemember);
                chkRemember.setShadowLayer(1, 0, 1, getResources().getColor(R.color.colorAccent));
                intent = new Intent(this, MainActivity.class); //???
                txtForget = (TextView) this.findViewById(R.id.txtForget);
                txtForget.setShadowLayer(1, 0, 1, getResources().getColor(R.color.colorAccent));
                txtSignup = (TextView) this.findViewById(R.id.txtSignup);
                txtSignup.setShadowLayer(1, 0, 1, getResources().getColor(R.color.colorAccent));
                txtForget.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendEmail();
                    }
                });
                txtSignup.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LoginActivity.this.startActivity(intentSignup);
                    }
                });
                txtUsername = (TextView) this.findViewById(R.id.txtName);
                txtUsername.setShadowLayer(1, 0, 1, getResources().getColor(R.color.colorAccent));
                txtPassword = (TextView) this.findViewById(R.id.txtPass);
                txtPassword.setShadowLayer(1, 0, 1, getResources().getColor(R.color.colorAccent));
                progressBar = (ProgressBar) this.findViewById(R.id.progressBar);
                sharedPreferences = PreferencesUtil.getDefaultSharedPreferences(LoginActivity.this);
                if (sharedPreferences.getBoolean(USER_CHOICE, false)) {
            edtUsername.setText(sharedPreferences.getString(USER_NAME, null));
            edtPass.setText(sharedPreferences.getString(USER_PASS, null));
            chkRemember.setChecked(true);
        }
        intentSignup = new Intent(this, RegisterActivity.class);
        btnClear = (Button) this.findViewById(R.id.btnClear);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*edtUsername.setText("");
                edtPass.setText("");
                chkRemember.setChecked(false);*/
                // Crashlytics test crash
                Crashlytics.getInstance().crash();
                Crashlytics.log("Crash occured");
            }
        });
        btnSubmit = (Button) this.findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected) {
                    SharedPreferences.Editor editor;
                    progressBar.setVisibility(View.VISIBLE);
                    if (chkRemember.isChecked()) {
                        editor = sharedPreferences.edit();
                        editor.putString(USER_NAME, edtUsername.getText().toString());
                        editor.putString(USER_PASS, edtPass.getText().toString());
                        editor.putBoolean(USER_CHOICE, true);
                        editor.apply();
                        editor.commit();
                        logHelper.logInfo("sharedPreferences updated with user provided values...");
                    }
                    else {
                        editor = sharedPreferences.edit();
                        editor.putString(USER_NAME, null);
                        editor.putString(USER_PASS, null);
                        editor.putBoolean(USER_CHOICE, false);
                        logHelper.logInfo("sharedPreferences updated with default values...");
                        editor.apply();
                        editor.commit();
                    }
                    final String userName = edtUsername.getText().toString();
                    final String password = edtPass.getText().toString();
                    if (!TextUtils.isEmpty(userName) && !TextUtils.isEmpty(password)) {
                        /*RequestParams params = new RequestParams();
                        params.put(USER_NAME, userName);
                        params.put(USER_PASS, password);*/
                        // Invoke RESTful Web Service with Http parameters
                        LoginHelper loginHelper = new LoginHelper(LoginActivity.this, intent, userName, password);
                        loginHelper.run();
                        /*intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra(AppConstants.TEMP_USER.toString(), userName);
                        intent.putExtra(USER_NAME, edtUsername.getText().toString());
                        startActivity(intent);*/
                    } else {
                        Toast.makeText(getApplicationContext(), getString(R.string.err_state_blank_login),
                                Toast.LENGTH_SHORT).show();
                        logHelper.logInfo("Username or password is empty, can not start new activity...");
                    }
                } else {
                    showErrorDialog();
                    logHelper.logWarning("Internet connection is a MUST for application...");
                }

            }
        });
        if (sharedPreferences.getBoolean(USER_CHOICE, false)) {
            edtUsername.setText(sharedPreferences.getString(USER_NAME, null));
            edtPass.setText(sharedPreferences.getString(USER_PASS, null));
            chkRemember.setChecked(true);
        }
    }

    public void invokeWS(final String userName, final String userPass) {
        AsyncHttpClient client = new AsyncHttpClient();
        final List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair(USER_NAME, userName));
        nameValuePairs.add(new BasicNameValuePair(USER_PASS, userPass));
        HttpEntity entity = null;
        try {
            entity = new UrlEncodedFormEntity(nameValuePairs);
        }
        catch (UnsupportedEncodingException a) {
            logHelper.logException(a);
        }
        client.post(getApplicationContext(), SERVICE_URL_GET, entity, "application/x-www-form-urlencoded",
                new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    if (response.getBoolean("status")) {
                        User user = new User();
                        user.setUserName(userName);
                        user.setUserPass(userPass);
                        Log.i(CLASS_TAG, getString(R.string.state_logged_in));
                        LoginActivity.this.startActivity(intent);
                        //progressBar.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(getApplicationContext(), getString(R.string.err_state_logged_in),
                                Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                } catch (JSONException ex) {
                    Toast.makeText(getApplicationContext(), getString(R.string.err_state_json),
                            Toast.LENGTH_SHORT).show();
                    logHelper.logException(ex);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable throwable) {
                if(statusCode == 404) {
                    Toast.makeText(getApplicationContext(), getString(R.string.err_server_404),
                            Toast.LENGTH_LONG).show();
                    logHelper.logWarning(getString(R.string.err_server_404), throwable);
                } else if(statusCode == 500) {
                    Toast.makeText(getApplicationContext(), getString(R.string.err_server_500),
                            Toast.LENGTH_LONG).show();
                    logHelper.logWarning(getString(R.string.err_server_500), throwable);
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.err_server_else),
                            Toast.LENGTH_LONG).show();
                    logHelper.logWarning(getString(R.string.err_server_else), throwable);
                }
            }
        });
    }

    private void sendEmail() {
        emailHelper = new EmailHelper(this, "bilalccaliskan@gmail.com", "piranha93");
        emailHelper.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem itm1 = menu.add("Settings");
        itm1.setNumericShortcut('1');
        itm1.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Toast.makeText(LoginActivity.this, "You selected Settings", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        MenuItem itm2 = menu.add("About us");
        itm2.setNumericShortcut('2');
        itm2.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                AlertDialog.Builder dlg = new AlertDialog.Builder(LoginActivity.this, AlertDialog.THEME_HOLO_DARK);
                dlg.setTitle(item.getTitle());
                dlg.setMessage("This is the place where we put some sort of messages.");
                dlg.setPositiveButton(android.R.string.ok, null);
                dlg.setNegativeButton(android.R.string.cancel, null);
                dlg.show();
                return false;
            }
        });
        MenuItem itm3 = menu.add("Close");
        itm3.setNumericShortcut('3');
        itm3.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                LoginActivity.this.finish();
                return false;
            }
        });
        return true;
    }

    public boolean isNetworkAvailable(Context context) {
        final ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }

    public boolean isInternetAvailable() {
        try {
            final InetAddress address = InetAddress.getByName("www.google.com");
            return !address.equals("");
        } catch (UnknownHostException e) {
            logHelper.logException(e);
        }
        return false;
    }

    private void showErrorDialog() {
        alertDialog = new AlertDialog.Builder(LoginActivity.this, AlertDialog.THEME_HOLO_DARK);
        alertDialog.setTitle("Error");
        alertDialog.setMessage("Application requires internet connection. Please check your connection.");
        alertDialog.setCancelable(false);
        alertDialog.setNeutralButton(android.R.string.ok, null);
        alertDialog.show();
    }
}