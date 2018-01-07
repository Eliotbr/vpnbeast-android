package com.b.android.openvpn60.core;

import java.util.Locale;

/**
 * Created by b on 5/15/17.
 */

public class IPAddress {
    String ip;
    int len;


    public IPAddress(String ip, String mask) {
        this.ip = ip;
        long netmask = getInt(mask);

        // Add 33. bit to ensure the loop terminates
        netmask += 1l << 32;

        int lenZeros = 0;
        while ((netmask & 0x1) == 0) {
            lenZeros++;
            netmask = netmask >> 1;
        }
        // Check if rest of netmask is only 1s
        if (netmask != (0x1ffffffffl >> lenZeros)) {
            // Asume no CIDR, set /32
            len = 32;
        } else {
            len = 32 - lenZeros;
        }

    }

    public IPAddress(String address, int prefix_length) {
        len = prefix_length;
        ip = address;
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "%s/%d", ip, len);
    }

    public boolean normalise() {
        long ip = getInt(this.ip);

        long newip = ip & (0xffffffffl << (32 - len));
        if (newip != ip) {
            this.ip = getNormalizedString(newip);
            return true;
        } else {
            return false;
        }

    }

    private String getNormalizedString(long newip) {
        return String.format("%d.%d.%d.%d", (newip & 0xff000000) >> 24, (newip & 0xff0000) >> 16,
                (newip & 0xff00) >> 8, newip & 0xff);
    }

    static long getInt(String ipaddr) {
        String[] ipt = ipaddr.split("\\.");
        long ip = 0;
        ip += Long.parseLong(ipt[0]) << 24;
        ip += Integer.parseInt(ipt[1]) << 16;
        ip += Integer.parseInt(ipt[2]) << 8;
        ip += Integer.parseInt(ipt[3]);
        return ip;
    }

    public long getInt() {
        return getInt(ip);
    }
}
