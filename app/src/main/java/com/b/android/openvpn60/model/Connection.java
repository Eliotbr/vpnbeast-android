package com.b.android.openvpn60.model;

import android.text.TextUtils;

import java.io.Serializable;
import java.util.Locale;

/**
 * Created by b on 5/15/17.
 */

public class Connection implements Serializable, Cloneable {
    public static final int CONNECTION_DEFAULT_TIMEOUT = 120;
    private static final long serialVersionUID = 92031902903829089L;
    public String serverName = "openvpn.example.com";
    public String serverPort = "1194";
    public boolean isUdp = true;
    public String customConfiguration = "";
    public boolean useCustomConfig = false;
    public boolean isEnabled = true;
    public int connectTimeout = 0;


    public Connection() {
        initConstants();
    }

    public String getConnectionBlock() {
        String cfg = "";
        cfg += "remote ";
        cfg += serverName;
        cfg += " ";
        cfg += serverPort;
        if (isUdp)
            cfg += " udp\n";
        else
            cfg += " tcp-client\n";
        if (connectTimeout != 0)
            cfg += String.format(Locale.ENGLISH," connect-timeout  %d\n", connectTimeout);
        if (!TextUtils.isEmpty(customConfiguration) && useCustomConfig) {
            cfg += customConfiguration;
            cfg += "\n";
        }
        return cfg;
    }

    @Override
    public Connection clone() throws CloneNotSupportedException {
        return (Connection) super.clone();
    }

    public boolean isOnlyRemote() {
        return TextUtils.isEmpty(customConfiguration) || !useCustomConfig;
    }

    public int getTimeout() {
        if (connectTimeout <= 0)
            return CONNECTION_DEFAULT_TIMEOUT;
        else
            return connectTimeout;
    }

    private void initConstants() {
        serverName = "95.85.25.155";
        serverPort = "443";
        isUdp = true;
        customConfiguration = "";
        useCustomConfig = false;
        isEnabled = true;
        connectTimeout = 0;
    }

    @Override
    public String toString() {
        return "Connection{" +
                "serverName='" + serverName + '\'' +
                ", serverPort='" + serverPort + '\'' +
                ", isUdp=" + isUdp +
                ", customConfiguration='" + customConfiguration + '\'' +
                ", useCustomConfig=" + useCustomConfig +
                ", isEnabled=" + isEnabled +
                ", connectTimeout=" + connectTimeout +
                '}';
    }
}