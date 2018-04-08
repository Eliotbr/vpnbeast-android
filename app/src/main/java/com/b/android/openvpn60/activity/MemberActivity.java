package com.b.android.openvpn60.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import com.b.android.openvpn60.service.MemberService;
import com.b.android.openvpn60.util.EmailUtil;
import com.b.android.openvpn60.helper.LogHelper;
import com.b.android.openvpn60.util.PreferencesUtil;
import com.b.android.openvpn60.util.ViewUtil;


public class MemberActivity extends AppCompatActivity {

    private EditText edtFirstName;
    private EditText edtLastName;
    private EditText edtEmail;
    private Button btnSubmit;
    private ProgressBar progressBar;
    private LogHelper logHelper;
    private SharedPreferences sharedPreferences;
    private Intent mainIntent;
    public int errorCount = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member);
        init();
    }


    private void init() {
        logHelper = LogHelper.getLogHelper(this);
        sharedPreferences = PreferencesUtil.getDefaultSharedPreferences(this);
        mainIntent = new Intent(this, MainActivity.class);
        edtFirstName = (EditText) this.findViewById(R.id.edtFirstName);
        edtLastName = (EditText) this.findViewById(R.id.edtLastName);
        edtEmail = (EditText) this.findViewById(R.id.edtEmail2);
        progressBar = (ProgressBar) this.findViewById(R.id.progressBarMember);
        btnSubmit = (Button) this.findViewById(R.id.btnSubmit3);
        btnSubmit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                String username = sharedPreferences.getString(AppConstants.USER_NAME.toString(), null);
                String firstName = edtFirstName.getText().toString();
                String lastName = edtLastName.getText().toString();
                String email = edtEmail.getText().toString();
                if (!TextUtils.isEmpty(firstName) && !TextUtils.isEmpty(lastName) && !TextUtils.isEmpty(email)) {
                    if (EmailUtil.validateEmail(edtEmail.getText().toString()))
                        startMembershipService(username, firstName, lastName, email);
                    else {
                        progressBar.setVisibility(View.GONE);
                        final AlertDialog.Builder alertDialog = ViewUtil.showErrorDialog(MemberActivity.this,
                                getString(R.string.err_email_format));
                        alertDialog.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        });
                        alertDialog.show();
                    }
                } else {
                    progressBar.setVisibility(View.GONE);
                    final AlertDialog.Builder alertDialog = ViewUtil.showErrorDialog(MemberActivity.this,
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
                Toast.makeText(MemberActivity.this, "You selected Settings", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        MenuItem itm2 = menu.add("About us");
        itm2.setNumericShortcut('2');
        itm2.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                AlertDialog.Builder dlg = new AlertDialog.Builder(MemberActivity.this, AlertDialog.THEME_HOLO_DARK);
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
                MemberActivity.this.finish();
                return false;
            }
        });
        return true;
    }


    public void startMembershipService(String userName, String firstName, String lastName, String email) {
        Intent i = new Intent(this, MemberService.class);
        i.setAction(AppConstants.INSERT_MEMBER.toString());
        i.putExtra(AppConstants.USER_NAME.toString(), userName);
        i.putExtra(AppConstants.FIRST_NAME.toString(), firstName);
        i.putExtra(AppConstants.LAST_NAME.toString(), lastName);
        i.putExtra(AppConstants.EMAIL.toString(), email);
        startService(i);
    }


    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(AppConstants.INSERT_MEMBER.toString());
        LocalBroadcastManager.getInstance(this).registerReceiver(testReceiver, filter);
    }


    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(testReceiver);
    }


    // Define the callback for what to do when message is received
    private BroadcastReceiver testReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String result = intent.getStringExtra("status");
            if (action != null && action.equals(AppConstants.INSERT_MEMBER.toString())) {
                if (result.equals("success")) {
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    mainIntent.putExtra(AppConstants.USER_NAME.toString(), intent.getStringExtra(AppConstants.USER_NAME.toString()));
                    mainIntent.putExtra(AppConstants.FIRST_NAME.toString(), intent.getStringExtra(AppConstants.FIRST_NAME.toString()));
                    mainIntent.putExtra(AppConstants.LAST_NAME.toString(), intent.getStringExtra(AppConstants.LAST_NAME.toString()));
                    mainIntent.putExtra(AppConstants.EMAIL.toString(), intent.getStringExtra(AppConstants.EMAIL.toString()));
                    MemberActivity.this.startActivity(mainIntent);
                } else if (result.equals("failure")) {
                    AlertDialog.Builder alertDialog = ViewUtil.showErrorDialog(MemberActivity.this,
                            MemberActivity.this.getString(R.string.err_state_membership));
                    alertDialog.show();
                    progressBar.setVisibility(View.INVISIBLE);
                } else if (result.equals("errServer404")){
                    AlertDialog.Builder alertDialog = ViewUtil.showErrorDialog(MemberActivity.this,
                            "\nServer returned HTTP 404 error code");
                    alertDialog.show();
                    progressBar.setVisibility(View.INVISIBLE);
                } else if (result.equals("errServer500")){
                    AlertDialog.Builder alertDialog = ViewUtil.showErrorDialog(MemberActivity.this,
                            "\nServer returned HTTP 500 error code");
                    alertDialog.show();
                    progressBar.setVisibility(View.INVISIBLE);
                } else if (result.equals("errServerElse")){
                    AlertDialog.Builder alertDialog = ViewUtil.showErrorDialog(MemberActivity.this,
                            "\nServer returned an error code");
                    alertDialog.show();
                    progressBar.setVisibility(View.INVISIBLE);
                }
            }
        }
    };

}