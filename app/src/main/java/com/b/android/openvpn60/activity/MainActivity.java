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
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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
import com.b.android.openvpn60.model.VpnProfile;
import com.b.android.openvpn60.adapter.CustomAdapter;
import com.b.android.openvpn60.constant.ServiceConstants;
import com.b.android.openvpn60.core.ProfileManager;
import com.b.android.openvpn60.R;
import com.b.android.openvpn60.fragment.ServerSelectFragment;
import com.b.android.openvpn60.helper.LogHelper;
import com.b.android.openvpn60.service.MemberService;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.message.BasicNameValuePair;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String SHARED_PREFS = AppConstants.SHARED_PREFS.toString();
    private static final String USER_NAME = AppConstants.USER_NAME.toString();
    private static final String RESULT_PROFILE = AppConstants.RESULT_PROFILE.toString();
    private static final String SERVICE_URL_PUT = ServiceConstants.URL_PUT.toString();
    private static final String SERVICE_URL_GET_PROFILES = ServiceConstants.URL_GET_PROFILES.toString();
    private static int UPDATE_INTERVAL = 10000; // 10 sec
    private static int FATEST_INTERVAL = 5000; // 5 sec
    private static int DISPLACEMENT = 10; // 10 meters
    private final String CLASS_TAG = AppConstants.CLASS_TAG_ACTIVITY.toString() + this.getClass().toString();
    private static final int PERMISSION_REQUEST = 23621;

    private SharedPreferences sharedPrefs;
    private Location lastLocation;
    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;
    // boolean flag to toggle periodic location updates
    private boolean isLocationUpdated = false;
    private LocationRequest locationRequest;
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
    private boolean isAvailable = false;
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
        //generateProfiles();
        init();
        prepareService();
        //updateViews();
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
        //profiles = getProfileInfos();
        //profiles = new ArrayList<>(getPM().getProfiles());
        profiles = new ArrayList<>();
        profiles = getProfileInfos();
        logHelper = LogHelper.getLogHelper(this);
        progressBar = (ProgressBar) this.findViewById(R.id.progressBar);
        btnConnect = (Button) this.findViewById(R.id.btnConnect);
        importer = new Intent(this, ImportActivity.class);
        intentService = new Intent(this, LaunchVPN.class);
        edtHost = (EditText) this.findViewById(R.id.edtIP);
        edtHost.setShadowLayer(1, 0, 1, getResources().getColor(R.color.colorAccent));
        edtUser = (EditText) this.findViewById(R.id.edtUser);
        edtUser.setShadowLayer(1, 0, 1, getResources().getColor(R.color.colorAccent));
        edtPort = (EditText) this.findViewById(R.id.edtPort);
        edtPort.setShadowLayer(1, 0, 1, getResources().getColor(R.color.colorAccent));
        txtProfileName = (TextView) this.findViewById(R.id.txtProfileName);
        txtUsername = (TextView) this.findViewById(R.id.txtName);
        txtUsername.setShadowLayer(1, 0, 1, getResources().getColor(R.color.colorAccent));
        txtProfile = (TextView) this.findViewById(R.id.txtLocation);
        txtProfile.setShadowLayer(1, 0, 1, getResources().getColor(R.color.colorAccent));
        txtIp = (TextView) this.findViewById(R.id.txtIp);
        txtIp.setShadowLayer(1, 0, 1, getResources().getColor(R.color.colorAccent));
        txtPort = (TextView) this.findViewById(R.id.txtPort);
        txtPort.setShadowLayer(1, 0, 1, getResources().getColor(R.color.colorAccent));
        sharedPrefs = this.getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        userName = getIntent().getStringExtra(AppConstants.USER_NAME.toString());
        edtUser.setText(userName);
        pnlMain = (RelativeLayout) this.findViewById(R.id.activity_main);
        //profile = ProfileManager.get(getApplicationContext(), getIntent().getStringExtra(AppConstants.EXTRA_KEY.toString()));
        //user = (User) intentMain.getSerializableExtra(AppConstants.TEMP_USER.toString());
        //userName = sharedPrefs.getString(AppConstants.USER_NAME.toString(), null);
        btnSelect = (Button) this.findViewById(R.id.btnSelect);
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
                            mStatus.putExtra(RESULT_PROFILE, profile);
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
                    intent.putExtra(USER_NAME, userName);
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
                displayLocation();
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST);
        }
        isAvailable = true;
    }


    private void prepareService() {
        invokeWS();
        startMembershipService(userName);
    }


    private ArrayList<VpnProfile> generateProfiles() {
        return getProfileInfos();
        /*profiles = new ArrayList<>(getPM().getProfiles());
        if (profiles.size() == 0) {
            for (int i=0; i<=3; i++) {
                VpnProfile tempProfile = new VpnProfile("converted profile " + i);
                saveProfile(tempProfile);
            }
        }*/
    }


    private void saveProfile(VpnProfile profile) {
        Intent result = new Intent();
        ProfileManager vpl = ProfileManager.getInstance(this);
        vpl.addProfile(profile);
        vpl.saveProfile(this, profile);
        vpl.saveProfileList(this);
        result.putExtra(LaunchVPN.EXTRA_KEY, profile.getUUIDString());
        setResult(Activity.RESULT_OK, result);
        //finish();
    }


    public void invokeWS() {
        AsyncHttpClient client = new AsyncHttpClient();
        final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair(USER_NAME, userName));
        HttpEntity entity = null;
        try {
            entity = new UrlEncodedFormEntity(nameValuePairs);
        } catch (UnsupportedEncodingException a) {
            logHelper.logException(a);
        }
        client.put(getApplicationContext(), SERVICE_URL_PUT, entity, "application/x-www-form-urlencoded",
                new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    if (response.getBoolean("status")) {
                        logHelper.logInfo("Update last login date = " + "OK");
                    }
                    else{
                        Toast.makeText(getApplicationContext(), getString(R.string.err_state_register),
                                Toast.LENGTH_SHORT).show();
                        logHelper.logInfo("Update last login date = " + "Failed");
                    }
                } catch (Exception ex) {
                    Toast.makeText(getApplicationContext(), getString(R.string.err_state_json),
                            Toast.LENGTH_SHORT).show();
                    logHelper.logException(ex);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
                if (statusCode == 404){
                    Toast.makeText(getApplicationContext(), getString(R.string.err_server_404),
                            Toast.LENGTH_SHORT).show();
                    logHelper.logWarning(getString(R.string.state_error_occured) + statusCode);
                }
                else if (statusCode == 500){
                    Toast.makeText(getApplicationContext(), getString(R.string.err_server_500),
                            Toast.LENGTH_SHORT).show();
                    logHelper.logWarning(getString(R.string.state_error_occured) + statusCode);
                }
                else{
                    Toast.makeText(getApplicationContext(), getString(R.string.err_server_else),
                            Toast.LENGTH_SHORT).show();
                    logHelper.logWarning(getString(R.string.state_error_occured) + statusCode);
                }
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (getIntent().getSerializableExtra(RESULT_PROFILE) != null)
            profile = (VpnProfile) getIntent().getSerializableExtra(RESULT_PROFILE);
        //isUserAMember();
        //updateProfiles();
        generateProfiles();
        if (progressBar.getVisibility() != View.INVISIBLE)
            progressBar.setVisibility(View.INVISIBLE);
        IntentFilter filter = new IntentFilter(AppConstants.CHECK_MEMBER.toString());
        LocalBroadcastManager.getInstance(this).registerReceiver(testReceiver, filter);
    }


    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(testReceiver);
    }

    private void startOrStopVPN(VpnProfile profile) {
        startVPN(profile);
    }


    private void updateProfiles() {
        profiles = getProfiles();
    }


    private ArrayList<VpnProfile> getProfiles() {
        return new ArrayList<>(getPM().getProfiles());
    }


    public void updateViews() {
        if (getIntent().getSerializableExtra(RESULT_PROFILE) != null) {
            profile = (VpnProfile) getIntent().getSerializableExtra(RESULT_PROFILE);
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
        //if (frTransaction != null && getFragmentManager() != null) {
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


    @Override
    public void onConnected(Bundle arg0) {
        displayLocation();
    }


    @TargetApi(Build.VERSION_CODES.M)
    private synchronized void displayLocation() {
        tasker = new AsyncTask<Void, Void, Integer>() {
            Thread threadOne;
            Thread threadTwo;

            @Override
            protected void onPreExecute() {
                threadOne = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        checkPermission();
                    }
                });
                threadOne.start();

                threadTwo = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (isAvailable) {
                            try {
                                lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                            } catch (SecurityException a) {
                                logHelper.logException(a);

                            }
                        }
                    }

                });
                //threadTwo.start();
            }

            @Override
            protected Integer doInBackground(Void... params) {
                try {
                    threadOne.join(2000);
                    threadTwo.start();
                    threadTwo.join();
                }catch( Exception e) {
                    logHelper.logException(e);
                    return -1;
                }

                return 0;
            }

            @Override
            protected void onPostExecute(Integer integer) {
                if (integer == 0) {
                    if (lastLocation != null) {
                        double latitude = lastLocation.getLatitude();
                        double longitude = lastLocation.getLongitude();

                        Toast.makeText(MainActivity.this, getString(R.string.state_displayLocation) + String.valueOf(latitude) + ", " + String.valueOf(longitude), Toast.LENGTH_SHORT).show();
                        logHelper.logInfo("Location received = " + String.valueOf(latitude) + ", " +
                                String.valueOf(longitude));

                    } else {
                        Toast.makeText(MainActivity.this, getString(R.string.prompt_displayLocation), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }.execute();
    }


    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }


    public void onConnectionFailed(ConnectionResult result) {
        logHelper.logInfo("Connection failed:  = " + result.getErrorCode() + " - " + result.getErrorMessage());
    }


    private  ArrayList<VpnProfile> getProfileInfos() {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(SERVICE_URL_GET_PROFILES, null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                try {
                    if (!isServerstaken) {
                        if (!profiles.isEmpty())
                            profiles.clear();
                        else if (profiles.isEmpty()) {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject object = (JSONObject) response.get(i);
                                VpnProfile tempProfile = new VpnProfile(object.getString("serverName"),
                                        object.getString("serverIp"), object.getString("serverPort"),
                                            object.getString("serverCert"));
                                //tempProfile.setConnection(con);
                                profiles.add(tempProfile);
                                saveProfile(tempProfile);
                            }
                            isServerstaken = true;
                            if (!profiles.isEmpty()) {
                                //profile = profiles.get(0);
                            }
                            //updateViews();
                            logHelper.logInfo(getString(R.string.state_sorted_profiles));
                        }
                    }
                } catch (JSONException ex) {
                    Toast.makeText(getApplicationContext(), getString(R.string.err_state_json), Toast.LENGTH_SHORT).show();
                    logHelper.logException(ex);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                if(statusCode == 404) {
                    Toast.makeText(getApplicationContext(), getString(R.string.err_server_404), Toast.LENGTH_SHORT).show();
                    logHelper.logException(throwable);
                } else if(statusCode == 500) {
                    Toast.makeText(getApplicationContext(), getString(R.string.err_server_500), Toast.LENGTH_SHORT).show();
                    logHelper.logException(throwable);
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.err_server_else), Toast.LENGTH_SHORT).show();
                    logHelper.logException(throwable);
                }
            }

        });
        return (ArrayList<VpnProfile>) profiles;
    }


    // Define the callback for what to do when message is received
    private BroadcastReceiver testReceiver = new BroadcastReceiver() {
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


    private void startMembershipService(String userName) {
        Intent i = new Intent(this, MemberService.class);
        i.setAction(AppConstants.CHECK_MEMBER.toString());
        i.putExtra(AppConstants.USER_NAME.toString(), userName);
        startService(i);
    }

}