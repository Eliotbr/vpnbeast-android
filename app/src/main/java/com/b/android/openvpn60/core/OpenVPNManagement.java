package com.b.android.openvpn60.core;

/**
 * Created by b on 5/15/17.
 */

public interface OpenVPNManagement {
    int byteCountInterval = 2;

    interface PausedStateCallback {
        boolean shouldBeRunning();
    }

    enum pauseReason {
        NO_NETWORK,
        USER_PAUSE,
        SCREEN_OFF,
    }


    void reconnect();

    void pause(pauseReason reason);

    void resume();

    /**
     * @param replaceConnection True if the VPN is connected by a new connection.
     * @return true if there was a process that has been send a stop signal
     */
    boolean stopVPN(boolean replaceConnection);

    /*
     * Rebind the interface
     */
    void networkChange(boolean sameNetwork);

    void setPauseCallback(PausedStateCallback callback);
}
