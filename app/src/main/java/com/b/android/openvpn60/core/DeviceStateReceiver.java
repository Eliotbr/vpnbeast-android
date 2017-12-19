package com.b.android.openvpn60.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;

import java.util.LinkedList;

/**
 * Created by b on 5/15/17.
 */

public class DeviceStateReceiver extends BroadcastReceiver implements VpnStatus.ByteCountListener, OpenVPNManagement.PausedStateCallback {
    private final Handler mDisconnectHandler;
    private int lastNetwork = -1;
    private OpenVPNManagement mManagement;
    // Window time in s
    private final int TRAFFIC_WINDOW = 60;
    // Data traffic limit in bytes
    private final long TRAFFIC_LIMIT = 64 * 1024;
    // Time to wait after network disconnect to pause the VPN
    private final int DISCONNECT_WAIT = 20;
    private ConnectState network = ConnectState.DISCONNECTED;
    private ConnectState screen = ConnectState.SHOULDBECONNECTED;
    private ConnectState userpause = ConnectState.SHOULDBECONNECTED;
    private String lastStateMsg = null;
    private Runnable mDelayDisconnectRunnable = new Runnable() {
        @Override
        public void run() {
            if (!(network == ConnectState.PENDINGDISCONNECT))
                return;
            network = ConnectState.DISCONNECTED;
            // Set screen state to be disconnected if disconnect pending
            if (screen == ConnectState.PENDINGDISCONNECT)
                screen = ConnectState.DISCONNECTED;
            mManagement.pause(getPauseReason());
        }
    };
    private NetworkInfo lastConnectedNetwork;


    @Override
    public boolean shouldBeRunning() {
        return shouldBeConnected();
    }

    private enum ConnectState {
        SHOULDBECONNECTED,
        PENDINGDISCONNECT,
        DISCONNECTED
    }

    private static class Datapoint {
        private Datapoint(long t, long d) {
            timestamp = t;
            data = d;
        }

        long timestamp;
        long data;
    }

    private LinkedList<Datapoint> trafficdata = new LinkedList<>();

    @Override
    public void updateByteCount(long in, long out, long diffIn, long diffOut) {
        if (screen != ConnectState.PENDINGDISCONNECT)
            return;
        long total = diffIn + diffOut;
        trafficdata.add(new Datapoint(System.currentTimeMillis(), total));
        while (trafficdata.getFirst().timestamp <= (System.currentTimeMillis() - TRAFFIC_WINDOW * 1000)) {
            trafficdata.removeFirst();
        }
        long windowtraffic = 0;
        for (Datapoint dp : trafficdata)
            windowtraffic += dp.data;
        if (windowtraffic < TRAFFIC_LIMIT) {
            screen = ConnectState.DISCONNECTED;
            mManagement.pause(getPauseReason());
        }
    }

    public void userPause(boolean pause) {
        if (pause) {
            userpause = ConnectState.DISCONNECTED;
            // Check if we should disconnect
            mManagement.pause(getPauseReason());
        } else {
            boolean wereConnected = shouldBeConnected();
            userpause = ConnectState.SHOULDBECONNECTED;
            if (shouldBeConnected() && !wereConnected)
                mManagement.resume();
            else
                // Update the reason why we currently paused
                mManagement.pause(getPauseReason());
        }
    }

