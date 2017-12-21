package com.b.android.openvpn60.util;

import android.util.Log;

import com.b.android.openvpn60.helper.LogHelper;
import com.b.android.openvpn60.model.VpnProfile;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

/**
 * Created by b on 5/15/17.
 */

public class ProxyUtil {
    private static final String TAG = "com.b.android.openvpn.core." + ProxyUtil.class.toString();
    private static LogHelper logHelper;

    static {
        logHelper = LogHelper.getLogHelper(ProxyUtil.class.toString());
    }


    public static SocketAddress detectProxy(VpnProfile vp) {
        // Construct a new url with https as protocol
        try {
            URL url = new URL(String.format("https://%s:%s",vp.connections[0].serverName, vp.connections[0].serverPort));
            Proxy proxy = getFirstProxy(url);
            if(proxy==null)
                return null;
            SocketAddress addr = proxy.address();
            if (addr instanceof InetSocketAddress) {
                logHelper.logInfo("Socket address returned...");
                return addr;
            }
        } catch (MalformedURLException e) {
            logHelper.logException(e);
        } catch (URISyntaxException e) {
            logHelper.logException(e);
        }
        return null;
    }

    public static Proxy getFirstProxy(URL url) throws URISyntaxException {
        System.setProperty("java.net.useSystemProxies", "true");
        List<Proxy> proxylist = ProxySelector.getDefault().select(url.toURI());
        if (proxylist != null) {
            for (Proxy proxy: proxylist) {
                SocketAddress addr = proxy.address();
                if (addr != null) {
                    return proxy;
                }
            }
        }
        return null;
    }
}