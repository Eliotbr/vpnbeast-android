package com.b.android.openvpn60.model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import de.blinkt.openvpn.core.NativeUtils;

import com.b.android.openvpn60.R;
import com.b.android.openvpn60.core.OpenVPNService;
import com.b.android.openvpn60.util.PasswordUtil;
import com.b.android.openvpn60.helper.VPNLaunchHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Created by b on 5/15/17.
 */

public class VpnProfile implements Parcelable, Serializable, Cloneable {

    public transient static final long MAX_EMBED_FILE_SIZE = 2048 * 1024; // 2048kB
    public static final String INLINE_TAG = "[[INLINE]]";
    public static final String DISPLAYNAME_TAG = "[[NAME]]";
    private static final String TAG = "com.b.android.openvpn." + VpnProfile.class.toString();
    private static final long serialVersionUID = 7085688938959334563L;
    public static final int MAXLOGLEVEL = 4;
    public static final int CURRENT_PROFILE_VERSION = 6;
    public static final int DEFAULT_MSSFIX_SIZE = 1280;
    public static String DEFAULT_DNS1 = "8.8.8.8";
    public static String DEFAULT_DNS2 = "8.8.4.4";
    public static final int TYPE_CERTIFICATES = 0;
    public static final int TYPE_PKCS12 = 1;
    public static final int TYPE_KEYSTORE = 2;
    public static final int TYPE_USERPASS = 3;
    public static final int TYPE_STATICKEYS = 4;
    public static final int TYPE_USERPASS_CERTIFICATES = 5;
    public static final int TYPE_USERPASS_PKCS12 = 6;
    public static final int TYPE_USERPASS_KEYSTORE = 7;
    public static final int X509_VERIFY_TLSREMOTE = 0;
    public static final int X509_VERIFY_TLSREMOTE_COMPAT_NOREMAPPING = 1;
    public static final int X509_VERIFY_TLSREMOTE_DN = 2;
    public static final int X509_VERIFY_TLSREMOTE_RDN = 3;
    public static final int X509_VERIFY_TLSREMOTE_RDN_PREFIX = 4;
    private transient boolean profileDeleted = false;
    private int mAuthenticationType = TYPE_KEYSTORE;
    public String name;
    private String aliasName;
    private String clientCertFilename;
    private String tlsAuthDirection = "";
    private String tlsAuthFilename;
    private String clientKeyFilename;
    private String caFilename;
    private boolean useLzo = true;
    private String pkcs12Filename;
    private String pkcs12Password;
    private boolean useTLSAuth = false;
    private String DNS1 = DEFAULT_DNS1;
    private String DNS2 = DEFAULT_DNS2;
    private String ipv4Address;
    private String mIPv6Address;
    private boolean overrideDNS = false;
    private boolean useDefaultRoute = true;
    private boolean usePull = true;
    private String customRoutes;
    private boolean checkRemoteCN = true;
    private boolean expectTLSCert = false;
    private String remoteCN = "";
    private String password = ""; //NOSONAR
    private String userName = "";
    private boolean routenopull = false;
    private boolean useRandomHostname = false;
    private boolean useFloat = false;
    private boolean useCustomConfig = false;
    private String customConfigOptions = "";
    private String verb = "1";  //ignored
    private String cipher = "";
    private boolean nobind = false;
    private boolean useDefaultRoutev6 = true;
    private String customRoutesv6 = "";
    private String keyPassword = ""; //NOSONAR
    public boolean persistTun = false;
    private String connectRetryMax = "-1";
    private String connectRetry = "2";
    private String connectRetryMaxTime = "300";
    private String auth = "";
    private int x509AuthType = X509_VERIFY_TLSREMOTE_RDN;
    private String x509UsernameField = null;
    private transient PrivateKey privateKey;
    private UUID uuid;
    public boolean allowLocalLAN;
    private int profileVersion;
    private String excludedRoutes;
    private int mssFix = 0; // -1 is default,
    public Connection[] connections = new Connection[0];
    private boolean remoteRandom = false;
    public HashSet<String> allowedAppsVpn = new HashSet<>();
    public boolean allowedAppsVpnAreDisallowed = true;
    private String crlFilename;
    private String profileCreator;
    public long ping;
    private boolean pushPeerInfo = false;
    private static final boolean isOpenVPN22 = false;
    public int version = 0;
    public long lastUsed;
    private String serverName = "openvpn.example.com";
    private String serverPort = "1194";
    private boolean useUdp = true;
    private String serverCert;


