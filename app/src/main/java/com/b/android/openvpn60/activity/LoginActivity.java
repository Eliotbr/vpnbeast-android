package com.b.android.openvpn60.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
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
import com.b.android.openvpn60.helper.EmailHelper;
import com.b.android.openvpn60.helper.LogHelper;
import com.b.android.openvpn60.service.LoginService;
import com.b.android.openvpn60.util.PreferencesUtil;
import com.b.android.openvpn60.util.ViewUtil;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class LoginActivity extends ActionBarActivity {

    private EditText edtUsername;
    private EditText edtPass;
    private CheckBox chkRemember;
    private SharedPreferences sharedPreferences;
    private Intent mainIntent;
    private Intent intentSignup;
    private ProgressBar progressBar;
    private boolean isConnected = false;
    public static int errorCount = 0;
    private LogHelper logHelper;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        init();
        //getDeviceInfos();
    }


    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.INVISIBLE);

        IntentFilter filter = new IntentFilter(LoginService.ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(testReceiver, filter);

        if (!isNetworkAvailable(getApplicationContext())) {
            showErrorDialog();
            isConnected = false;
        } else {
            isConnected = true;
            edtUsername.setText(sharedPreferences.getString(AppConstants.USER_NAME.toString(), null));
            edtPass.setText(sharedPreferences.getString(AppConstants.USER_PASS.toString(), null));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(testReceiver);
    }


    /**
     * Below method created to debug for creating different views for different devices
     */
    /*private void getDeviceInfos() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        logHelper.logInfo("width = " + width);
        logHelper.logInfo("height = " + height);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int heightPixels = metrics.heightPixels;
        int widthPixels = metrics.widthPixels;
        int densityDpi = metrics.densityDpi;
        float xdpi = metrics.xdpi;
        float ydpi = metrics.ydpi;
        logHelper.logInfo("widthPixels = " + widthPixels);
        logHelper.logInfo("heightPixels = " + heightPixels);
        logHelper.logInfo("densityDpi = " + densityDpi);
    }*/


    public ProgressBar getProgressBar() {
        return progressBar;
    }


    private void init() {
        logHelper = LogHelper.getLogHelper(this);
        edtUsername = (EditText) this.findViewById(R.id.edtUser);
        edtPass = (EditText) this.findViewById(R.id.edtPass);
        chkRemember = (CheckBox) this.findViewById(R.id.chkRemember);
        chkRemember.setShadowLayer(1, 0, 1, getResources().getColor(R.color.colorAccent));
        mainIntent = new Intent(this, MainActivity.class); //???
        intentSignup = new Intent(this, RegisterActivity.class);
        //txtForget = (TextView) this.findViewById(R.id.txtForget);
        //txtForget.setShadowLayer(1, 0, 1, getResources().getColor(R.color.colorAccent));
        //txtSignup = (TextView) this.findViewById(R.id.txtSignup);
        //txtSignup.setShadowLayer(1, 0, 1, getResources().getColor(R.color.colorAccent));
        final TextView txtForget = (TextView) this.findViewById(R.id.txtForget);
        txtForget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendEmail();
            }
        });

        final TextView txtSignup = (TextView) this.findViewById(R.id.txtSignup);
        txtSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginActivity.this.startActivity(intentSignup);
            }
        });

        progressBar = (ProgressBar) this.findViewById(R.id.progressBar);
        sharedPreferences = PreferencesUtil.getDefaultSharedPreferences(LoginActivity.this);

        if (sharedPreferences.getBoolean(AppConstants.USER_CHOICE.toString(), false)) {
            edtUsername.setText(sharedPreferences.getString(AppConstants.USER_NAME.toString(), null));
            edtPass.setText(sharedPreferences.getString(AppConstants.USER_PASS.toString(), null));
            chkRemember.setChecked(true);
        }

        final Button btnClear = (Button) this.findViewById(R.id.btnClear);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edtUsername.setText("");
                edtPass.setText("");
                chkRemember.setChecked(false);
            }
        });

        final Button btnSubmit = (Button) this.findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isConnected) {
                    SharedPreferences.Editor editor;
                    progressBar.setVisibility(View.VISIBLE);
                    if (chkRemember.isChecked()) {
                        editor = sharedPreferences.edit();
                        editor.putString(AppConstants.USER_NAME.toString(), edtUsername.getText().toString());
                        editor.putString(AppConstants.USER_PASS.toString(), edtPass.getText().toString());
                        editor.putBoolean(AppConstants.USER_CHOICE.toString(), true);
                        editor.apply();
                        editor.commit();
                        logHelper.logInfo("sharedPreferences updated with user provided values...");
                    } else {
                        editor = sharedPreferences.edit();
                        editor.putString(AppConstants.USER_NAME.toString(), null);
                        editor.putString(AppConstants.USER_PASS.toString(), null);
                        editor.putBoolean(AppConstants.USER_CHOICE.toString(), false);
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
                        if (errorCount > 10) {
                            Toast.makeText(LoginActivity.this, "Login count exceeded", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.INVISIBLE);
                            final AlertDialog.Builder alertDialog = ViewUtil.showErrorDialog(LoginActivity.this,
                                    getString(R.string.err_state_login_extra));
                            alertDialog.setPositiveButton("Send Email", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    sendEmail();
                                }
                            });
                            alertDialog.setNegativeButton("Create New Account", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    final Intent intentSignup = new Intent(LoginActivity.this, RegisterActivity.class);
                                    LoginActivity.this.startActivity(intentSignup);
                                }
                            });
                            alertDialog.show();
                        }
                        else
                            startLoginService(userName, password);
                    } else {
                        AlertDialog.Builder alertDialog = ViewUtil.showErrorDialog(LoginActivity.this,
                                LoginActivity.this.getString(R.string.err_state_empty_fields));
                        alertDialog.show();
                        progressBar.setVisibility(View.INVISIBLE);
                        logHelper.logInfo("Username or password is empty, can not start new activity...");
                    }
                } else {
                    showErrorDialog();
                    logHelper.logWarning("Internet connection is a MUST for application...");
                }
            }
        });

        if (sharedPreferences.getBoolean(AppConstants.USER_CHOICE.toString(), false)) {
            edtUsername.setText(sharedPreferences.getString(AppConstants.USER_NAME.toString(), null));
            edtPass.setText(sharedPreferences.getString(AppConstants.USER_PASS.toString(), null));
            chkRemember.setChecked(true);
        }
    }

    private void sendEmail() {
        final EmailHelper emailHelper = new EmailHelper(this, "bilalccaliskan@gmail.com", "piranha93");
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
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(LoginActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        alertDialog.setTitle("Error");
        alertDialog.setMessage("Application requires internet connection. Please check your connection.");
        alertDialog.setCancelable(false);
        alertDialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isConnected = isNetworkAvailable(LoginActivity.this);
            }
        });
        alertDialog.show();
    }

    public void startLoginService(String userName, String userPass) {
        // Construct our Intent specifying the Service
        Intent i = new Intent(this, LoginService.class);
        // Add extras to the bundle
        i.putExtra(AppConstants.USER_NAME.toString(), userName);
        i.putExtra(AppConstants.USER_PASS.toString(), userPass);
        // Start the service
        startService(i);
    }

    // Define the callback for what to do when message is received
    private BroadcastReceiver testReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String result = intent.getStringExtra("status");
            logHelper.logInfo(result);
            if (result.equals("success")) {
                mainIntent.putExtra(AppConstants.USER_NAME.toString(), intent.getStringExtra(AppConstants.USER_NAME.toString()));
                mainIntent.putExtra(AppConstants.USER_PASS.toString(), intent.getStringExtra(AppConstants.USER_PASS.toString()));
                LoginActivity.this.startActivity(mainIntent);
            } else if (result.equals("failure")) {
                AlertDialog.Builder alertDialog = ViewUtil.showErrorDialog(LoginActivity.this,
                        LoginActivity.this.getString(R.string.err_state_login));
                alertDialog.show();
                progressBar.setVisibility(View.INVISIBLE);
            } else if (result.equals("err_server_404")){
                AlertDialog.Builder alertDialog = ViewUtil.showErrorDialog(LoginActivity.this,
                        "\nServer returned HTTP 404 error code");
                alertDialog.show();
                progressBar.setVisibility(View.INVISIBLE);
            } else if (result.equals("err_server_500")){
                AlertDialog.Builder alertDialog = ViewUtil.showErrorDialog(LoginActivity.this,
                        "\nServer returned HTTP 500 error code");
                alertDialog.show();
                progressBar.setVisibility(View.INVISIBLE);
            } else if (result.equals("err_server_else")){
                AlertDialog.Builder alertDialog = ViewUtil.showErrorDialog(LoginActivity.this,
                        "\nServer returned an error code");
                alertDialog.show();
                progressBar.setVisibility(View.INVISIBLE);
            }

        }
    };

}