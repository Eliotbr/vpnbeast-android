package com.b.android.openvpn60.model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import de.blinkt.openvpn.core.NativeUtils;

import com.b.android.openvpn60.R;
import com.b.android.openvpn60.core.OpenVPNService;
import com.b.android.openvpn60.helper.CacheHelper;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;
import java.util.Vector;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Created by b on 5/15/17.
 */

public class VpnProfile implements Serializable {

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
    // variable named wrong and should haven beeen transient
    // but needs to keep wrong name to guarante loading of old
    // profiles
    public transient boolean profileDeleted = false;
    public int mAuthenticationType = TYPE_KEYSTORE;
    public String name;
    public String aliasName;
    public String clientCertFilename;
    public String tlsAuthDirection = "";
    public String tlsAuthFilename;
    public String clientKeyFilename;
    public String caFilename;
    public boolean useLzo = true;
    public String pkcs12Filename;
    public String pkcs12Password;
    public boolean useTLSAuth = false;

    public String DNS1 = DEFAULT_DNS1;
    public String DNS2 = DEFAULT_DNS2;
    public String ipv4Address;
    public String mIPv6Address;
    public boolean overrideDNS = false;
    //public String mSearchDomain = "blinkt.de";
    public boolean useDefaultRoute = true;
    public boolean usePull = true;
    public String customRoutes;
    public boolean checkRemoteCN = true;
    public boolean expectTLSCert = false;
    public String remoteCN = "";
    public String password = "";
    public String userName = "";
    public boolean routenopull = false;
    public boolean useRandomHostname = false;
    public boolean useFloat = false;
    public boolean useCustomConfig = false;
    public String customConfigOptions = "";
    public String verb = "1";  //ignored
    public String cipher = "";
    public boolean nobind = false;
    public boolean useDefaultRoutev6 = true;
    public String customRoutesv6 = "";
    public String keyPassword = "";
    public boolean persistTun = false;
    public String connectRetryMax = "-1";
    public String connectRetry = "2";
    public String connectRetryMaxTime = "300";
    public boolean userEditable = true;
    public String auth = "";
    public int x509AuthType = X509_VERIFY_TLSREMOTE_RDN;
    public String x509UsernameField = null;

    private transient PrivateKey privateKey;
    // Public attributes, since I got mad with getter/setter
    // set members to default values
    private UUID uuid;
    public boolean allowLocalLAN;
    private int profileVersion;
    public String excludedRoutes;
    public String excludedRoutesv6;
    public int mssFix = 0; // -1 is default,
    public Connection[] connections = new Connection[0];
    public boolean remoteRandom = false;
    public HashSet<String> allowedAppsVpn = new HashSet<>();
    public boolean allowedAppsVpnAreDisallowed = true;
    public String crlFilename;
    public String profileCreator;
    public long ping;

    public boolean pushPeerInfo = false;
    public static final boolean isOpenVPN22 = false;

    public int version = 0;

    // timestamp when the profile was last used
    public long lastUsed;

    /* Options no longer used in new profiles */
    public String serverName = "openvpn.example.com";
    public String serverPort = "1194";
    public boolean useUdp = true;


