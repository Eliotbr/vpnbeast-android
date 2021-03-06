/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package com.b.android.openvpn60.listener;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.b.android.openvpn60.constant.AppConstants;
import com.b.android.openvpn60.core.IServiceStatus;
import com.b.android.openvpn60.core.IStatusCallbacks;
import com.b.android.openvpn60.core.OpenVPNStatusService;
import com.b.android.openvpn60.core.VpnStatus;

import java.io.File;

import com.b.android.openvpn60.core.ConnectionStatus;
import com.b.android.openvpn60.core.LogItem;
import com.b.android.openvpn60.helper.LogHelper;


/**
 * Created by b on 09.05.17
 */

public class StatusListener {
    private static final LogHelper logHelper = LogHelper.getLogHelper(StatusListener.class.getName());

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            IServiceStatus serviceStatus = IServiceStatus.Stub.asInterface(service);
            try {
                /* Check if this a local service ... */
                if (service.queryLocalInterface("com.b.android.openvpn60.core.IServiceStatus") == null) {
                    // Not a local service
                    VpnStatus.setConnectedVPNProfile(serviceStatus.getLastConnectedVPN());
                    serviceStatus.registerStatusCallback(mCallback);
                }

            } catch (RemoteException e) {
                logHelper.logException(e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // nothing to do
        }

    };


    public void init(Context c) {
        Intent intent = new Intent(c, OpenVPNStatusService.class);
        intent.setAction(AppConstants.START_SERVICE.toString());
        c.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }


    private IStatusCallbacks mCallback = new IStatusCallbacks.Stub() {

        @Override
        public void newLogItem(LogItem item) throws RemoteException {
            VpnStatus.newLogItem(item);
        }

        @Override
        public void updateStateString(String state, String msg, int resid, ConnectionStatus
                level) throws RemoteException {
            VpnStatus.updateStateString(state, msg, resid, level);
        }

        @Override
        public void updateByteCount(long inBytes, long outBytes) throws RemoteException {
            VpnStatus.updateByteCount(inBytes, outBytes);
        }

        @Override
        public void connectedVPN(String uuid) throws RemoteException {
            VpnStatus.setConnectedVPNProfile(uuid);
        }
    };

}