    public VpnProfile(String name) {
        connections = new Connection[1];
        connections[0] = new Connection();
        initConstants();
        this.name = name;
    }

    public VpnProfile(String uuid, String name, String ipAddress, String serverPort, String serverCert) {
        connections = new Connection[1];
        connections[0] = new Connection(ipAddress, serverPort);
        this.name = name;
        this.uuid = UUID.fromString(uuid);
        this.serverCert = serverCert;
        initConstants();
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public VpnProfile createFromParcel(Parcel in) {
            return new VpnProfile(in);
        }
        public VpnProfile[] newArray(int size) {
            return new VpnProfile[size];
        }
    };

    // Parcelling part
    public VpnProfile(Parcel in) {
        this.uuid = UUID.fromString(in.readString());
        this.name = in.readString();
        connections = new Connection[1];
        connections[0] = new Connection(in.readString(), in.readString());
        //connections[0].serverName = in.readString();
        //connections[0].serverPort = in.readString();
        serverCert = in.readString();
        initConstants();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.uuid.toString());
        dest.writeString(this.name);
        dest.writeString(connections[0].serverName);
        dest.writeString(connections[0].serverPort);
        dest.writeString(this.serverCert);
    }


    public String getIpAddress() {
        return connections[0].serverName;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getPing() {
        return ping;
    }

    private void initConstants() {
        profileVersion = CURRENT_PROFILE_VERSION;
        lastUsed = System.currentTimeMillis();
        profileDeleted = false;
        mAuthenticationType = 3;
        aliasName = null;
        clientCertFilename = null;
        tlsAuthDirection = "";
        tlsAuthFilename = null;
        clientKeyFilename = null;
        caFilename = INLINE_TAG + serverCert;
        useLzo = true;
        pkcs12Filename = null;
        pkcs12Password = null;
        useTLSAuth = false;
        DNS1 = "8.8.8.8";
        DNS2 = "8.8.4.4";
        ipv4Address = null;
        mIPv6Address = null;
        overrideDNS = false;
        useDefaultRoute = false;
        usePull = true;
        customRoutes = null;
        checkRemoteCN = false;
        expectTLSCert = false;
        remoteCN = "";
        password = ""; //NOSONAR
        userName = "";
        routenopull = false;
        useRandomHostname = false;
        useFloat = false;
        useCustomConfig = false;
        customConfigOptions = "";
        verb = "3";
        cipher = "AES-256-CBC";
        nobind = true;
        useDefaultRoutev6 = false;
        customRoutesv6 = "";
        keyPassword = ""; //NOSONAR
        persistTun = true;
        connectRetryMax = "-1";
        connectRetry = "2";
        connectRetryMaxTime = "300";
        auth = "SHA256";
        x509AuthType = 3;
        x509UsernameField = null;
        privateKey = null;
        allowLocalLAN = true;
        excludedRoutes = null;
        mssFix = 0;
        remoteRandom = false;
        allowedAppsVpn = new HashSet<>();
        allowedAppsVpnAreDisallowed = true;
        crlFilename = null;
        profileCreator = null;
        ping = 0;
        pushPeerInfo = false;
        version = 2;
        lastUsed = System.currentTimeMillis();
        serverName = name;
        serverPort = "1194";
        useUdp = true;
    }

    public static String openVpnEscape(String unescaped) {
        if (unescaped == null)
            return null;
        String escapedString = unescaped.replace("\\", "\\\\");
        escapedString = escapedString.replace("\"", "\\\"");
        escapedString = escapedString.replace("\n", "\\n");

        if (escapedString.equals(unescaped) && !escapedString.contains(" ") &&
                !escapedString.contains("#") && !escapedString.contains(";")
                && !escapedString.equals(""))
            return unescaped;
        else
            return '"' + escapedString + '"';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VpnProfile) {
            VpnProfile vpnProfile = (VpnProfile) obj;
            return uuid.equals(vpnProfile.uuid);
        } else {
            return false;
        }
    }

    public UUID getUUID() {
        return uuid;

    }

    public String getName() {
        if (TextUtils.isEmpty(name))
            return "No profile name";
        return name;
    }

    public void upgradeProfile() {
        if (profileVersion < 2)
            /* default to the behaviour the OS used */
            allowLocalLAN = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT;
        if (profileVersion < 4) {
            moveOptionsToConnection();
            allowedAppsVpnAreDisallowed = true;
        }

        if (allowedAppsVpn == null)
            allowedAppsVpn = new HashSet<>();

        if (connections == null)
            connections = new Connection[0];

        profileVersion = CURRENT_PROFILE_VERSION;
    }

    private void moveOptionsToConnection() {
        connections = new Connection[1];
        Connection conn = new Connection();
        conn.serverName = serverName;
        conn.serverPort = serverPort;
        conn.isUdp = useUdp;
        conn.customConfiguration = "";
        connections[0] = conn;
    }

    public String getConfigFile(Context context, boolean configForOvpn3) {
        File cacheDir = context.getCacheDir();
        String cfg = "";
        // Enable management interface
        cfg += "# Enables connection to GUI\n";
        cfg += "management ";
        cfg += cacheDir.getAbsolutePath() + "/" + "mgmtsocket";
        cfg += " unix\n";
        cfg += "management-client\n";
        // Not needed, see updated man page in 2.3
        //cfg += "management-signal\n";
        cfg += "management-query-passwords\n";
        cfg += "management-hold\n\n";
        if (!configForOvpn3) {
            cfg += String.format("setenv IV_GUI_VER %s \n", openVpnEscape(getVersionEnvString(context)));
            String versionString = String.format(Locale.US, "%d %s %s %s %s %s", Build.VERSION.SDK_INT, Build.VERSION.RELEASE,
                    NativeUtils.getNativeAPI(), Build.BRAND, Build.BOARD, Build.MODEL);
            cfg += String.format("setenv IV_PLAT_VER %s\n", openVpnEscape(versionString));
        }
        cfg += "machine-readable-output\n";
        cfg += "allow-recursive-routing\n";
        // Users are confused by warnings that are misleading...
        cfg += "ifconfig-nowarn\n";

        boolean useTLSClient = (mAuthenticationType != TYPE_STATICKEYS);

        if (useTLSClient && usePull)
            cfg += "client\n";
        else if (usePull)
            cfg += "pull\n";
        else if (useTLSClient)
            cfg += "tls-client\n";

        cfg += "verb " + MAXLOGLEVEL + "\n";

        if (connectRetryMax == null) {
            connectRetryMax = "-1";
        }

        if (!connectRetryMax.equals("-1"))
            cfg += "connect-retry-max " + connectRetryMax + "\n";

        if (TextUtils.isEmpty(connectRetry))
            connectRetry = "2";

        if (TextUtils.isEmpty(connectRetryMaxTime))
            connectRetryMaxTime = "300";

        if (!isOpenVPN22)
            cfg += "connect-retry " + connectRetry + " " + connectRetryMaxTime + "\n";
        else if (isOpenVPN22 && useUdp)
            cfg += "connect-retry " + connectRetry + "\n";


        cfg += "resolv-retry 60\n";


        // We cannot use anything else than tun
        cfg += "dev tun\n";


        boolean canUsePlainRemotes = true;

        if (connections.length == 1) {
            cfg += connections[0].getConnectionBlock();
        } else {
            for (Connection conn : connections) {
                canUsePlainRemotes = canUsePlainRemotes && conn.isOnlyRemote();
            }

            if (remoteRandom)
                cfg += "remote-random\n";

            if (canUsePlainRemotes) {
                for (Connection conn : connections) {
                    if (conn.isEnabled) {
                        cfg += conn.getConnectionBlock();
                    }
                }
            }
        }

        switch (mAuthenticationType) {
            case VpnProfile.TYPE_USERPASS_CERTIFICATES:
                cfg += "auth-user-pass\n";
                break;

            case VpnProfile.TYPE_CERTIFICATES:
                // Ca
                cfg += insertFileData("ca", caFilename);
                // Client Cert + Key
                cfg += insertFileData("key", clientKeyFilename);
                cfg += insertFileData("cert", clientCertFilename);
                break;

            case VpnProfile.TYPE_USERPASS_PKCS12:
                cfg += "auth-user-pass\n";
                break;

            case VpnProfile.TYPE_PKCS12:
                cfg += insertFileData("pkcs12", pkcs12Filename);
                break;

            case VpnProfile.TYPE_USERPASS_KEYSTORE:
                cfg += "auth-user-pass\n";
                break;

            case VpnProfile.TYPE_USERPASS:
                cfg += "auth-user-pass\n";
                cfg += insertFileData("ca", caFilename);
                break;

            default:
                throw new IllegalStateException("Invalid auth type");
        }

        if (!TextUtils.isEmpty(crlFilename))
            cfg += insertFileData("crl-verify", crlFilename);

        if (useLzo) {
            cfg += "comp-lzo\n";
        }

        if (useTLSAuth) {
            boolean useTlsCrypt = tlsAuthDirection.equals("tls-crypt");

            if (mAuthenticationType == TYPE_STATICKEYS)
                cfg += insertFileData("secret", tlsAuthFilename);
            else if (useTlsCrypt)
                cfg += insertFileData("tls-crypt", tlsAuthFilename);
            else
                cfg += insertFileData("tls-auth", tlsAuthFilename);

            if (!TextUtils.isEmpty(tlsAuthDirection) && !useTlsCrypt) {
                cfg += "key-direction ";
                cfg += tlsAuthDirection;
                cfg += "\n";
            }
        }

        if (!usePull) {
            if (!TextUtils.isEmpty(ipv4Address))
                cfg += "ifconfig " + cidrToIPAndNetmask(ipv4Address) + "\n";

            if (!TextUtils.isEmpty(mIPv6Address))
                cfg += "ifconfig-ipv6 " + mIPv6Address + "\n";
        }

        if (usePull && routenopull)
            cfg += "route-nopull\n";

        String routes = "";

        if (useDefaultRoute)
            routes += "route 0.0.0.0 0.0.0.0 vpn_gateway\n";
        else {
            for (String route : getCustomRoutes(customRoutes)) {
                routes += "route " + route + " vpn_gateway\n";
            }

            for (String route : getCustomRoutes(excludedRoutes)) {
                routes += "route " + route + " net_gateway\n";
            }
        }

        if (useDefaultRoutev6)
            cfg += "route-ipv6 ::/0\n";
        else
            for (String route : getCustomRoutesv6(customRoutesv6)) {
                routes += "route-ipv6 " + route + "\n";
            }

        cfg += routes;

        if (overrideDNS || !usePull) {
            if (!TextUtils.isEmpty(DNS1)) {
                if (DNS1.contains(":"))
                    cfg += "dhcp-option DNS6 " + DNS1 + "\n";
                else
                    cfg += "dhcp-option DNS " + DNS1 + "\n";
            } if (!TextUtils.isEmpty(DNS2)) {
                if (DNS2.contains(":"))
                    cfg += "dhcp-option DNS6 " + DNS2 + "\n";
                else
                    cfg += "dhcp-option DNS " + DNS2 + "\n";
            }
            /*if (!TextUtils.isEmpty(mSearchDomain))
                cfg += "dhcp-option DOMAIN " + mSearchDomain + "\n";*/

        }

        if (mssFix != 0) {
            if (mssFix != 1450) {
                cfg += String.format(Locale.US, "mssfix %d\n", mssFix);
            } else
                cfg += "mssfix\n";
        }

        if (nobind)
            cfg += "nobind\n";


        // Authentication
        if (mAuthenticationType != TYPE_STATICKEYS) {
            if (checkRemoteCN) {
                if (remoteCN == null || remoteCN.equals(""))
                    cfg += "verify-x509-name " + openVpnEscape(connections[0].serverName) + " name\n";
                else
                    switch (x509AuthType) {

                        // 2.2 style x509 checks
                        case X509_VERIFY_TLSREMOTE_COMPAT_NOREMAPPING:
                            cfg += "compat-names no-remapping\n";
                            break;
                        case X509_VERIFY_TLSREMOTE:
                            cfg += "tls-remote " + openVpnEscape(remoteCN) + "\n";
                            break;

                        case X509_VERIFY_TLSREMOTE_RDN:
                            cfg += "verify-x509-name " + openVpnEscape(remoteCN) + " name\n";
                            break;

                        case X509_VERIFY_TLSREMOTE_RDN_PREFIX:
                            cfg += "verify-x509-name " + openVpnEscape(remoteCN) + " name-prefix\n";
                            break;

                        case X509_VERIFY_TLSREMOTE_DN:
                            cfg += "verify-x509-name " + openVpnEscape(remoteCN) + "\n";
                            break;
                    }
                if (!TextUtils.isEmpty(x509UsernameField))
                    cfg += "x509-username-field " + openVpnEscape(x509UsernameField) + "\n";
            }
            if (expectTLSCert)
                cfg += "remote-cert-tls server\n";
        }

        if (!TextUtils.isEmpty(cipher)) {
            cfg += "cipher " + cipher + "\n";
        }

        if (!TextUtils.isEmpty(auth)) {
            cfg += "auth " + auth + "\n";
        }

        // Obscure Settings dialog
        if (useRandomHostname)
            cfg += "#my favorite options :)\nremote-random-hostname\n";

        if (useFloat)
            cfg += "float\n";

        if (persistTun) {
            cfg += "persist-tun\n";
            cfg += "# persist-tun also enables pre resolving to avoid DNS resolve problem\n";
            cfg += "preresolve\n";
        }

        if (pushPeerInfo)
            cfg += "push-peer-info\n";

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean usesystemproxy = prefs.getBoolean("usesystemproxy", true);
        if (usesystemproxy && !isOpenVPN22) {
            cfg += "# Use system proxy setting\n";
            cfg += "management-query-proxy\n";
        }


        if (useCustomConfig) {
            cfg += "# Custom configuration options\n";
            cfg += "# You are on your on own here :)\n";
            cfg += customConfigOptions;
            cfg += "\n";

        }

        if (!canUsePlainRemotes) {
            cfg += "# Connection Options are at the end to allow global options (and global custom options) to influence connection blocks\n";
            for (Connection conn : connections) {
                if (conn.isEnabled) {
                    cfg += "<connection>\n";
                    cfg += conn.getConnectionBlock();
                    cfg += "</connection>\n";
                }
            }
        }


        return cfg;
    }

    public String getVersionEnvString(Context c) {
        String version = "unknown";
        try {
            PackageInfo packageinfo = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            version = packageinfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "getVersionEnvString: ", e);
        }
        return String.format(Locale.US, "%s %s", c.getPackageName(), version);

    }

    //! Put inline data inline and other data as normal escaped filename
    public static String insertFileData(String cfgentry, String filedata) {
        if (filedata == null) {
            return String.format("%s %s\n", cfgentry, "file missing in config profile");
        } else if (isEmbedded(filedata)) {
            String dataWithOutHeader = getEmbeddedContent(filedata);
            return String.format(Locale.ENGLISH, "<%s>\n%s\n</%s>\n", cfgentry, dataWithOutHeader, cfgentry);
        } else {
            return String.format(Locale.ENGLISH, "%s %s\n", cfgentry, openVpnEscape(filedata));
        }
    }

    @NonNull
    private Collection<String> getCustomRoutes(String routes) {
        ArrayList<String> cidrRoutes = new ArrayList<>();
        if (routes == null) {
            // No routes set, return empty vector
            return cidrRoutes;
        }
        for (String route : routes.split("[\n \t]")) {
            if (!route.equals("")) {
                String cidrroute = cidrToIPAndNetmask(route);
                if (cidrroute == null)
                    return cidrRoutes;

                cidrRoutes.add(cidrroute);
            }
        }

        return cidrRoutes;
    }

    private Collection<String> getCustomRoutesv6(String routes) {
        ArrayList<String> cidrRoutes = new ArrayList<>();
        if (routes == null) {
            // No routes set, return empty vector
            return cidrRoutes;
        }
        for (String route : routes.split("[\n \t]")) {
            if (!route.equals("")) {
                cidrRoutes.add(route);
            }
        }

        return cidrRoutes;
    }

    private String cidrToIPAndNetmask(String route) {
        String[] parts = route.split("/");

        // No /xx, assume /32 as netmask
        if (parts.length == 1)
            parts = (route + "/32").split("/");

        if (parts.length != 2)
            return null;
        int len;
        try {
            len = Integer.parseInt(parts[1]);
        } catch (NumberFormatException ne) {
            return null;
        }
        if (len < 0 || len > 32)
            return null;


        long nm = 0xffffffffL;
        nm = (nm << (32 - len)) & 0xffffffffL;

        String netmask = String.format(Locale.ENGLISH, "%d.%d.%d.%d", (nm & 0xff000000) >> 24, (nm & 0xff0000) >> 16, (nm & 0xff00) >> 8, nm & 0xff);
        return parts[0] + "  " + netmask;
    }

    public Intent prepareStartService(Context context) {
        return getStartServiceIntent(context);
    }

    public void writeConfigFile(Context context) throws IOException {
        FileWriter cfg = null;
        try {
            cfg = new FileWriter(VPNLaunchHelper.getConfigFilePath(context));
            cfg.write(getConfigFile(context, false));
            cfg.flush();
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            if (cfg != null)
                cfg.close();
        }
    }

    public Intent getStartServiceIntent(Context context) {
        String prefix = context.getPackageName();

        Intent intent = new Intent(context, OpenVPNService.class);
        intent.putExtra(prefix + ".profileUUID", uuid.toString());
        intent.putExtra(prefix + ".profileVersion", version);
        return intent;
    }

    public static String getDisplayName(String embeddedFile) {
        int start = DISPLAYNAME_TAG.length();
        int end = embeddedFile.indexOf(INLINE_TAG);
        return embeddedFile.substring(start, end);
    }

    public static String getEmbeddedContent(String data) {
        if (!data.contains(INLINE_TAG))
            return data;

        int start = data.indexOf(INLINE_TAG) + INLINE_TAG.length();
        return data.substring(start);
    }

    public static boolean isEmbedded(String data) {
        if (data == null)
            return false;
        if (data.startsWith(INLINE_TAG) || data.startsWith(DISPLAYNAME_TAG))
            return true;
        else
            return false;
    }



    @Override
    protected VpnProfile clone() throws CloneNotSupportedException {
        VpnProfile copy = (VpnProfile) super.clone();
        copy.uuid = UUID.randomUUID();
        copy.connections = new Connection[connections.length];
        int i = 0;
        for (Connection conn : connections) {
            copy.connections[i++] = conn.clone();
        }
        copy.allowedAppsVpn = (HashSet<String>) allowedAppsVpn.clone();
        return copy;
    }

    public VpnProfile copy(String name) {
        try {
            VpnProfile copy = clone();
            copy.name = name;
            return copy;

        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }


    class NoCertReturnedException extends Exception {
        public NoCertReturnedException(String msg) {
            super(msg);
        }
    }

    //! Return an error if something is wrong
    public int checkProfile(Context context) {
        if (mAuthenticationType == TYPE_KEYSTORE || mAuthenticationType == TYPE_USERPASS_KEYSTORE) {
            if (aliasName == null)
                return R.string.no_keystore_cert_selected;
        } else if (mAuthenticationType == TYPE_CERTIFICATES || mAuthenticationType == TYPE_USERPASS_CERTIFICATES){
            if (TextUtils.isEmpty(caFilename))
                return R.string.no_ca_cert_selected;
        }

        if (checkRemoteCN && x509AuthType==X509_VERIFY_TLSREMOTE)
            return R.string.deprecated_tls_remote;

        if (!usePull || mAuthenticationType == TYPE_STATICKEYS) {
            if (ipv4Address == null || cidrToIPAndNetmask(ipv4Address) == null)
                return R.string.ipv4_format_error;
        }
        if (!useDefaultRoute) {
            if (!TextUtils.isEmpty(customRoutes) && getCustomRoutes(customRoutes).size() == 0)
                return R.string.custom_route_format_error;

            if (!TextUtils.isEmpty(excludedRoutes) && getCustomRoutes(excludedRoutes).size() == 0)
                return R.string.custom_route_format_error;

        }

        if (useTLSAuth && TextUtils.isEmpty(tlsAuthFilename))
            return R.string.missing_tlsauth;

        if ((mAuthenticationType == TYPE_USERPASS_CERTIFICATES || mAuthenticationType == TYPE_CERTIFICATES)
                && (TextUtils.isEmpty(clientCertFilename) || TextUtils.isEmpty(clientKeyFilename)))
            return R.string.missing_certificates;

        if ((mAuthenticationType == TYPE_CERTIFICATES || mAuthenticationType == TYPE_USERPASS_CERTIFICATES)
                && TextUtils.isEmpty(caFilename))
            return R.string.missing_ca_certificate;


        boolean noRemoteEnabled = true;
        for (Connection c : connections)
            if (c.isEnabled)
                noRemoteEnabled = false;

        if (noRemoteEnabled)
            return R.string.remote_no_server_selected;

        // Everything okay
        return R.string.no_error_found;

    }

    //! Openvpn asks for a "Private Key", this should be pkcs12 key
    //
    public String getPasswordPrivateKey() {
        String cachedPw = PasswordUtil.getPKCS12orCertificatePassword(uuid, true);
        if (cachedPw != null) {
            return cachedPw;
        }
        switch (mAuthenticationType) {
            case TYPE_PKCS12:
            case TYPE_USERPASS_PKCS12:
                return pkcs12Password;

            case TYPE_CERTIFICATES:
            case TYPE_USERPASS_CERTIFICATES:
                return keyPassword;

            case TYPE_USERPASS:
            case TYPE_STATICKEYS:
            default:
                return null;
        }
    }

    public boolean isUserPWAuth() {
        switch (mAuthenticationType) {
            case TYPE_USERPASS:
            case TYPE_USERPASS_CERTIFICATES:
            case TYPE_USERPASS_KEYSTORE:
            case TYPE_USERPASS_PKCS12:
                return true;
            default:
                return false;

        }
    }

    public boolean requireTLSKeyPassword() {
        if (TextUtils.isEmpty(clientKeyFilename))
            return false;

        String data = "";
        if (isEmbedded(clientKeyFilename))
            data = clientKeyFilename;
        else {
            char[] buf = new char[2048];
            FileReader fr;
            try {
                fr = new FileReader(clientKeyFilename);
                int len = fr.read(buf);
                while (len > 0) {
                    data += new String(buf, 0, len);
                    len = fr.read(buf);
                }
                fr.close();
            } catch (FileNotFoundException e) {
                return false;
            } catch (IOException e) {
                return false;
            }

        }

        if (data.contains("Proc-Type: 4,ENCRYPTED"))
            return true;
        else if (data.contains("-----BEGIN ENCRYPTED PRIVATE KEY-----"))
            return true;
        else
            return false;
    }

    public int needUserPWInput(String transientCertOrPkcs12PW, String mTransientAuthPW) {
        if ((mAuthenticationType == TYPE_PKCS12 || mAuthenticationType == TYPE_USERPASS_PKCS12) &&
                (pkcs12Password == null || pkcs12Password.equals(""))) {
            if (transientCertOrPkcs12PW == null)
                return R.string.pkcs12_file_encryption_key;
        }

        if (mAuthenticationType == TYPE_CERTIFICATES || mAuthenticationType == TYPE_USERPASS_CERTIFICATES) {
            if (requireTLSKeyPassword() && TextUtils.isEmpty(keyPassword))
                if (transientCertOrPkcs12PW == null) {
                    return R.string.private_key_password;
                }
        }

        if (isUserPWAuth() &&
                (TextUtils.isEmpty(userName) ||
                        (TextUtils.isEmpty(password) && mTransientAuthPW == null))) {
            return R.string.password;
        }
        return 0;
    }

    public String getPasswordAuth() {
        String cachedPw = PasswordUtil.getAuthPassword(uuid, true);
        if (cachedPw != null) {
            return cachedPw;
        } else {
            return password;
        }
    }


    @Override
    public String toString() {
        return serverName + "\n" + connections[0].serverName + "\n" + connections[0].serverPort;
    }

    public String getUUIDString() {
        return uuid.toString();
    }

    public PrivateKey getKeystoreKey() {
        return privateKey;
    }

    public String getSignedData(String b64data) {
        PrivateKey privkey = getKeystoreKey();

        byte[] data = Base64.decode(b64data, Base64.DEFAULT);

        // The Jelly Bean *evil* Hack
        // 4.2 implements the RSA/ECB/PKCS1PADDING in the OpenSSLprovider
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
            return processSignJellyBeans(privkey, data);
        }


        try {

            /* ECB is perfectly fine in this special case, since we are using it for
               the public/private part in the TLS exchange
             */
            @SuppressLint("GetInstance")
            Cipher rsaSigner = Cipher.getInstance("RSA/ECB/PKCS1PADDING");

            rsaSigner.init(Cipher.ENCRYPT_MODE, privkey);

            byte[] signed_bytes = rsaSigner.doFinal(data);
            return Base64.encodeToString(signed_bytes, Base64.NO_WRAP);

        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException
                | BadPaddingException | NoSuchPaddingException e) {
            Log.e(TAG, "getSignedData: ", e);
            return null;
        }
    }

    private String processSignJellyBeans(PrivateKey privkey, byte[] data) {
        try {
            Method getKey = privkey.getClass().getSuperclass().getDeclaredMethod("getOpenSSLKey");
            getKey.setAccessible(true);

            // Real object type is OpenSSLKey
            Object opensslkey = getKey.invoke(privkey);

            getKey.setAccessible(false);

            Method getPkeyContext = opensslkey.getClass().getDeclaredMethod("getPkeyContext");

            // integer pointer to EVP_pkey
            getPkeyContext.setAccessible(true);
            int pkey = (Integer) getPkeyContext.invoke(opensslkey);
            getPkeyContext.setAccessible(false);

            // 112 with TLS 1.2 (172 back with 4.3), 36 with TLS 1.0
            byte[] signed_bytes = NativeUtils.rsasign(data, pkey);
            return Base64.encodeToString(signed_bytes, Base64.NO_WRAP);

        } catch (NoSuchMethodException | InvalidKeyException | InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
            Log.e(TAG, "processSignJellyBeans: ", e);
            return null;
        }
    }


    public String getServerCert() {
        return serverCert;
    }

    public void setServerCert(String serverCert) {
        this.serverCert = serverCert;
    }


}