    public VpnProfile(String name) {
        uuid = UUID.randomUUID();
        this.name = name;
        profileVersion = CURRENT_PROFILE_VERSION;

        connections = new Connection[1];
        connections[0] = new Connection();
        lastUsed = System.currentTimeMillis();
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

    public void clearDefaults() {
        serverName = "unknown";
        usePull = false;
        useLzo = false;
        useDefaultRoute = false;
        useDefaultRoutev6 = false;
        expectTLSCert = false;
        checkRemoteCN = false;
        persistTun = false;
        allowLocalLAN = true;
        pushPeerInfo = false;
        mssFix = 0;
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
        if (profileVersion < 2) {
            /* default to the behaviour the OS used */
            allowLocalLAN = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT;
        }

        if (profileVersion < 4) {
            moveOptionsToConnection();
            allowedAppsVpnAreDisallowed = true;
        }
        if (allowedAppsVpn == null)
            allowedAppsVpn = new HashSet<>();
        if (connections == null)
            connections = new Connection[0];

        if (profileVersion < 6) {
            if (TextUtils.isEmpty(profileCreator))
                userEditable = true;
        }
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


        //cfg += "verb " + mVerb + "\n";
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
            case VpnProfileTest.TYPE_USERPASS_CERTIFICATES:
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
        Vector<String> cidrRoutes = new Vector<>();
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
        Vector<String> cidrRoutes = new Vector<>();
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
        Intent intent = getStartServiceIntent(context);

        // TODO: Handle this?!
//        if (mAuthenticationType == VpnProfile.TYPE_KEYSTORE || mAuthenticationType == VpnProfile.TYPE_USERPASS_KEYSTORE) {
//            if (getKeyStoreCertificates(context) == null)
//                return null;
//        }

        return intent;
    }

    public void writeConfigFile(Context context) throws IOException {
        FileWriter cfg = new FileWriter(VPNLaunchHelper.getConfigFilePath(context));
        cfg.write(getConfigFile(context, false));
        cfg.flush();
        cfg.close();

    }

    public Intent getStartServiceIntent(Context context) {
        String prefix = context.getPackageName();

        Intent intent = new Intent(context, OpenVPNService.class);
        intent.putExtra(prefix + ".profileUUID", uuid.toString());
        intent.putExtra(prefix + ".profileVersion", version);
        return intent;
    }

    /*public String[] getKeyStoreCertificates(Context context) {
        return getKeyStoreCertificates(context, 5);
    }*/

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

    /*synchronized String[] getKeyStoreCertificates(Context context, int tries) {
        // Force application context- KeyChain methods will block long enough that by the time they
        // are finished and try to unbind, the original activity context might have been destroyed.
        context = context.getApplicationContext();

        try {
            PrivateKey privateKey = KeyChain.getPrivateKey(context, mAlias);
            mPrivateKey = privateKey;

            String keystoreChain = null;


            X509Certificate[] caChain = KeyChain.getCertificateChain(context, mAlias);
            if (caChain == null)
                throw new NoCertReturnedException("No certificate returned from Keystore");

            else {
                StringWriter ksStringWriter = new StringWriter();

                PemWriter pw = new PemWriter(ksStringWriter);
                for (int i = 1; i < caChain.length; i++) {
                    X509Certificate cert = caChain[i];
                    pw.writeObject(new PemObject("CERTIFICATE", cert.getEncoded()));
                }
                pw.close();
                keystoreChain = ksStringWriter.toString();
            }


            String caout = null;
            if (!TextUtils.isEmpty(mCaFilename)) {
                try {
                    Certificate[] cacerts = X509Utils.getCertificatesFromFile(mCaFilename);
                    StringWriter caoutWriter = new StringWriter();
                    PemWriter pw = new PemWriter(caoutWriter);

                    for (Certificate cert : cacerts)
                        pw.writeObject(new PemObject("CERTIFICATE", cert.getEncoded()));
                    pw.close();
                    caout = caoutWriter.toString();

                } catch (Exception e) {
                    Log.e(TAG, "getKeyStoreCertificates: ", e);
                }
            }


            StringWriter certout = new StringWriter();


            if (caChain.length >= 1) {
                X509Certificate usercert = caChain[0];

                PemWriter upw = new PemWriter(certout);
                upw.writeObject(new PemObject("CERTIFICATE", usercert.getEncoded()));
                upw.close();

            }
            String user = certout.toString();


            String ca, extra;
            if (caout == null) {
                ca = keystoreChain;
                extra = null;
            } else {
                ca = caout;
                extra = keystoreChain;
            }

            return new String[]{ca, extra, user};
        } catch (InterruptedException | IOException | KeyChainException | NoCertReturnedException | IllegalArgumentException
                | CertificateException e) {
            e.printStackTrace();
            Log.e(TAG, "getKeyStoreCertificates: ", e);

            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
                if (!mAlias.matches("^[a-zA-Z0-9]$")) {

                }
            }
            return null;

        } catch (AssertionError e) {
            if (tries == 0)
                return null;
            Log.e(TAG, "getKeyStoreCertificates: ", e);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e1) {
                Log.e(TAG, "getKeyStoreCertificates: ", e1);
            }
            return getKeyStoreCertificates(context, tries - 1);
        }

    }*/

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
        String cachedPw = CacheHelper.getPKCS12orCertificatePassword(uuid, true);
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
        String cachedPw = CacheHelper.getAuthPassword(uuid, true);
        if (cachedPw != null) {
            return cachedPw;
        } else {
            return password;
        }
    }

