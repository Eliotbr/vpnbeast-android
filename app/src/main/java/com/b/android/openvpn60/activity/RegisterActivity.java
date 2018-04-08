package com.b.android.openvpn60.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.b.android.openvpn60.R;
import com.b.android.openvpn60.constant.AppConstants;
import com.b.android.openvpn60.helper.LogHelper;
import com.b.android.openvpn60.service.UserService;
import com.b.android.openvpn60.util.ViewUtil;


public class RegisterActivity extends AppCompatActivity {

    private LogHelper logHelper;
    private EditText edtUsername;
    private EditText edtPass;
    private EditText edtPass2;
    private Button btnSubmit;
    private Button btnClear;
    private Intent loginIntent;
    private ProgressBar progressBar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        logHelper = LogHelper.getLogHelper(this);
        edtUsername = (EditText) this.findViewById(R.id.edtEmail2);
        edtPass = (EditText) this.findViewById(R.id.edtPassSignup);
        edtPass2 = (EditText) this.findViewById(R.id.edtPassSignup2);
        progressBar = (ProgressBar) this.findViewById(R.id.progressBar);
        loginIntent = new Intent(this, LoginActivity.class);
        btnClear = (Button) this.findViewById(R.id.btnClear2);
        btnSubmit = (Button) this.findViewById(R.id.btnSubmit2);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edtUsername.setText("");
                edtPass.setText("");
                edtPass2.setText("");
            }
        });
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = edtUsername.getText().toString();
                String password = edtPass.getText().toString();
                String password2 = edtPass2.getText().toString();
                progressBar.setVisibility(View.VISIBLE);
                if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password) && !TextUtils.isEmpty(password2)) {
                    if (password.equals(password2)) {
                        if (password.length() > 6)
                            startRegisterService(username, password);
                        else {
                            progressBar.setVisibility(View.GONE);
                            final AlertDialog.Builder alertDialog = ViewUtil.showErrorDialog(RegisterActivity.this,
                                    getString(R.string.err_password));
                            alertDialog.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            });
                            alertDialog.show();
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        final AlertDialog.Builder alertDialog = ViewUtil.showErrorDialog(RegisterActivity.this,
                                getString(R.string.err_password_not_same));
                        alertDialog.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        });
                        alertDialog.show();
                    }
                } else {
                    progressBar.setVisibility(View.GONE);
                    final AlertDialog.Builder alertDialog = ViewUtil.showErrorDialog(RegisterActivity.this,
                            getString(R.string.err_state_empty_fields));
                    alertDialog.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });
                    alertDialog.show();
                    logHelper.logInfo("Required fields can not be empty for registration");
                }
                }
            });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem itm1 = menu.add("Settings");
        itm1.setNumericShortcut('1');
        itm1.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Toast.makeText(RegisterActivity.this, "You selected Settings",
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        MenuItem itm2 = menu.add("About us");
        itm2.setNumericShortcut('2');
        itm2.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                AlertDialog.Builder dlg = new AlertDialog.Builder(RegisterActivity.this,
                        AlertDialog.THEME_HOLO_DARK);
                dlg.setTitle(item.getTitle());
                dlg.setMessage("This is the place where we put some sort of messages.");
                dlg.setPositiveButton("OK", null);
                dlg.setNegativeButton("NOT OK", null);
                dlg.show();
                return false;
            }
        });
        MenuItem itm3 = menu.add("Close");
        itm3.setNumericShortcut('3');
        itm3.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                close();
                return false;
            }
        });
        return true;
    }


    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.INVISIBLE);
        IntentFilter filter = new IntentFilter(AppConstants.INSERT_USER.toString());
        LocalBroadcastManager.getInstance(this).registerReceiver(testReceiver, filter);
    }


    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(testReceiver);
    }


    public ProgressBar getProgressBar() {
        return progressBar;
    }


    public void close(){
        this.finish();
    }


    public void startRegisterService(String userName, String userPass) {
        Intent i = new Intent(this, UserService.class);
        i.setAction(AppConstants.INSERT_USER.toString());
        i.putExtra(AppConstants.USER_NAME.toString(), userName);
        i.putExtra(AppConstants.USER_PASS.toString(), userPass);
        startService(i);
    }


    private BroadcastReceiver testReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String result = intent.getStringExtra("status");
            String action = intent.getAction();
            AlertDialog.Builder alertDialog;
            if (action != null && action.equals(AppConstants.INSERT_USER.toString())) {
                switch (result) {
                    case "success":
                        loginIntent.putExtra(AppConstants.USER_NAME.toString(), intent.getStringExtra(AppConstants.USER_NAME.toString()));
                        loginIntent.putExtra(AppConstants.USER_PASS.toString(), intent.getStringExtra(AppConstants.USER_PASS.toString()));
                        progressBar.setVisibility(View.INVISIBLE);
                        loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        Toast.makeText(RegisterActivity.this, R.string.state_register, Toast.LENGTH_LONG).show();
                        RegisterActivity.this.startActivity(loginIntent);
                        RegisterActivity.this.finish();
                        break;

                    case "failure":
                        alertDialog = ViewUtil.showErrorDialog(RegisterActivity.this,
                                RegisterActivity.this.getString(R.string.err_state_registration));
                        alertDialog.show();
                        progressBar.setVisibility(View.INVISIBLE);
                        break;

                    case "errServer404":
                        alertDialog = ViewUtil.showErrorDialog(RegisterActivity.this,
                                "\nServer returned HTTP 404 error code");
                        alertDialog.show();
                        progressBar.setVisibility(View.INVISIBLE);
                        break;

                    case "errServer500":
                        alertDialog = ViewUtil.showErrorDialog(RegisterActivity.this,
                                "\nServer returned HTTP 500 error code");
                        alertDialog.show();
                        progressBar.setVisibility(View.INVISIBLE);
                        break;

                    case "errServerElse":
                        alertDialog = ViewUtil.showErrorDialog(RegisterActivity.this,
                                "\nServer returned an error code");
                        alertDialog.show();
                        progressBar.setVisibility(View.INVISIBLE);
                        break;

                    default:
                        throw new IllegalArgumentException("Invalid response from service = " + result);

                }
            }
        }
    };
}