    public DeviceStateReceiver(OpenVPNManagement magnagement) {
        super();
        mManagement = magnagement;
        mManagement.setPauseCallback(this);
        mDisconnectHandler = new Handler();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = Preferences.getDefaultSharedPreferences(context);
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            networkStateChange(context);
        } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
            boolean screenOffPause = prefs.getBoolean("screenoff", false);
            if (screenOffPause) {
                if (ProfileManager.getLastConnectedVpn() != null && !ProfileManager.getLastConnectedVpn().persistTun)
                    screen = ConnectState.PENDINGDISCONNECT;
                fillTrafficData();
                if (network == ConnectState.DISCONNECTED || userpause == ConnectState.DISCONNECTED)
                    screen = ConnectState.DISCONNECTED;
            }
        } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
            // Network was disabled because screen off
            boolean connected = shouldBeConnected();
            screen = ConnectState.SHOULDBECONNECTED;
            /* We should connect now, cancel any outstanding disconnect timer */
            mDisconnectHandler.removeCallbacks(mDelayDisconnectRunnable);
            /* should be connected has changed because the screen is on now, connect the VPN */
            if (shouldBeConnected() != connected)
                mManagement.resume();
            else if (!shouldBeConnected())
                /*Update the reason why we are still paused */
                mManagement.pause(getPauseReason());
        }
    }

    private void fillTrafficData() {
        trafficdata.add(new Datapoint(System.currentTimeMillis(), TRAFFIC_LIMIT));
    }

    public static boolean equalsObj(Object a, Object b) {
        return (a == null) ? (b == null) : a.equals(b);
    }

    public void networkStateChange(Context context) {
        NetworkInfo networkInfo = getCurrentNetworkInfo(context);
        SharedPreferences prefs = Preferences.getDefaultSharedPreferences(context);
        boolean sendusr1 = prefs.getBoolean("netchangereconnect", true);
        String netstatestring;
        if (networkInfo == null) {
            netstatestring = "not connected";
        } else {
            String subtype = networkInfo.getSubtypeName();
            if (subtype == null)
                subtype = "";
            String extrainfo = networkInfo.getExtraInfo();
            if (extrainfo == null)
                extrainfo = "";
            /*
            if(networkInfo.getType()==android.net.ConnectivityManager.TYPE_WIFI) {
				WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
				WifiInfo wifiinfo = wifiMgr.getConnectionInfo();
				extrainfo+=wifiinfo.getBSSID();

				subtype += wifiinfo.getNetworkId();
			}*/
            netstatestring = String.format("%2$s %4$s to %1$s %3$s", networkInfo.getTypeName(),
                    networkInfo.getDetailedState(), extrainfo, subtype);
        }
        if (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED) {
            int newnet = networkInfo.getType();
            boolean pendingDisconnect = (network == ConnectState.PENDINGDISCONNECT);
            network = ConnectState.SHOULDBECONNECTED;
            boolean sameNetwork;
            if (lastConnectedNetwork == null
                    || lastConnectedNetwork.getType() != networkInfo.getType()
                    || !equalsObj(lastConnectedNetwork.getExtraInfo(), networkInfo.getExtraInfo())
                    )
                sameNetwork = false;
            else
                sameNetwork = true;
            /* Same network, connection still 'established' */
            if (pendingDisconnect && sameNetwork) {
                mDisconnectHandler.removeCallbacks(mDelayDisconnectRunnable);
                // Reprotect the sockets just be sure
                mManagement.networkChange(true);
            } else {
                /* Different network or connection not established anymore */
                if (screen == ConnectState.PENDINGDISCONNECT)
                    screen = ConnectState.DISCONNECTED;

                if (shouldBeConnected()) {
                    mDisconnectHandler.removeCallbacks(mDelayDisconnectRunnable);
                    if (pendingDisconnect || !sameNetwork)
                        mManagement.networkChange(sameNetwork);
                    else
                        mManagement.resume();
                }
                lastNetwork = newnet;
                lastConnectedNetwork = networkInfo;
            }
        } else if (networkInfo == null) {
            // Not connected, stop openvpn, set last connected network to no network
            lastNetwork = -1;
            if (sendusr1) {
                network = ConnectState.PENDINGDISCONNECT;
                mDisconnectHandler.postDelayed(mDelayDisconnectRunnable, DISCONNECT_WAIT * 1000);
            }
        }
        lastStateMsg = netstatestring;
    }

    public boolean isUserPaused() {
        return userpause == ConnectState.DISCONNECTED;
    }

    private boolean shouldBeConnected() {
        return (screen == ConnectState.SHOULDBECONNECTED && userpause == ConnectState.SHOULDBECONNECTED &&
                network == ConnectState.SHOULDBECONNECTED);
    }

    private OpenVPNManagement.pauseReason getPauseReason() {
        if (userpause == ConnectState.DISCONNECTED)
            return OpenVPNManagement.pauseReason.userPause;

        if (screen == ConnectState.DISCONNECTED)
            return OpenVPNManagement.pauseReason.screenOff;

        if (network == ConnectState.DISCONNECTED)
            return OpenVPNManagement.pauseReason.noNetwork;

        return OpenVPNManagement.pauseReason.userPause;
    }

    private NetworkInfo getCurrentNetworkInfo(Context context) {
        ConnectivityManager conn = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return conn.getActiveNetworkInfo();
    }
}