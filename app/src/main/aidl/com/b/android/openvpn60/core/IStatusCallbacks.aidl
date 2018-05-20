package com.b.android.openvpn60.core;

import com.b.android.openvpn60.core.LogItem;
import com.b.android.openvpn60.core.ConnectionStatus;


interface IStatusCallbacks {
    /**
     * Called when the service has a new status for you.
     */
    oneway void newLogItem(in LogItem item);

    oneway void updateStateString(in String state, in String msg, in int resid, in ConnectionStatus level);

    oneway void updateByteCount(long inBytes, long outBytes);

    oneway void connectedVPN(String uuid);
}
