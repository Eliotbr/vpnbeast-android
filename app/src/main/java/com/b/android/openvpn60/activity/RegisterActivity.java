package com.b.android.openvpn60.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.b.android.openvpn60.R;
import com.b.android.openvpn60.constant.Constants;
import com.b.android.openvpn60.helper.RegisterHelper;
import com.b.android.openvpn60.helper.LogHelper;


public class RegisterActivity extends AppCompatActivity {
    private static final String USER_NAME = Constants.USER_NAME.toString();
    private static final String USER_PASS = Constants.USER_PASS.toString();
    private static final String USER_UUID = Constants.USER_UUID.toString();
    private static final String SERVICE_URL = Constants.URL_REGISTER.toString();
    private static final String CLASS_TAG = RegisterActivity.class.toString();

    private LogHelper logHelper;
    private EditText edtUsername;
    private EditText edtPass;
    private EditText edtPass2;
    private Button btnSubmit;
    private Button btnClear;
    private Intent intentLogin;
    private ProgressBar progressBar;
    private String userName;
    private AsyncTask<Void, Void, Integer> checker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        logHelper = LogHelper.getLogHelper(this);
        edtUsername = (EditText) this.findViewById(R.id.edtEmail2);
        edtPass = (EditText) this.findViewById(R.id.edtPassSignup);
        edtPass2 = (EditText) this.findViewById(R.id.edtPassSignup2);
        progressBar = (ProgressBar) this.findViewById(R.id.progressBar);
        intentLogin = new Intent(this, LoginActivity.class);
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
                userName = edtUsername.getText().toString();
                if (!username.equals("") && !password.equals("") && !password2.equals("")) {
                    if (password.length() < 6)
                        Toast.makeText(getApplicationContext(), getString(R.string.err_password),
                                Toast.LENGTH_SHORT).show();
                    if (password.equals(password2)) {
                        RegisterHelper registerHelper = new RegisterHelper(RegisterActivity.this,
                                intentLogin, username, password);
                        registerHelper.run();
                    }
                    else {
                        Toast.makeText(RegisterActivity.this, "Passwords do not match, please check",
                                Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        logHelper.logWarning("Passwords does not match...");
                    }
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
        progressBar.setVisibility(View.GONE);
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public void close(){
        this.finish();
    }
}
