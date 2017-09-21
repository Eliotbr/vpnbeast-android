package com.b.android.openvpn60.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.b.android.openvpn60.R;
import com.b.android.openvpn60.core.OpenVPNService;
import com.b.android.openvpn60.core.User;
import com.b.android.openvpn60.core.VpnStatus;
import com.b.android.openvpn60.enums.Constants;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HeaderElement;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.ParseException;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.entity.ContentType;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.impl.client.BasicResponseHandler;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.protocol.HTTP;
import de.blinkt.openvpn.core.ConnectionStatus;


public class RegisterActivity extends AppCompatActivity {
    private static final String USER_NAME = Constants.USER_NAME.toString();
    private static final String USER_PASS = Constants.USER_PASS.toString();
    private static final String USER_UUID = Constants.USER_UUID.toString();
    private static final String SERVICE_URL = Constants.URL_REGISTER.toString();
    private static final String CLASS_TAG = RegisterActivity.class.toString();

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
                    if (password.length() < 6 && password.equals(password2)) {
                        Toast.makeText(getApplicationContext(), getString(R.string.err_password), Toast.LENGTH_SHORT).show();
                    } else {
                        invokeWS(userName, password);
                    }
                }
                }
            });
    }

    public void invokeWS(String userName, String userPass) {
        AsyncHttpClient client = new AsyncHttpClient();
        final User user = new User(userName, userPass);
        final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair(USER_NAME, userName));
        nameValuePairs.add(new BasicNameValuePair(USER_PASS, userPass));
        nameValuePairs.add(new BasicNameValuePair(USER_UUID, user.getUuid().toString()));
        //client.addHeader("Content-Type", "application/json");
        HttpEntity entity = null;
        try {
            entity = new UrlEncodedFormEntity(nameValuePairs);
        }
        catch (UnsupportedEncodingException a) {
            Log.e(CLASS_TAG, a.getLocalizedMessage());
        }

        client.post(getApplicationContext(), SERVICE_URL, entity, "application/x-www-form-urlencoded", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    if (response.getBoolean("status")) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getApplicationContext(), getString(R.string.state_register) , Toast.LENGTH_SHORT).show();
                        intentLogin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intentLogin.putExtra(Constants.TEMP_USER.toString(), user);
                        intentLogin.putExtra(USER_NAME, edtUsername.getText().toString());
                        startActivity(intentLogin);
                    }
                    else{
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getApplicationContext(), getString(R.string.err_state_register), Toast.LENGTH_SHORT).show();
                    }
                }
                catch (JSONException ex) {
                    Toast.makeText(getApplicationContext(), getString(R.string.err_state_json), Toast.LENGTH_SHORT).show();
                    Log.e(CLASS_TAG, "Exception: " + Log.getStackTraceString(ex));
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
                if(statusCode == 404){
                    Toast.makeText(getApplicationContext(), getString(R.string.err_server_404), Toast.LENGTH_SHORT).show();
                }
                else if(statusCode == 500){
                    Toast.makeText(getApplicationContext(), getString(R.string.err_server_500), Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getApplicationContext(), getString(R.string.err_server_else), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(RegisterActivity.this, "You selected Settings", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        MenuItem itm2 = menu.add("About us");
        itm2.setNumericShortcut('2');
        itm2.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                AlertDialog.Builder dlg = new AlertDialog.Builder(RegisterActivity.this, AlertDialog.THEME_HOLO_DARK);
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

    public void close(){
        this.finish();
    }

}
