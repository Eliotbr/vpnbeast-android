package com.b.android.openvpn60.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.b.android.openvpn60.constant.AppConstants;
import com.b.android.openvpn60.core.IOpenVPNServiceInternal;
import com.b.android.openvpn60.core.OpenVPNManagement;
import com.b.android.openvpn60.core.OpenVPNService;
import com.b.android.openvpn60.core.ProfileManager;
import com.b.android.openvpn60.R;
import com.b.android.openvpn60.core.VpnStatus;
import com.b.android.openvpn60.helper.LogHelper;
import com.b.android.openvpn60.model.VpnProfile;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import com.b.android.openvpn60.core.ConnectionStatus;
import com.b.android.openvpn60.util.PreferencesUtil;


public class StatusActivity extends AppCompatActivity implements VpnStatus.StateListener, VpnStatus.ByteCountListener {

    public static final String TAG = StatusActivity.class.getName();
    private long hours = 00;
    private long minutes = 00;
    private long seconds = 00;
    private long connectTime;
    private boolean isBytesDisplayed = false;
    private static ProgressDialog progressDialog;
    private static VpnProfile vpnProfile;
    private EditText edtUser;
    private EditText edtLocation;
    private EditText edtIp;
    private EditText edtPort;
    private EditText edtProfile;
    private EditText edtDuration;
    private EditText edtBytesIn;
    private EditText edtBytesOut;
    private EditText edtStatus;
    private Button btnDisconnect;
    private static AsyncTask<Void, Void, Integer> connectionChecker;
    private static Intent intent;
    private Runnable runnable;
    private static SharedPreferences sharedPrefs;
    private static Context context;
    private IOpenVPNServiceInternal serviceInternal;
    private ServiceConnection serviceConnection;
    private String lastStatus = "";
    private boolean isDestroyed = false;
    private OpenVPNService instance;
    private LogHelper logHelper;



    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        //bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        //updateViews();
        VpnStatus.addStateListener(this);
        VpnStatus.addByteCountListener(this);
        Intent intent = new Intent(StatusActivity.this, OpenVPNService.class);
        intent.setAction(AppConstants.START_SERVICE.toString());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        init();
        updateViews();
        runnable.run();
    }


    private void init() {
        logHelper = LogHelper.getLogHelper(this);
        progressDialog = new ProgressDialog(StatusActivity.this);
        progressDialog.setProgressStyle(R.style.ProgressBar);
        btnDisconnect = (Button) this.findViewById(R.id.btnDisconnect);
        vpnProfile = (VpnProfile) getIntent().getParcelableExtra(AppConstants.RESULT_PROFILE.toString());
        logHelper.logInfo("PROFILE = " + "\n" + vpnProfile.toString());
        edtUser = (EditText) this.findViewById(R.id.edtUser);
        edtProfile = (EditText) this.findViewById(R.id.edtProfile);
        edtIp = (EditText) this.findViewById(R.id.edtIp);
        edtPort = (EditText) this.findViewById(R.id.edtPort);
        sharedPrefs = PreferencesUtil.getDefaultSharedPreferences(StatusActivity.this);
        edtStatus = (EditText) this.findViewById(R.id.edtStatus);
        edtDuration = (EditText) this.findViewById(R.id.edtDuration);
        context = this.getApplicationContext();
        edtBytesIn = (EditText) this.findViewById(R.id.edtBytesIn);
        edtBytesOut = (EditText) this.findViewById(R.id.edtBytesOut);
        edtUser.setText(sharedPrefs.getString(AppConstants.USER_NAME.toString(), null));
        edtProfile.setText(vpnProfile.name);
        edtIp.setText(vpnProfile.connections[0].serverName);
        edtPort.setText(vpnProfile.connections[0].serverPort);
        edtStatus.setText("Connecting...");
        btnDisconnect.setText("Connecting");
        edtBytesIn.setText("null");
        edtBytesOut.setText("null");
        edtDuration.setText("00:00:00");
        intent = new Intent(this, MainActivity.class);
        runnable = new Runnable() {
            @Override
            public void run() {
                serviceConnection = new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName className,
                                                   IBinder service) {
                        serviceInternal = IOpenVPNServiceInternal.Stub.asInterface(service);
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName arg0) {
                        serviceInternal = null;
                    }
                };
            }
        };
        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });
    }


    private void updateViews() {
        connectionChecker = new AsyncTask<Void, Void, Integer>() {
            @Override
            protected void onPreExecute() {
                if (VpnStatus.mLastLevel != ConnectionStatus.LEVEL_CONNECTED) {
                    progressDialog.setTitle(R.string.state_connecting);
                    progressDialog.setMessage(getString(R.string.state_msg_connecting));
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                }
            }

            @Override
            protected Integer doInBackground(Void... params) {
                while (VpnStatus.mLastLevel != ConnectionStatus.LEVEL_CONNECTED) {
                    lastStatus = StatusActivity.this.getIntent().getStringExtra("status");
                }
                return 0;
            }

            @Override
            protected void onPostExecute(Integer integer) {
                progressDialog.dismiss();
                connectTime = System.currentTimeMillis();
                edtStatus.setText(getString(R.string.state_connected));
                btnDisconnect.setText(getString(R.string.disconnect));
                btnDisconnect.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_selector_red));
                isBytesDisplayed = true;
                VpnStatus.addByteCountListener(StatusActivity.this);
                VpnStatus.addStateListener(StatusActivity.this);
                updateDuration();
                Intent intent = new Intent(StatusActivity.this, OpenVPNService.class);
                intent.setAction(AppConstants.START_SERVICE.toString());
                bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            }
        }.execute();
    }


    @Override
    protected void onPause() {
        super.onPause();
    }


    private String humanReadableByteCount(long bytes, boolean mbit) {
        if (mbit)
            bytes = bytes * 8;
        int unit = mbit ? 1000 : 1024;
        if (bytes < unit)
            return bytes + (mbit ? " bit" : " B");
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (mbit ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (mbit ? "" : "");
        if (mbit)
            return String.format(Locale.getDefault(), "%.1f %sbit", bytes / Math.pow(unit, exp), pre);
        else
            return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }


    @Override
    public void onBackPressed() {
        Toast.makeText(this, R.string.state_already_connected_msg, Toast.LENGTH_SHORT).show();
        //super.onBackPressed();
    }


    @Override
    protected void onStop() {
        super.onStop();
        VpnStatus.removeStateListener(this);
        VpnStatus.removeByteCountListener(this);

    }


    @Override
    protected void onDestroy() {
        //disconnectOnDestroy();
        unbindService(serviceConnection);
        super.onDestroy();
    }


    private void disconnectOnDestroy() {
        ProfileManager.setConntectedVpnProfileDisconnected(context);
        if (serviceInternal != null) {
            try {
                serviceInternal.stopVPN(false);
                logHelper.logInfo("Disconnection onDestroy()");
                VpnStatus.mLastLevel = ConnectionStatus.LEVEL_NOTCONNECTED;
            } catch (RemoteException e) {
                logHelper.logException(e);
            }
        }
        unbindService(serviceConnection);
    }


    @Override
    public void updateByteCount(long in, long out, long diffIn, long diffOut) {
        final String byteIn = humanReadableByteCount(in, false);
        final String sumByteIn = humanReadableByteCount(diffIn / OpenVPNManagement.mBytecountInterval, true);
        final String byteOut = humanReadableByteCount(out, false);
        final String sumByteOut = humanReadableByteCount(diffOut / OpenVPNManagement.mBytecountInterval, true);
        if (VpnStatus.mLastLevel == ConnectionStatus.LEVEL_CONNECTED) {
            StatusActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateByteTexts(byteIn, sumByteIn, byteOut, sumByteOut);

                }
            });
        }
    }


    private void updateDuration() {
        if (VpnStatus.mLastLevel == ConnectionStatus.LEVEL_CONNECTED) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    StatusActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            seconds = (System.currentTimeMillis() - connectTime) / 1000;
                            Calendar cal = Calendar.getInstance();
                            cal.set(Calendar.HOUR_OF_DAY, (int) hours);
                            cal.set(Calendar.MINUTE, (int) minutes);
                            cal.set(Calendar.SECOND, (int) seconds);
                            Date date = cal.getTime();
                            SimpleDateFormat mSimple = new SimpleDateFormat("HH:mm:ss");
                            edtDuration.setText(mSimple.format(date));
                        }
                    });
                }
            }, 0, 1000);
        }
    }


    private void updateByteTexts(String in, String ins, String out, String outs) {
        edtBytesIn.setText(ins + " / " + in);
        edtBytesOut.setText(outs + " / " + out);
    }


    private void disconnect() {
        logHelper.logInfo("Disconnect Thread is starting...");
        disconnectVpn();
    }


    private void disconnectVpn() {
        connectionChecker = new AsyncTask<Void, Void, Integer>() {
            @Override
            protected void onPreExecute() {
                progressDialog.setTitle(R.string.state_disconnecting);
                progressDialog.setMessage(getString(R.string.state_msg_disconnecting));
                progressDialog.setCancelable(false);
                progressDialog.show();
            }

            @Override
            protected Integer doInBackground(Void... params) {
                ProfileManager.setConntectedVpnProfileDisconnected(context);
                if (serviceInternal != null) {
                    try {
                        serviceInternal.stopVPN(false);
                        VpnStatus.mLastLevel = ConnectionStatus.LEVEL_NOTCONNECTED;
                    } catch (RemoteException e) {
                        logHelper.logException(e);
                    }
                }
                return 0;
            }

            @Override
            protected void onPostExecute(Integer integer) {
                AlertDialog.Builder builder = new AlertDialog.Builder(StatusActivity.this);
                builder.setTitle(getString(R.string.state_disconnected));
                String dialogMessage = getResources().getString(R.string.state_msg_disconnected);
                ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(getResources().getColor(R.color.colorAccent));
                // Initialize a new spannable string builder instance
                SpannableStringBuilder ssBuilder = new SpannableStringBuilder(dialogMessage);
                ssBuilder.setSpan(
                        foregroundColorSpan,
                        0,
                        dialogMessage.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                builder.setMessage(ssBuilder);
                progressDialog.dismiss();
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        intent.putExtra(AppConstants.DISCONNECT_VPN.toString(), true);
                        VpnStatus.removeStateListener(StatusActivity.this);
                        VpnStatus.removeByteCountListener(StatusActivity.this);
                        StatusActivity.this.setResult(Activity.RESULT_OK);
                        StatusActivity.this.finish();
                        if (isDestroyed) {
                            Intent intentMain = new Intent(StatusActivity.this, MainActivity.class);
                            intentMain.putExtra(AppConstants.RESULT_DESTROYED.toString(), isDestroyed);
                            StatusActivity.this.startActivity(intentMain);
                        }

                    }
                });
                builder.setNegativeButton("Reconnect", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent();
                        VpnProfile profile = StatusActivity.this.getIntent().
                                getParcelableExtra(AppConstants.RESULT_PROFILE.toString());
                        intent.putExtra(AppConstants.RESULT_PROFILE.toString(), (Parcelable) profile);
                        StatusActivity.this.setResult(Activity.RESULT_FIRST_USER, intent);
                        StatusActivity.this.finish();
                        //startVPN(profile);
                    }
                });
                builder.setCancelable(false);
                builder.show();
            }
        }.execute();
    }


    private ProfileManager getPM() {
        return ProfileManager.getInstance(this);
    }


    private void startVPN(VpnProfile profile) {
        getPM().saveProfile(this, profile);
        Intent intent = new Intent(this, LaunchVPN.class);
        intent.putExtra(LaunchVPN.EXTRA_KEY, profile.getUUID().toString());
        intent.setAction(Intent.ACTION_MAIN);
        startActivity(intent);
    }


    @Override
    public void updateState(String state, String logmessage, int localizedResId, ConnectionStatus level) {

    }


    @Override
    public void setConnectedVPN(String uuid) {

    }
}
