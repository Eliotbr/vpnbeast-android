package com.b.android.openvpn60.model;

import java.io.Serializable;
import java.security.PrivateKey;
import java.util.HashSet;
import java.util.UUID;

/**
 * Created by b on 12/30/2017.
 */

public class VpnProfileTest implements Serializable {

    // Note that this class cannot be moved to core where it belongs since
    // the profile loading depends on it being here
    // The Serializable documentation mentions that class name change are possible
    // but the how is unclear
    //
    transient public static final long MAX_EMBED_FILE_SIZE = 2048 * 1024; // 2048kB
    // Don't change this, not all parts of the program use this constant
    public static final String EXTRA_PROFILEUUID = "de.blinkt.openvpn.profileUUID";
    public static final String INLINE_TAG = "[[INLINE]]";
    public static final String DISPLAYNAME_TAG = "[[NAME]]";

    private static final String TAG = "com.b.android.openvpn." + VpnProfileTest.class.toString();
    private static final long serialVersionUID = 7085688938959334563L;
    public static final int MAXLOGLEVEL = 4;
    public static final int CURRENT_PROFILE_VERSION = 6;
    public static final int DEFAULT_MSSFIX_SIZE = 1280;
    public static String DEFAULT_DNS1;
    public static String DEFAULT_DNS2;

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
    // variable named wrong and should haven beeen transient
    // but needs to keep wrong name to guarante loading of old
    // profiles
    public transient boolean profileDeleted;
    public int mAuthenticationType;
    public String name;
    public String aliasName;
    public String clientCertFilename;
    public String tlsAuthDirection;
    public String tlsAuthFilename;
    public String clientKeyFilename;
    public String caFilename;
    public boolean useLzo = true;
    public String pkcs12Filename;
    public String pkcs12Password;
    public boolean useTLSAuth;

    public String DNS1 = DEFAULT_DNS1;
    public String DNS2 = DEFAULT_DNS2;
    public String ipv4Address;
    public String mIPv6Address;
    public boolean overrideDNS;
    //public String mSearchDomain = "blinkt.de";
    public boolean useDefaultRoute;
    public boolean usePull;
    public String customRoutes;
    public boolean checkRemoteCN;
    public boolean expectTLSCert;
    public String remoteCN;
    public String password;
    public String userName;
    public boolean routenopull;
    public boolean useRandomHostname;
    public boolean useFloat;
    public boolean useCustomConfig;
    public String customConfigOptions;
    public String verb;  //ignored
    public String cipher;
    public boolean nobind;
    public boolean useDefaultRoutev6;
    public String customRoutesv6;
    public String keyPassword;
    public boolean persistTun;
    public String connectRetryMax;
    public String connectRetry;
    public String connectRetryMaxTime;
    public boolean userEditable;
    public String auth;
    public int x509AuthType;
    public String x509UsernameField;

    private transient PrivateKey privateKey;
    // Public attributes, since I got mad with getter/setter
    // set members to default values
    private UUID uuid;
    public boolean allowLocalLAN;
    private int profileVersion;
    public String excludedRoutes;
    public String excludedRoutesv6;
    public int mssFix; // -1 is default,
    public ConnectionTest[] connections;
    public boolean remoteRandom;
    public HashSet<String> allowedAppsVpn;
    public boolean allowedAppsVpnAreDisallowed;
    public String crlFilename;
    public String profileCreator;
    public long ping;

    public boolean pushPeerInfo;
    public static final boolean isOpenVPN22 = false;

    public int version;

    // timestamp when the profile was last used
    public long lastUsed;

    /* Options no longer used in new profiles */
    public String serverName;
    public String serverPort;
    public boolean useUdp;
    public String certString;


    public VpnProfileTest() {
        uuid = UUID.randomUUID();
        this.name = name;
        profileVersion = CURRENT_PROFILE_VERSION;
        connections = new ConnectionTest[1];
        connections[0] = new ConnectionTest();
        generateProfile();
    }

    private void generateProfile() {
        profileDeleted = false;
        mAuthenticationType = TYPE_KEYSTORE;
        name = "converted profile";
        clientCertFilename = null;
        tlsAuthDirection = "";
        tlsAuthFilename = null;
        clientKeyFilename = null;
        caFilename = "[[INLINE]]" + certString;
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
        password = "";
        userName = "";
        routenopull = false;
        useRandomHostname = false;
        useFloat = false;
        useCustomConfig = false;customConfigOptions = "";
        verb = "3";
        cipher = "AES-256-CBC";
        nobind = true;
        useDefaultRoutev6 = false;
        customRoutesv6 = "";
        keyPassword = "";
        persistTun = true;
        connectRetryMax = "-1";
        connectRetry = "2";
        connectRetryMaxTime = "300";
        userEditable = true;
        auth = "SHA256";
        x509AuthType = 3;
        x509UsernameField = null;
        privateKey = null;
        uuid = UUID.randomUUID();
        allowLocalLAN = true;
        profileVersion = 6;
        excludedRoutes = null;
        excludedRoutesv6 = null;
        mssFix = 0;
        remoteRandom = false;
        allowedAppsVpn = new HashSet<>();
        allowedAppsVpnAreDisallowed = true;
        crlFilename = null;
        profileCreator = null;
        ping = 0;
        pushPeerInfo = false;
        version = 1;
        lastUsed = System.currentTimeMillis();
        serverName = "unknown";
        serverPort = "1194";
        useUdp = true;
    }

    public UUID getUUID() {
        return uuid;
    }

}




















