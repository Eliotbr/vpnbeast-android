package com.b.android.openvpn60.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.b.android.openvpn60.R;
import com.b.android.openvpn60.core.Utility;
import com.b.android.openvpn60.enums.Constants;
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


public class MemberActivity extends AppCompatActivity {
    private static final String SERVICE_URL = Constants.URL_REGISTER_MEMBER.toString();
    private static final String USER_NAME = Constants.USER_NAME.toString();
    private static final String FIRST_NAME = Constants.FIRST_NAME.toString();
    private static final String LAST_NAME = Constants.LAST_NAME.toString();
    private static final String EMAIL = Constants.EMAIL.toString();
    private static final String CLASS_TAG = MemberActivity.class.toString();

    private EditText edtFirstName;
    private EditText edtLastName;
    private EditText edtEmail;
    private Button btnSubmit;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member);
        init();
    }

    private void init() {
        edtFirstName = (EditText) this.findViewById(R.id.edtFirstName);
        edtLastName = (EditText) this.findViewById(R.id.edtLastName);
        edtEmail = (EditText) this.findViewById(R.id.edtEmail2);
        progressBar = (ProgressBar) this.findViewById(R.id.progressBarMember);
        btnSubmit = (Button) this.findViewById(R.id.btnSubmit3);
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                if (Utility.validate(edtEmail.getText().toString())) {
                    invokeWSForMember(edtFirstName.getText().toString(), edtLastName.getText().toString(),
                            edtEmail.getText().toString());
                }
                else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MemberActivity.this, "Your email address format is wrong!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void invokeWSForMember(final String firstName, final String lastName,
                                  final String email) {
        AsyncHttpClient client = new AsyncHttpClient();
        final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        String userName = this.getIntent().getStringExtra(USER_NAME);
        nameValuePairs.add(new BasicNameValuePair(USER_NAME, userName));
        nameValuePairs.add(new BasicNameValuePair(FIRST_NAME, firstName));
        nameValuePairs.add(new BasicNameValuePair(LAST_NAME, lastName));
        nameValuePairs.add(new BasicNameValuePair(EMAIL, email));
        HttpEntity entity = null;
        try {
            entity = new UrlEncodedFormEntity(nameValuePairs);
        }
        catch (UnsupportedEncodingException a) {
            Log.e(CLASS_TAG, Log.getStackTraceString(a));
        }

        client.post(getApplicationContext(), SERVICE_URL, entity, "application/x-www-form-urlencoded", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    if (response.getBoolean("status")) {
                        Toast.makeText(getApplicationContext(), "Member successfully created!", Toast.LENGTH_SHORT).show();
                        Log.i(CLASS_TAG, getString(R.string.state_member_insert));
                        MemberActivity.this.finish();
                    }
                    else{
                        Toast.makeText(getApplicationContext(), "An error occured while creating member!", Toast.LENGTH_SHORT).show();
                        edtFirstName.setText("");
                        edtLastName.setText("");
                        edtEmail.setText("");
                        MemberActivity.this.finish();
                    }
                }
                catch (JSONException ex) {
                    Toast.makeText(getApplicationContext(), getString(R.string.err_state_json), Toast.LENGTH_SHORT).show();
                    Log.e(CLASS_TAG, getString(R.string.state_exception) + " : " + Log.getStackTraceString(ex));
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
                if(statusCode == 404){
                    Toast.makeText(getApplicationContext(), getString(R.string.err_server_404), Toast.LENGTH_SHORT).show();
                }
                // When Http response code is '500'
                else if(statusCode == 500){
                    Toast.makeText(getApplicationContext(), getString(R.string.err_server_500), Toast.LENGTH_SHORT).show();
                }
                // When Http response code other than 404, 500
                else{
                    Toast.makeText(getApplicationContext(), getString(R.string.err_server_else), Toast.LENGTH_SHORT).show();
                }
            }


        });
    }
}
