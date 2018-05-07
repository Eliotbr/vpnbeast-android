/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package com.b.android.openvpn60.listener;

import android.annotation.TargetApi;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;

/**
 * Created by b on 26.11.14.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LollipopDeviceStateListener extends ConnectivityManager.NetworkCallback {

    private String mLastConnectedStatus;
    private String mLastLinkProperties;
    private String mLastNetworkCapabilities;



    @Override
    public void onAvailable(Network network) {
        super.onAvailable(network);

        if (!network.toString().equals(mLastConnectedStatus)) {
            mLastConnectedStatus = network.toString();
        }
    }


    @Override
    public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
        super.onLinkPropertiesChanged(network, linkProperties);

        if (!linkProperties.toString().equals(mLastLinkProperties)) {
            mLastLinkProperties = linkProperties.toString();
        }
    }


    @Override
    public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
        super.onCapabilitiesChanged(network, networkCapabilities);
        if (!networkCapabilities.toString().equals(mLastNetworkCapabilities)) {
            mLastNetworkCapabilities = networkCapabilities.toString();
        }
    }
}
