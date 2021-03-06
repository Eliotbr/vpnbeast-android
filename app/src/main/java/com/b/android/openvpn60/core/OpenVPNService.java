package com.b.android.openvpn60.core;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;

import com.b.android.openvpn60.activity.LaunchVPN;
import com.b.android.openvpn60.R;
import com.b.android.openvpn60.constant.AppConstants;
import com.b.android.openvpn60.constant.BuildConstants;
import com.b.android.openvpn60.helper.VPNLaunchHelper;
import com.b.android.openvpn60.model.VpnProfile;
import com.b.android.openvpn60.activity.StatusActivity;
import com.b.android.openvpn60.helper.LogHelper;
import com.b.android.openvpn60.util.NotificationUtil;
import com.b.android.openvpn60.util.PreferencesUtil;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Locale;
import java.util.Vector;

import de.blinkt.openvpn.core.NativeUtils;

import static com.b.android.openvpn60.core.ConnectionStatus.LEVEL_CONNECTED;
import static com.b.android.openvpn60.core.ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT;

/**
 * Created by b on 5/15/17.
 */

public class OpenVPNService extends VpnService implements VpnStatus.StateListener, Handler.Callback,
        VpnStatus.ByteCountListener, IOpenVPNServiceInternal {

    private static final int OPENVPN_STATUS = 1;
    private static boolean notificationsAlwaysVisible = false;
    private final Vector<String> dnsVector = new Vector<>();
    private final NetworkSpace routesIPv4 = new NetworkSpace();
    private final NetworkSpace routesIPv6 = new NetworkSpace();
    private static Thread processThread = null;
    private VpnProfile vpnProfile;
    private String domainName = null;
    private IPAddress localIP = null;
    private int mtu;
    private String localIPv6 = null;
    private DeviceStateReceiver deviceStateReceiver;
    private boolean displayByteCount = false;
    private boolean isStarting = false;
    private long connectTime;
    private boolean isOpenvpn3 = false;
    private OpenVPNManagement vpnManagement;
    private String lastTunCfg;
    private String remoteGW;
    private final Object mProcessLock = new Object();
    private Runnable vpnThread;
    private static Class notificationActivityClass;
    //private static OpenVPNService instance;
    private LogHelper logHelper;
    private NotificationUtil notificationUtil;

    private static final int PRIORITY_MIN = -2;
    private static final int PRIORITY_DEFAULT = 0;
    private static final int PRIORITY_MAX = 2;


    private final IBinder mBinder = new IOpenVPNServiceInternal.Stub() {

        @Override
        public boolean protect(int fd) throws RemoteException {
            return OpenVPNService.this.protect(fd);
        }

        @Override
        public void userPause(boolean shouldbePaused) throws RemoteException {
            OpenVPNService.this.userPause(shouldbePaused);
        }

        @Override
        public boolean stopVPN(boolean replaceConnection) throws RemoteException {
            return OpenVPNService.this.stopVPN(replaceConnection);
        }
    };

    // From: http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
    public static String humanReadableByteCount(long bytes, boolean mbit) {
        if (mbit)
            bytes = bytes * 8;
        int unit = mbit ? 1000 : 1024;
        if (bytes < unit)
            return bytes + (mbit ? " bit" : " B");

        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (mbit ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (mbit ? "" : "");
        if (mbit)
            return String.format(Locale.getDefault(), "%.1f %sbit", bytes / Math.pow(unit, exp), pre);
        else
            return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    @Override
    public IBinder onBind(Intent intent) {
        String action = intent.getAction();
        if (action != null && action.equals(AppConstants.START_SERVICE.toString()))
            return mBinder;
        else
            return super.onBind(intent);
    }

    @Override
    public void onRevoke() {
        VpnStatus.logError(R.string.permission_revoked);
        vpnManagement.stopVPN(false);
        endVpnService();
    }

    // Similar to revoke but do not try to stop process
    public void processDied() {
        endVpnService();
    }

    private void endVpnService() {
        synchronized (mProcessLock) {
            processThread = null;
        }
        VpnStatus.removeByteCountListener(this);
        unregisterDeviceStateReceiver();
        ProfileManager.setConntectedVpnProfileDisconnected(this);
        vpnThread = null;
        if (!isStarting) {
            stopForeground(!notificationsAlwaysVisible);

            if (!notificationsAlwaysVisible) {
                stopSelf();
                VpnStatus.removeStateListener(this);
            }
        }
    }


    private void showNotification(final String msg, String tickerText, int priority, long when, ConnectionStatus status) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
        int icon = R.drawable.vpn26;
        Notification.Builder nbuilder = new Notification.Builder(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationUtil = new NotificationUtil(this);
            nbuilder = notificationUtil.getAndroidChannelNotification();
        }

        if (vpnProfile != null)
            nbuilder.setContentTitle(getString(R.string.notifcation_title, vpnProfile.name));
        else
            nbuilder.setContentTitle(getString(R.string.notifcation_title_notconnect));

        nbuilder.setContentText(msg);
        nbuilder.setOnlyAlertOnce(true);
        nbuilder.setOngoing(true);
        nbuilder.setSmallIcon(icon);
        if (status == LEVEL_WAITING_FOR_USER_INPUT)
            nbuilder.setContentIntent(getUserInputIntent(msg));
        else
            nbuilder.setContentIntent(getStatusPendingIntent());

        if (when != 0)
            nbuilder.setWhen(when);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            lpNotificationExtras(nbuilder);

        if (tickerText != null && !tickerText.equals(""))
            nbuilder.setTicker(tickerText);

        Notification notification = nbuilder.getNotification();
        mNotificationManager.notify(OPENVPN_STATUS, notification);
        startForeground(OPENVPN_STATUS, notification);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void lpNotificationExtras(Notification.Builder nbuilder) {
        nbuilder.setCategory(Notification.CATEGORY_SERVICE);
        nbuilder.setLocalOnly(true);

    }

    private boolean runningOnAndroidTV() {
        UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        return uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    /*private int getIconByConnectionStatus(ConnectionStatus level) {
        switch (level) {
            case LEVEL_CONNECTED:
                return R.drawable.vpn26;
            case LEVEL_AUTH_FAILED:
            case LEVEL_NONETWORK:
            case LEVEL_NOTCONNECTED:
                return R.drawable.ic_stat_vpn_offline;
            case LEVEL_CONNECTING_NO_SERVER_REPLY_YET:
            case LEVEL_WAITING_FOR_USER_INPUT:
                return R.drawable.ic_stat_vpn_outline;
            case LEVEL_CONNECTING_SERVER_REPLIED:
                return R.drawable.ic_stat_vpn_empty_halo;
            case LEVEL_VPNPAUSED:
                return android.R.drawable.ic_media_pause;
            case UNKNOWN_LEVEL:
            default:
                return R.drawable.vpn26;

        }
    }*/


    /**
     * Sets the activity which should be opened when tapped on the permanent notification tile.
     *
     * @param activityClass The activity class to open
     */
    public static void setNotificationActivityClass(Class<? extends Activity> activityClass) {
        notificationActivityClass = activityClass;
    }

    PendingIntent getUserInputIntent(String needed) {
        Intent intent = new Intent(getApplicationContext(), LaunchVPN.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("need", needed);
        Bundle b = new Bundle();
        b.putString("need", needed);
        PendingIntent pIntent = PendingIntent.getActivity(this, 12, intent, 0);
        return pIntent;
    }

    PendingIntent getStatusPendingIntent() {
        // Let the configure Button show the Log
        Class activityClass = StatusActivity.class;
        if (notificationActivityClass != null) {
            activityClass = notificationActivityClass;
        }
        Intent intent = new Intent(getBaseContext(), activityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent startLW = PendingIntent.getActivity(this, 0, intent, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return startLW;

    }

    synchronized void registerDeviceStateReceiver(OpenVPNManagement magnagement) {
        // Registers BroadcastReceiver to track network connection changes.
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        deviceStateReceiver = new DeviceStateReceiver(magnagement);

        // Fetch initial network state
        deviceStateReceiver.networkStateChange(this);

        registerReceiver(deviceStateReceiver, filter);
        VpnStatus.addByteCountListener(deviceStateReceiver);

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            addLollipopCMListener(); */
    }

    synchronized void unregisterDeviceStateReceiver() {
        if (deviceStateReceiver != null)
            try {
                VpnStatus.removeByteCountListener(deviceStateReceiver);
                this.unregisterReceiver(deviceStateReceiver);
            } catch (IllegalArgumentException iae) {
                // I don't know why  this happens:
                // java.lang.IllegalArgumentException: Receiver not registered: de.blinkt.openvpn.NetworkSateReceiver@41a61a10
                // Ignore for now ...
                logHelper.logException(iae);
            }
        deviceStateReceiver = null;

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            removeLollipopCMListener();*/

    }

    public void userPause(boolean shouldBePaused) {
        if (deviceStateReceiver != null)
            deviceStateReceiver.userPause(shouldBePaused);
    }

    @Override
    public boolean stopVPN(boolean replaceConnection) throws RemoteException {
        if (getManagement() != null)
            return getManagement().stopVPN(replaceConnection);
        else
            return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logHelper = LogHelper.getLogHelper(OpenVPNService.this);
        if (intent != null && intent.getBooleanExtra(AppConstants.NOTIFICATION_ALWAYS_VISIBLE.toString(), false))
            notificationsAlwaysVisible = true;

        VpnStatus.addStateListener(this);
        VpnStatus.addByteCountListener(this);

        Handler guiHandler = new Handler(getMainLooper());


        if (intent != null && AppConstants.PAUSE_VPN.toString().equals(intent.getAction())) {
            if (deviceStateReceiver != null)
                deviceStateReceiver.userPause(true);
            return START_NOT_STICKY;
        }

        if (intent != null && AppConstants.RESUME_VPN.toString().equals(intent.getAction())) {
            if (deviceStateReceiver != null)
                deviceStateReceiver.userPause(false);
            return START_NOT_STICKY;
        }


        if (intent != null && AppConstants.START_SERVICE.toString().equals(intent.getAction()))
            return START_NOT_STICKY;
        if (intent != null && AppConstants.START_SERVICE_STICKY.toString().equals(intent.getAction())) {
            return START_REDELIVER_INTENT;
        }

        if (intent != null && intent.hasExtra(getPackageName() + ".profileUUID")) {
            String profileUUID = intent.getStringExtra(getPackageName() + ".profileUUID");
            int profileVersion = intent.getIntExtra(getPackageName() + ".profileVerfsion", 0);
            // Try for 10s to get current version of the profile
            vpnProfile = ProfileManager.get(this, profileUUID, profileVersion, 100);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                updateShortCutUsage(vpnProfile);
            }

        } else {
            /* The intent is null when we are set as always-on or the service has been restarted. */
            vpnProfile = ProfileManager.getLastConnectedProfile(this);
            VpnStatus.logInfo(R.string.service_restarted);

            /* Got no profile, just stop */
            if (vpnProfile == null) {
                Log.d("OpenVPN", "Got no last connected profile on null intent. Assuming always on.");
                vpnProfile = ProfileManager.getAlwaysOnVPN(this);

                if (vpnProfile == null) {
                    stopSelf(startId);
                    return START_NOT_STICKY;
                }
            }
            /* Do the asynchronous keychain certificate stuff */
            //vpnProfile.checkForRestart(this);
        }

        /* start the OpenVPN process itself in a background thread */
        new Thread(new Runnable() {
            @Override
            public void run() {
                startOpenVPN();
            }
        }).start();


        ProfileManager.setConnectedVpnProfile(this, vpnProfile);
        VpnStatus.setConnectedVPNProfile(vpnProfile.getUUIDString());

        return START_STICKY;
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private void updateShortCutUsage(VpnProfile profile) {
        if (profile == null)
            return;
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
        shortcutManager.reportShortcutUsed(profile.getUUIDString());
    }

    private void startOpenVPN() {
        VpnStatus.logInfo(R.string.building_configration);
        VpnStatus.updateStateString("VPN_GENERATE_CONFIG", "", R.string.building_configration,
                ConnectionStatus.LEVEL_START);
        try {
            vpnProfile.writeConfigFile(this);
        } catch (IOException e) {
            logHelper.logException("Error writing config file", e);
            endVpnService();
            return;
        }
        String nativeLibraryDirectory = getApplicationInfo().nativeLibraryDir;

        // Write OpenVPN binary
        String[] argv = VPNLaunchHelper.buildOpenvpnArgv(this);


        // Set a flag that we are starting a new VPN
        isStarting = true;
        // Stop the previous session by interrupting the thread.

        stopOldOpenVPNProcess();
        // An old running VPN should now be exited
        isStarting = false;

        // Start a new session by creating a new thread.
        SharedPreferences prefs = PreferencesUtil.getDefaultSharedPreferences(this);

        isOpenvpn3 = prefs.getBoolean("ovpn3", false);
        if (!"ovpn3".equals(BuildConstants.FLAVOR))
            isOpenvpn3 = false;

        // Open the Management Interface
        if (!isOpenvpn3) {
            // start a Thread that handles incoming messages of the managment socket
            OpenVPNManagementThread ovpnManagementThread = new OpenVPNManagementThread(vpnProfile, this);
            if (ovpnManagementThread.openManagementInterface(this)) {

                Thread mSocketManagerThread = new Thread(ovpnManagementThread, "OpenVPNManagementThread");
                mSocketManagerThread.start();
                vpnManagement = ovpnManagementThread;
                VpnStatus.logInfo("started Socket Thread");
            } else {
                endVpnService();
                return;
            }
        }

        Runnable processThread;
        if (isOpenvpn3)

        {

            OpenVPNManagement mOpenVPN3 = instantiateOpenVPN3Core();
            processThread = (Runnable) mOpenVPN3;
            vpnManagement = mOpenVPN3;


        } else {
            processThread = new OpenVPNThread(this, argv, nativeLibraryDirectory);
            vpnThread = processThread;
        }

        synchronized (mProcessLock)

        {
            OpenVPNService.processThread = new Thread(processThread, "OpenVPNProcessThread");
            OpenVPNService.processThread.start();
        }

        new Handler(getMainLooper()).post(new Runnable() {
                                              @Override
                                              public void run() {
                                                  if (deviceStateReceiver != null)
                                                      unregisterDeviceStateReceiver();

                                                  registerDeviceStateReceiver(vpnManagement);
                                              }
                                          }

        );
    }

    private void stopOldOpenVPNProcess() {
        if (vpnManagement != null) {
            if (vpnThread != null)
                ((OpenVPNThread) vpnThread).setReplaceConnection();
            if (vpnManagement.stopVPN(true)) {
                // an old was asked to exit, wait 1s
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logHelper.logException(e);
                }
            }
        }

        forceStopOpenVpnProcess();
    }


    public void forceStopOpenVpnProcess() {
        synchronized (mProcessLock) {
            if (processThread != null) {
                processThread.interrupt();
                Thread.interrupted();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logHelper.logException(e);
                }
            }
        }
    }




    private OpenVPNManagement instantiateOpenVPN3Core() {
        try {
            Class cl = Class.forName("de.blinkt.openvpn.core.OpenVPNThreadv3");
            return (OpenVPNManagement) cl.getConstructor(OpenVPNService.class, VpnProfile.class).newInstance(this, vpnProfile);
        } catch (IllegalArgumentException | InstantiationException | InvocationTargetException |
                NoSuchMethodException | ClassNotFoundException | IllegalAccessException e) {
            logHelper.logException(e);
        }
        return null;
    }

    @Override
    public IBinder asBinder() {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        synchronized (mProcessLock) {
            if (processThread != null) {
                vpnManagement.stopVPN(true);
            }
        }

        if (deviceStateReceiver != null) {
            this.unregisterReceiver(deviceStateReceiver);
        }
        // Just in case unregister for state
        VpnStatus.removeStateListener(this);
    }

    private String getTunConfigString() {
        // The format of the string is not important, only that
        // two identical configurations produce the same result
        String cfg = "TUNCFG UNQIUE STRING ips:";

        if (localIP != null)
            cfg += localIP.toString();
        if (localIPv6 != null)
            cfg += localIPv6;


        cfg += "routes: " + TextUtils.join("|", routesIPv4.getNetworks(true)) +
                TextUtils.join("|", routesIPv6.getNetworks(true));
        cfg += "excl. routes:" + TextUtils.join("|", routesIPv4.getNetworks(false)) +
                TextUtils.join("|", routesIPv6.getNetworks(false));
        cfg += "dns: " + TextUtils.join("|", dnsVector);
        cfg += "domain: " + domainName;
        cfg += "mtu: " + mtu;
        return cfg;
    }

    public ParcelFileDescriptor openTun() {
        //Debug.startMethodTracing(getExternalFilesDir(null).toString() + "/opentun.trace", 40* 1024 * 1024);
        Builder builder = new Builder();
        VpnStatus.logInfo(R.string.last_openvpn_tun_config);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && vpnProfile.allowLocalLAN) {
            //allowAllAFFamilies(builder);
            //Never allow other than IPv4 for PIA
        }

        if (localIP == null && localIPv6 == null) {
            VpnStatus.logError(getString(R.string.opentun_no_ipaddr));
            return null;
        }

        if (localIP != null) {
            addLocalNetworksToRoutes();
            try {
                builder.addAddress(localIP.ip, localIP.len);
            } catch (IllegalArgumentException iae) {
                logHelper.logException(getString(R.string.dns_add_error), iae);
                return null;
            }
        }

        if (localIPv6 != null) {
            String[] ipv6parts = localIPv6.split("/");
            try {
                builder.addAddress(ipv6parts[0], Integer.parseInt(ipv6parts[1]));
            } catch (IllegalArgumentException iae) {
                logHelper.logException(getString(R.string.ip_add_error), iae);
                return null;
            }
        }

        for (String dns : dnsVector) {
            try {
                builder.addDnsServer(dns);
            } catch (IllegalArgumentException iae) {
                logHelper.logException(getString(R.string.dns_add_error), iae);
            }
        }

        String release = Build.VERSION.RELEASE;

        if ((Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT && !release.startsWith("4.4.3")
                && !release.startsWith("4.4.4") && !release.startsWith("4.4.5") && !release.startsWith("4.4.6"))
                && mtu < 1280) {
            VpnStatus.logInfo(String.format(Locale.US, "Forcing MTU to 1280 instead of %d to workaround " +
                    "Android Bug #70916", mtu));
            builder.setMtu(1280);
        } else {
            builder.setMtu(mtu);
        }

        Collection<NetworkSpace.ipAddress> positiveIPv4Routes = routesIPv4.getPositiveIPList();
        Collection<NetworkSpace.ipAddress> positiveIPv6Routes = routesIPv6.getPositiveIPList();
        if ("samsung".equals(Build.BRAND) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                dnsVector.size() >= 1) {
            // Check if the first DNS Server is in the VPN range
            try {
                NetworkSpace.ipAddress dnsServer = new NetworkSpace.ipAddress(new IPAddress(dnsVector.get(0),
                        32), true);
                boolean dnsIncluded = false;
                for (NetworkSpace.ipAddress net : positiveIPv4Routes) {
                    if (net.containsNet(dnsServer)) {
                        dnsIncluded = true;
                    }
                }
                if (!dnsIncluded) {
                    String samsungwarning = String.format("Warning Samsung Android 5.0+ devices ignore " +
                            "DNS servers outside the VPN range. To enable DNS resolution a route to your DNS " +
                            "Server (%s) has been added.", dnsVector.get(0));
                    VpnStatus.logWarning(samsungwarning);
                    positiveIPv4Routes.add(dnsServer);
                }
            } catch (Exception e) {
                logHelper.logException("Error parsing DNS Server IP: " + dnsVector.get(0), e);
            }
        }

        NetworkSpace.ipAddress multicastRange = new NetworkSpace.ipAddress(new IPAddress("224.0.0.0", 3), true);

        for (NetworkSpace.ipAddress route : positiveIPv4Routes) {
            try {
                if (multicastRange.containsNet(route))
                    VpnStatus.logDebug(R.string.ignore_multicast_route, route.toString());
                else
                    builder.addRoute(route.getIPv4Address(), route.networkMask);
            } catch (IllegalArgumentException ia) {
                logHelper.logException(getString(R.string.route_rejected) + " " + route, ia);
            }
        }

        for (NetworkSpace.ipAddress route6 : positiveIPv6Routes) {
            try {
                builder.addRoute(route6.getIPv6Address(), route6.networkMask);
            } catch (IllegalArgumentException ia) {
                logHelper.logException(getString(R.string.route_rejected) + " " + route6, ia);
            }
        }

        if (domainName != null)
            builder.addSearchDomain(domainName);

        VpnStatus.logInfo(R.string.local_ip_info, localIP.ip, localIP.len, localIPv6, mtu);
        VpnStatus.logInfo(R.string.dns_server_info, TextUtils.join(", ", dnsVector), domainName);
        VpnStatus.logInfo(R.string.routes_info_incl, TextUtils.join(", ", routesIPv4.getNetworks(true)), TextUtils.join(", ", routesIPv6.getNetworks(true)));
        VpnStatus.logInfo(R.string.routes_info_excl, TextUtils.join(", ", routesIPv4.getNetworks(false)), TextUtils.join(", ", routesIPv6.getNetworks(false)));
        VpnStatus.logDebug(R.string.routes_debug, TextUtils.join(", ", positiveIPv4Routes), TextUtils.join(", ", positiveIPv6Routes));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            setAllowedVpnPackages(builder);

        String session = vpnProfile.getIpAddress();

        if (localIP != null && localIPv6 != null)
            session = getString(R.string.session_ipv6string, session, localIP, localIPv6);

        else if (localIP != null)
            session = getString(R.string.session_ipv4string, session, localIP);

        builder.setSession(session);
        // No DNS Server, log a warning

        if (dnsVector.size() == 0)
            VpnStatus.logInfo(R.string.warn_no_dns);

        lastTunCfg = getTunConfigString();
        // Reset information
        dnsVector.clear();
        routesIPv4.clear();
        routesIPv6.clear();
        localIP = null;
        localIPv6 = null;
        domainName = null;
        builder.setConfigureIntent(getStatusPendingIntent());

        try {
            //Debug.stopMethodTracing();
            ParcelFileDescriptor tun = builder.establish();
            if (tun == null)
                throw new NullPointerException("Android establish() method returned null (Really broken " +
                        "network configuration?)");
            return tun;
        } catch (Exception e) {
            logHelper.logException(getString(R.string.tun_open_error), e);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                logHelper.logWarning(getString(R.string.tun_error_helpful));
            }
            return null;
        }
    }

    /*@TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void allowAllAFFamilies(Builder builder) {
        builder.allowFamily(OsConstants.AF_INET);
        builder.allowFamily(OsConstants.AF_INET6);
    }*/

    private void addLocalNetworksToRoutes() {
        // Add local network interfaces
        String[] localRoutes = NativeUtils.getIfconfig();
        // The format of mLocalRoutes is kind of broken because I don't really like JNI
        for (int i = 0; i < localRoutes.length; i += 3) {
            String intf = localRoutes[i];
            String ipAddr = localRoutes[i + 1];
            String netMask = localRoutes[i + 2];

            if (intf == null || intf.equals("lo") ||
                    intf.startsWith("tun") || intf.startsWith("rmnet"))
                continue;

            if (ipAddr == null || netMask == null) {
                VpnStatus.logError("Local routes are broken?! (Report to author) " + TextUtils.join("|", localRoutes));
                continue;
            }

            if (ipAddr.equals(localIP.ip))
                continue;

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT && !vpnProfile.allowLocalLAN) {
                routesIPv4.addIPSplit(new IPAddress(ipAddr, netMask), true);

            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && vpnProfile.allowLocalLAN)
                routesIPv4.addIP(new IPAddress(ipAddr, netMask), false);
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setAllowedVpnPackages(Builder builder) {
        boolean atLeastOneAllowedApp = false;
        for (String pkg : vpnProfile.allowedAppsVpn) {
            try {
                if (vpnProfile.allowedAppsVpnAreDisallowed) {
                    builder.addDisallowedApplication(pkg);
                } else {
                    builder.addAllowedApplication(pkg);
                    atLeastOneAllowedApp = true;
                }
            } catch (PackageManager.NameNotFoundException e) {
                vpnProfile.allowedAppsVpn.remove(pkg);
                logHelper.logException(e);
                logHelper.logInfo(getString(R.string.app_no_longer_exists));
            }
        }

        if (!vpnProfile.allowedAppsVpnAreDisallowed && !atLeastOneAllowedApp) {
            VpnStatus.logDebug(R.string.no_allowed_app, getPackageName());
            try {
                builder.addAllowedApplication(getPackageName());
            } catch (PackageManager.NameNotFoundException e) {
                logHelper.logException(e);
            }
        }

        if (vpnProfile.allowedAppsVpnAreDisallowed) {
            VpnStatus.logDebug(R.string.disallowed_vpn_apps_info, TextUtils.join(", ", vpnProfile.allowedAppsVpn));
        } else {
            VpnStatus.logDebug(R.string.allowed_vpn_apps_info, TextUtils.join(", ", vpnProfile.allowedAppsVpn));
        }
    }


    public void addDNS(String dns) {
        dnsVector.add(dns);
    }


    public void setDomain(String domain) {
        if (domainName == null)
            domainName = domain;
    }

    /**
     * Route that is always included, used by the v3 core
     */
    public void addRoute(IPAddress route) {
        routesIPv4.addIP(route, true);
    }


    public void addRoute(String dest, String mask, String gateway, String device) {
        IPAddress route = new IPAddress(dest, mask);
        boolean include = isAndroidTunDevice(device);
        NetworkSpace.ipAddress gatewayIP = new NetworkSpace.ipAddress(new IPAddress(gateway, 32), false);

        if (localIP == null) {
            VpnStatus.logError("Local IP address unset and received. Neither pushed server config nor local config specifies an IP addresses. Opening tun device is most likely going to fail.");
            return;
        }

        NetworkSpace.ipAddress localNet = new NetworkSpace.ipAddress(localIP, true);

        if (localNet.containsNet(gatewayIP))
            include = true;

        if (gateway != null &&
                (gateway.equals("255.255.255.255") || gateway.equals(remoteGW)))
            include = true;

        if (route.len == 32 && !mask.equals("255.255.255.255")) {
            VpnStatus.logWarning(R.string.route_not_cidr, dest, mask);
        }

        if (route.normalise())
            VpnStatus.logWarning(R.string.route_not_netip, dest, route.len, route.ip);

        routesIPv4.addIP(route, include);
    }


    public void addRoutev6(String network, String device) {
        String[] v6parts = network.split("/");
        boolean included = isAndroidTunDevice(device);
        try {
            Inet6Address ip = (Inet6Address) InetAddress.getAllByName(v6parts[0])[0];
            int mask = Integer.parseInt(v6parts[1]);
            routesIPv6.addIPv6(ip, mask, included);
        } catch (UnknownHostException e) {
            logHelper.logException(e);
        }
    }


    private boolean isAndroidTunDevice(String device) {
        return device != null &&
                (device.startsWith("tun") || "(null)".equals(device) || "vpnservice-tun".equals(device));
    }


    public void setMtu(int mtu) {
        this.mtu = mtu;
    }


    public void setLocalIP(IPAddress cdrip) {
        localIP = cdrip;
    }


    public void setLocalIP(String local, String netmask, int mtu, String mode) {
        localIP = new IPAddress(local, netmask);
        this.mtu = mtu;
        remoteGW = null;
        long netMaskAsInt = IPAddress.getInt(netmask);
        if (localIP.len == 32 && !netmask.equals("255.255.255.255")) {
            // get the netmask as IP
            int masklen;
            long mask;
            if ("net30".equals(mode)) {
                masklen = 30;
                mask = 0xfffffffc;
            } else {
                masklen = 31;
                mask = 0xfffffffe;
            }
            // Netmask is Ip address +/-1, assume net30/p2p with small net
            if ((netMaskAsInt & mask) == (localIP.getInt() & mask)) {
                localIP.len = masklen;
            } else {
                localIP.len = 32;
                if (!"p2p".equals(mode))
                    VpnStatus.logWarning(R.string.ip_not_cidr, local, netmask, mode);
            }
        }
        if (("p2p".equals(mode) && localIP.len < 32) || ("net30".equals(mode) && localIP.len < 30)) {
            VpnStatus.logWarning(R.string.ip_looks_like_subnet, local, netmask, mode);
        }

        /* Workaround for Lollipop, it  does not route traffic to the VPNs own network mask */
        if (localIP.len <= 31 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            IPAddress interfaceRoute = new IPAddress(localIP.ip, localIP.len);
            interfaceRoute.normalise();
            addRoute(interfaceRoute);
        }
        // Configurations are sometimes really broken...
        remoteGW = netmask;
    }


    public void setLocalIPv6(String ipv6addr) {
        localIPv6 = ipv6addr;
    }


    @Override
    public void updateState(String state, String logmessage, int resid, ConnectionStatus level) {
        doSendBroadcast(state, level);
        if (processThread == null && !notificationsAlwaysVisible)
            return;
        int priority = PRIORITY_DEFAULT;
        // Display byte count only after being connected
        if (level == LEVEL_WAITING_FOR_USER_INPUT) {
            // The user is presented a dialog of some kind, no need to inform the user
            // with a notifcation
            return;
        } else if (level == LEVEL_CONNECTED) {
            displayByteCount = true;
            connectTime = System.currentTimeMillis();
            if (!runningOnAndroidTV())
                priority = PRIORITY_MIN;
        } else
            displayByteCount = false;
        String msg = getString(resid);
        showNotification(VpnStatus.getLastCleanLogMessage(this),
                msg, priority, 0, level);
    }


    @Override
    public void setConnectedVPN(String uuid) {
    }


    private void doSendBroadcast(String state, ConnectionStatus level) {
        Intent vpnstatus = new Intent();
        vpnstatus.setAction("de.blinkt.openvpn.VPN_STATUS");
        vpnstatus.putExtra("status", level.toString());
        vpnstatus.putExtra("detailstatus", state);
        sendBroadcast(vpnstatus, Manifest.permission.ACCESS_NETWORK_STATE);
    }


    @Override
    public void updateByteCount(long in, long out, long diffIn, long diffOut) {
        if (displayByteCount) {
            String netstat = String.format(getString(R.string.statusline_bytecount),
                    humanReadableByteCount(in, false),
                    humanReadableByteCount(diffIn / OpenVPNManagement.mBytecountInterval, true),
                    humanReadableByteCount(out, false),
                    humanReadableByteCount(diffOut / OpenVPNManagement.mBytecountInterval, true));
            int priority = notificationsAlwaysVisible ? PRIORITY_DEFAULT : PRIORITY_MIN;
            showNotification(netstat, null, priority, connectTime, LEVEL_CONNECTED);
        }
    }


    @Override
    public boolean handleMessage(Message msg) {
        Runnable r = msg.getCallback();
        if (r != null) {
            r.run();
            return true;
        } else {
            return false;
        }
    }


    public OpenVPNManagement getManagement() {
        return vpnManagement;
    }


    public String getTunReopenStatus() {
        String currentConfiguration = getTunConfigString();
        if (currentConfiguration.equals(lastTunCfg)) {
            return "NOACTION";
        } else {
            String release = Build.VERSION.RELEASE;
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT && !release.startsWith("4.4.3")
                    && !release.startsWith("4.4.4") && !release.startsWith("4.4.5") && !release.startsWith("4.4.6"))
                // There will be probably no 4.4.4 or 4.4.5 version, so don't waste effort to do parsing here
                return "OPEN_AFTER_CLOSE";
            else
                return "OPEN_BEFORE_CLOSE";
        }
    }


    public void requestInputFromUser(int resid, String needed) {
        VpnStatus.updateStateString("NEED", "need " + needed, resid, LEVEL_WAITING_FOR_USER_INPUT);
        showNotification(getString(resid), getString(resid), PRIORITY_MAX, 0, LEVEL_WAITING_FOR_USER_INPUT);
    }
}