    @Override
    public String toString() {
        return "VpnProfile{" +
                "profileDeleted=" + profileDeleted +
                "\n mAuthenticationType=" + mAuthenticationType +
                "\n name='" + name + '\'' +
                "\n aliasName='" + aliasName + '\'' +
                "\n clientCertFilename='" + clientCertFilename + '\'' +
                "\n tlsAuthDirection='" + tlsAuthDirection + '\'' +
                "\n tlsAuthFilename='" + tlsAuthFilename + '\'' +
                "\n clientKeyFilename='" + clientKeyFilename + '\'' +
                "\n caFilename='" + caFilename + '\'' +
                "\n useLzo=" + useLzo +
                "\n pkcs12Filename='" + pkcs12Filename + '\'' +
                "\n pkcs12Password='" + pkcs12Password + '\'' +
                "\n useTLSAuth=" + useTLSAuth +
                "\n DNS1='" + DNS1 + '\'' +
                "\n DNS2='" + DNS2 + '\'' +
                "\n ipv4Address='" + ipv4Address + '\'' +
                "\n mIPv6Address='" + mIPv6Address + '\'' +
                "\n overrideDNS=" + overrideDNS +
                "\n useDefaultRoute=" + useDefaultRoute +
                "\n usePull=" + usePull +
                "\n customRoutes='" + customRoutes + '\'' +
                "\n checkRemoteCN=" + checkRemoteCN +
                "\n expectTLSCert=" + expectTLSCert +
                "\n remoteCN='" + remoteCN + '\'' +
                "\n password='" + password + '\'' +
                "\n userName='" + userName + '\'' +
                "\n routenopull=" + routenopull +
                "\n useRandomHostname=" + useRandomHostname +
                "\n useFloat=" + useFloat +
                "\n useCustomConfig=" + useCustomConfig +
                "\n customConfigOptions='" + customConfigOptions + '\'' +
                "\n verb='" + verb + '\'' +
                "\n cipher='" + cipher + '\'' +
                "\n nobind=" + nobind +
                "\n useDefaultRoutev6=" + useDefaultRoutev6 +
                "\n customRoutesv6='" + customRoutesv6 + '\'' +
                "\n keyPassword='" + keyPassword + '\'' +
                "\n persistTun=" + persistTun +
                "\n connectRetryMax='" + connectRetryMax + '\'' +
                "\n connectRetry='" + connectRetry + '\'' +
                "\n connectRetryMaxTime='" + connectRetryMaxTime + '\'' +
                "\n userEditable=" + userEditable +
                "\n auth='" + auth + '\'' +
                "\n x509AuthType=" + x509AuthType +
                "\n x509UsernameField='" + x509UsernameField + '\'' +
                "\n privateKey=" + privateKey +
                "\n uuid=" + uuid +
                "\n allowLocalLAN=" + allowLocalLAN +
                "\n profileVersion=" + profileVersion +
                "\n excludedRoutes='" + excludedRoutes + '\'' +
                "\n excludedRoutesv6='" + excludedRoutesv6 + '\'' +
                "\n mssFix=" + mssFix +
                "\n connections=" + Arrays.toString(connections) +
                "\n remoteRandom=" + remoteRandom +
                "\n allowedAppsVpn=" + allowedAppsVpn +
                "\n allowedAppsVpnAreDisallowed=" + allowedAppsVpnAreDisallowed +
                "\n crlFilename='" + crlFilename + '\'' +
                "\n profileCreator='" + profileCreator + '\'' +
                "\n ping=" + ping +
                "\n pushPeerInfo=" + pushPeerInfo +
                "\n version=" + version +
                "\n lastUsed=" + lastUsed +
                "\n serverName='" + serverName + '\'' +
                "\n serverPort='" + serverPort + '\'' +
                "\n useUdp=" + useUdp +
                '}';
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


}
