package com.b.android.openvpn60.service;

import android.content.Intent;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;
import com.b.android.openvpn60.R;
import com.b.android.openvpn60.activity.LaunchVPN;
import com.b.android.openvpn60.constant.AppConstants;
import com.b.android.openvpn60.constant.ServiceConstants;
import com.b.android.openvpn60.core.ProfileManager;
import com.b.android.openvpn60.helper.DbHelper;
import com.b.android.openvpn60.helper.LogHelper;
import com.b.android.openvpn60.model.VpnProfile;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.Header;


public class ServerService extends MainService {
    private Map<String, VpnProfile> profileMap;
    private List<VpnProfile> profileList;
    private boolean isServerstaken = false;
    private Bundle bundle;
    private DbHelper dbHelper;


    public void onCreate() {
        super.onCreate();
        // An Android handler thread internally operates on a looper.
        handlerThread = new HandlerThread("ServerService.HandlerThread");
        handlerThread.start();
        context = getApplicationContext();
        dbHelper = new DbHelper(context);
        LOG_HELPER = LogHelper.getLogHelper(ServerService.class.getName());
        bundle = new Bundle();
        profileMap = new HashMap<>();
        profileList = new ArrayList<>();
        // An Android service handler is a handler running on a specific background thread.
        serviceHandler = new ServiceHandler(handlerThread.getLooper());
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        serviceHandler.post(new Runnable() {
            @Override
            public void run() {
                String action = intent.getAction();
                if (action != null && action.equals(AppConstants.GET_VPN_PROFILES.toString())) {
                    responseIntent = new Intent(AppConstants.GET_VPN_PROFILES.toString());
                    getProfileInfos();
                }
            }
        });
        return START_STICKY;
    }

    private void getProfileInfos() {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(ServiceConstants.URL_GET_PROFILES.toString(), null, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                try {
                    if (response != null) {
                        if (!isServerstaken) {
                            profileList.clear();
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject object = (JSONObject) response.get(i);
                                VpnProfile tempProfile = new VpnProfile(object.getString("serverUuid"),
                                        object.getString("serverName"), object.getString("serverIp"),
                                        object.getString("serverPort"), object.getString("serverCert"));
                                //tempProfile.setConnection(con);
                                profileList.add(tempProfile);
                                saveProfile(tempProfile);
                            }
                            isServerstaken = true;
                            if (!profileList.isEmpty()) {
                                //profile = profiles.get(0);
                            }
                            bundle.putParcelableArrayList(AppConstants.VPN_PROFILES.toString(), (ArrayList<VpnProfile>) profileList);
                            responseIntent.putExtra(AppConstants.BUNDLE_VPN_PROFILES.toString(), bundle);
                            LOG_HELPER.logInfo(getString(R.string.state_sorted_profiles));
                            LOG_HELPER.logInfo("Talking with DbHelper...");
                            dbHelper.insertProfileList((ArrayList<VpnProfile>) profileList);
                        }
                    }
                } catch (JSONException ex) {
                    LOG_HELPER.logException(ex);
                }
                stopService();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                if(statusCode == 404) {
                    LOG_HELPER.logException(context.getString(R.string.err_server_404), throwable);
                    responseIntent.putExtra("status", "errServer404");
                } else if(statusCode == 500) {
                    LOG_HELPER.logException(context.getString(R.string.err_server_500), throwable);
                    responseIntent.putExtra("status", "errServer500");
                } else {
                    LOG_HELPER.logException(context.getString(R.string.err_server_else), throwable);
                    responseIntent.putExtra("status", "errServerElse");
                }
                stopService();
            }
        });
    }

    private void saveProfile(VpnProfile profile) {
        Intent result = new Intent();
        ProfileManager vpl = ProfileManager.getInstance(this);
        vpl.addProfile(profile);
        vpl.saveProfile(this, profile);
        vpl.saveProfileList(this);
        result.putExtra(LaunchVPN.EXTRA_KEY, profile.getUUIDString());
        //setResult(Activity.RESULT_OK, result);
        //finish();
    }
}