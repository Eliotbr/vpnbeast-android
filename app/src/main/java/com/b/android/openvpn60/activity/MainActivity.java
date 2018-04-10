package com.b.android.openvpn60.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.test.mock.MockPackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.b.android.openvpn60.constant.AppConstants;
import com.b.android.openvpn60.constant.ServiceConstants;
import com.b.android.openvpn60.model.VpnProfile;
import com.b.android.openvpn60.adapter.CustomAdapter;
import com.b.android.openvpn60.core.ProfileManager;
import com.b.android.openvpn60.R;
import com.b.android.openvpn60.fragment.ServerSelectFragment;
import com.b.android.openvpn60.helper.LogHelper;
import com.b.android.openvpn60.service.LocationService;
import com.b.android.openvpn60.service.MemberService;
import com.b.android.openvpn60.service.ServerService;
import com.b.android.openvpn60.service.UserService;
import com.b.android.openvpn60.util.PreferencesUtil;
import com.b.android.openvpn60.util.ViewUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.json.JSONObject;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.message.BasicNameValuePair;


public class MainActivity extends AppCompatActivity {

    private final String CLASS_TAG = AppConstants.CLASS_TAG_ACTIVITY.toString() + this.getClass().toString();
    private static final int PERMISSION_REQUEST = 23621;

    private SharedPreferences sharedPrefs;
    private Intent importer;
    public static VpnProfile profile;
    private EditText edtPort;
    private EditText edtUser;
    private EditText edtHost;
    private Button btnConnect;
    private ProgressDialog dlgProgress;
    private Intent intentService;
    public static List<VpnProfile> profiles;
    private ArrayAdapter<VpnProfile> adapter;
    private CustomAdapter customAdapter;
    private Button btnSelect;
    private AsyncTask<Void, Void, Integer> tasker;
    private String userName;
    private boolean isMember = false;
    private boolean isServerstaken = false;
    private TextView txtProfileName;
    private Spinner spinner;
    private FragmentTransaction frTransaction;
    private ServerSelectFragment mFragment;
    private RelativeLayout pnlMain;
    private LogHelper logHelper;
    private TextView txtUsername, txtProfile, txtIp, txtPort;
    private ProgressBar progressBar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        prepareService();
        updateViewForFirst();
        ActionBar mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(false);
        LayoutInflater mInflater = LayoutInflater.from(this);
        View mCustomView = mInflater.inflate(R.layout.custom_menu, null);
        TextView mTitleTextView = mCustomView.findViewById(R.id.title_text);
        mTitleTextView.setText(userName);
        mActionBar.setCustomView(mCustomView);
        mActionBar.setDisplayShowCustomEnabled(true);
    }


    private void init() {
        logHelper = LogHelper.getLogHelper(this);
        progressBar = this.findViewById(R.id.progressBar);
        btnConnect = this.findViewById(R.id.btnConnect);
        importer = new Intent(this, ImportActivity.class);
        intentService = new Intent(this, LaunchVPN.class);
        edtHost = this.findViewById(R.id.edtIP);
        edtHost.setShadowLayer(1, 0, 1, getResources().getColor(R.color.colorAccent));
        edtUser = this.findViewById(R.id.edtUser);
        edtUser.setShadowLayer(1, 0, 1, getResources().getColor(R.color.colorAccent));
        edtPort = this.findViewById(R.id.edtPort);
        edtPort.setShadowLayer(1, 0, 1, getResources().getColor(R.color.colorAccent));
        txtProfileName = this.findViewById(R.id.txtProfileName);
        txtUsername = this.findViewById(R.id.txtName);
        txtUsername.setShadowLayer(1, 0, 1, getResources().getColor(R.color.colorAccent));
        txtProfile = this.findViewById(R.id.txtLocation);
        txtProfile.setShadowLayer(1, 0, 1, getResources().getColor(R.color.colorAccent));
        txtIp = this.findViewById(R.id.txtIp);
        txtIp.setShadowLayer(1, 0, 1, getResources().getColor(R.color.colorAccent));
        txtPort = this.findViewById(R.id.txtPort);
        txtPort.setShadowLayer(1, 0, 1, getResources().getColor(R.color.colorAccent));
        sharedPrefs = PreferencesUtil.getDefaultSharedPreferences(MainActivity.this);
        userName = getIntent().getStringExtra(AppConstants.USER_NAME.toString());
        edtUser.setText(userName);
        pnlMain = this.findViewById(R.id.activity_main);
        //profile = ProfileManager.get(getApplicationContext(), getIntent().getStringExtra(AppConstants.EXTRA_KEY.toString()));
        //user = (User) intentMain.getSerializableExtra(AppConstants.TEMP_USER.toString());
        //userName = sharedPrefs.getString(AppConstants.USER_NAME.toString(), null);
        btnSelect = this.findViewById(R.id.btnSelect);
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                frTransaction = getFragmentManager().beginTransaction();
                mFragment = new ServerSelectFragment();
                frTransaction.replace(android.R.id.content, mFragment);
                frTransaction.commit();
            }
        });
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (profile == null) {
                    String mErr = getString(R.string.err_noprofile);
                    Toast.makeText(MainActivity.this, mErr, Toast.LENGTH_LONG).show();
                } else {
                    dlgProgress = new ProgressDialog(MainActivity.this, AlertDialog.THEME_HOLO_DARK);
                    dlgProgress.setTitle(R.string.state_importing);
                    final Intent mStatus = new Intent(MainActivity.this, StatusActivity.class);
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mStatus.putExtra(AppConstants.RESULT_PROFILE.toString(), (Parcelable) profile);
                            startOrStopVPN(profile);
                        }
                    });
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem itm3 = menu.add("Register");
        itm3.setNumericShortcut('3');
        itm3.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        itm3.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (isMember) {
                    Toast.makeText(MainActivity.this, "You are already a valid member!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(MainActivity.this, MemberActivity.class);
                    intent.putExtra(AppConstants.USER_NAME.toString(), userName);
                    MainActivity.this.startActivity(intent);
                }
                return false;
            }
        });
        MenuItem itm4 = menu.add(getString(R.string.prompt_sort));
        itm4.setNumericShortcut('3');
        itm4.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        itm4.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                dlgProgress = new ProgressDialog(MainActivity.this, AlertDialog.THEME_HOLO_DARK);
                dlgProgress.setTitle(getString(R.string.sorting_profiles));
                dlgProgress.setMessage(getString(R.string.sorting_profiles_msg));
                dlgProgress.setCancelable(false);
                dlgProgress.show();
                Thread mThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (VpnProfile profile : profiles) {
                            long startTime = System.nanoTime();
                            executeCmd("ping -c 2 -w 2 " + profile.connections[0].serverName, false);
                            long time = System.nanoTime() - startTime;
                            Log.i(CLASS_TAG, "runtime: " + time);
                            profile.ping = time;
                        }
                        sortBySpeed();
                    }
                });
                mThread.start();
                return false;
            }
        });
        MenuItem itm5 = menu.add(getString(R.string.prompt_displayLocation));
        itm5.setNumericShortcut('4');
        itm5.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        itm5.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                checkPermission();
                return false;
            }
        });
        MenuItem itm6 = menu.add(getString(R.string.prompt_logout));
        itm6.setNumericShortcut('5');
        itm6.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        itm6.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //FirebaseAuth.getInstance().signOut();
                MainActivity.this.finish();
                return false;
            }
        });
        return true;
    }


    private ProfileManager getPM() {
        return ProfileManager.getInstance(this);
    }


    private void checkPermission() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != MockPackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2);
            } else
                startLocationService();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void prepareService() {
        //startUserService(userName);
        //startMemberService(userName);
        //startServerService();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (getIntent().getParcelableExtra(AppConstants.RESULT_PROFILE.toString()) != null)
            profile = getIntent().getParcelableExtra(AppConstants.RESULT_PROFILE.toString());
        if (progressBar.getVisibility() != View.INVISIBLE)
            progressBar.setVisibility(View.INVISIBLE);
        IntentFilter memberFilter = new IntentFilter(AppConstants.CHECK_MEMBER.toString());
        LocalBroadcastManager.getInstance(this).registerReceiver(memberReceiver, memberFilter);
        IntentFilter serverFilter = new IntentFilter(AppConstants.GET_VPN_PROFILES.toString());
        LocalBroadcastManager.getInstance(this).registerReceiver(serverReceiver, serverFilter);
        IntentFilter userFilter = new IntentFilter(AppConstants.UPDATE_LAST_LOGIN.toString());
        LocalBroadcastManager.getInstance(this).registerReceiver(userReceiver, userFilter);
        IntentFilter locationFilter = new IntentFilter(AppConstants.GET_LOCATION.toString());
        LocalBroadcastManager.getInstance(this).registerReceiver(locationReceiver, locationFilter);
    }


    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(memberReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serverReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(userReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver);
    }


    private void startOrStopVPN(VpnProfile profile) {
        startVPN(profile);
    }


    public void updateViews() {
        if (getIntent().getParcelableExtra(AppConstants.RESULT_PROFILE.toString()) != null) {
            profile = getIntent().getParcelableExtra(AppConstants.RESULT_PROFILE.toString());
            //intentService.putExtra(LaunchVPN.EXTRA_KEY, profile.getUUIDString());
            btnConnect.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_selector_green));
            //edtUser.setText(userName);
            edtHost.setText(profile.connections[0].serverName);
            edtPort.setText(profile.connections[0].serverPort);
        } else {
            //edtUser.setText(userName);
            btnConnect.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_selector_grey));
        }
    }


    private void updateViewForFirst() {
            btnConnect.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_selector_grey));
    }


    @Override
    public void onBackPressed() {
        if (frTransaction != null) {
            if (!frTransaction.isEmpty()) {
                getFragmentManager().beginTransaction().remove(mFragment).commit();
                updateViews();
                pnlMain.setVisibility(View.VISIBLE);
            } else
                Toast.makeText(MainActivity.this, getString(R.string.msg_logout), Toast.LENGTH_SHORT).show();
        } else
            Toast.makeText(MainActivity.this, getString(R.string.msg_logout), Toast.LENGTH_SHORT).show();
    }


    private void startVPN(VpnProfile profile) {
        getPM().saveProfile(this, profile);
        Intent intent = new Intent(this, LaunchVPN.class);
        intent.putExtra(LaunchVPN.EXTRA_KEY, profile.getUUID().toString());
        intent.setAction(Intent.ACTION_MAIN);
        startActivity(intent);
    }


    private void sortBySpeed() {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Map<String, Long> longMap = new HashMap<>();
                ArrayList<VpnProfile> profilesSorted = new ArrayList<>();
                for (VpnProfile tempProfile : profiles) {
                    longMap.put(tempProfile.connections[0].serverName,
                            tempProfile.getPing());
                }
                ArrayList<Long> longList = new ArrayList<>(longMap.values());
                Collections.sort(longList);
                ArrayList<String> nameList = new ArrayList<>();
                for (int i = 0; i < longMap.size(); i++) {
                    String key = getKeyByValue(longMap, longList.get(i));
                    nameList.add(key);
                }
                for (String value : nameList) {
                    for (int i = 0; i < profiles.size(); i++) {
                        if (profiles.get(i).connections[0].serverName.equals(value)) {
                            profilesSorted.add(profiles.get(i));
                        }
                    }
                }
                profiles = profilesSorted;
                dlgProgress.dismiss();
                Toast.makeText(MainActivity.this, getString(R.string.state_sorted_profiles),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }


    public boolean executeCmd(String cmd, boolean sudo) {
        try {
            java.lang.Process p;
            if (!sudo)
                p = Runtime.getRuntime().exec(cmd);
            else
                p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String s;
            String res = "";
            while ((s = stdInput.readLine()) != null)
                res += s + "\n";
            logHelper.logInfo("executeCmd " + res);
            int exitValue = p.waitFor();
            return (exitValue == 0);
        } catch (Exception e) {
            logHelper.logException(e);
        }
        return false;
    }


    private void startUserService(String userName) {
        Intent i = new Intent(this, UserService.class);
        i.setAction(AppConstants.UPDATE_LAST_LOGIN.toString());
        i.putExtra(AppConstants.USER_NAME.toString(), userName);
        startService(i);
    }


    private BroadcastReceiver userReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String result = intent.getStringExtra("status");
            AlertDialog.Builder alertDialog;
            if (action != null && action.equals(AppConstants.UPDATE_LAST_LOGIN.toString())) {
                switch (result) {
                    case "success":
                        Toast.makeText(MainActivity.this, "Update last login = true", Toast.LENGTH_SHORT).show();
                        break;

                    case "failure":
                        isMember = false;
                        Toast.makeText(MainActivity.this, "An error occured while updating last login date",
                                Toast.LENGTH_LONG).show();
                        break;

                    case "errServer404":
                        alertDialog = ViewUtil.showErrorDialog(MainActivity.this,
                                "\nServer returned HTTP 404 error code");
                        alertDialog.show();
                        progressBar.setVisibility(View.INVISIBLE);
                        break;

                    case "errServer500":
                        alertDialog = ViewUtil.showErrorDialog(MainActivity.this,
                                "\nServer returned HTTP 500 error code");
                        alertDialog.show();
                        progressBar.setVisibility(View.INVISIBLE);
                        break;

                    case "errServerElse":
                        alertDialog = ViewUtil.showErrorDialog(MainActivity.this,
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


    private void startMemberService(String userName) {
        Intent i = new Intent(this, MemberService.class);
        i.setAction(AppConstants.CHECK_MEMBER.toString());
        i.putExtra(AppConstants.USER_NAME.toString(), userName);
        startService(i);
    }


    private BroadcastReceiver memberReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String result = intent.getStringExtra("status");
            AlertDialog.Builder alertDialog;
            if (action != null && action.equals(AppConstants.CHECK_MEMBER.toString())) {
                switch (result) {
                    case "success":
                        isMember = true;
                        Toast.makeText(MainActivity.this, "Member validation = OK", Toast.LENGTH_LONG).show();
                        break;

                    case "failure":
                        isMember = false;
                        Toast.makeText(MainActivity.this, "You are not a member, you can register " + "\n" +
                                "from options menu", Toast.LENGTH_LONG).show();
                        break;

                    case "errServer404":
                        alertDialog = ViewUtil.showErrorDialog(MainActivity.this,
                                "\nServer returned HTTP 404 error code");
                        alertDialog.show();
                        progressBar.setVisibility(View.INVISIBLE);
                        break;

                    case "errServer500":
                        alertDialog = ViewUtil.showErrorDialog(MainActivity.this,
                                "\nServer returned HTTP 500 error code");
                        alertDialog.show();
                        progressBar.setVisibility(View.INVISIBLE);
                        break;

                    case "errServerElse":
                        alertDialog = ViewUtil.showErrorDialog(MainActivity.this,
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


    private void startServerService() {
        Intent i = new Intent(this, ServerService.class);
        i.setAction(AppConstants.GET_VPN_PROFILES.toString());
        logHelper.logInfo("Creating ServerService...");
        startService(i);
    }


    private BroadcastReceiver serverReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            profiles = new ArrayList<>();
            //Bundle bundle = intent.getBundleExtra(AppConstants.BUNDLE_VPN_PROFILES.toString());
            if (action != null && action.equals(AppConstants.GET_VPN_PROFILES.toString())) {
                logHelper.logInfo("ArrayList successfully fetched!");
                Bundle bundle = intent.getBundleExtra(AppConstants.BUNDLE_VPN_PROFILES.toString());
                profiles = bundle.getParcelableArrayList(AppConstants.VPN_PROFILES.toString());
            }
        }
    };


    private void startLocationService() {
        Intent i = new Intent(this, LocationService.class);
        i.setAction(AppConstants.GET_LOCATION.toString());
        logHelper.logInfo("Creating LocationService...");
        startService(i);
    }


    private BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(AppConstants.GET_LOCATION.toString())) {
                logHelper.logInfo("Location successfully fetched!");
                logHelper.logInfo("Latitude = " + intent.getDoubleExtra("latitude", 0));
                logHelper.logInfo("Longitude = " + intent.getDoubleExtra("longitude", 0));

                Toast.makeText(MainActivity.this, "Latitude = " + String.valueOf(intent.getDoubleExtra("latitude", 0)) +
                        "\n" + "Longitude = " + String.valueOf(intent.getDoubleExtra("longitude", 0)), Toast.LENGTH_SHORT).show();
            }
        }
    };


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 0) {
            startOrStopVPN((VpnProfile) data.getParcelableExtra(AppConstants.RESULT_PROFILE.toString()));
        }
    }
}