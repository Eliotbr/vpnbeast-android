package com.b.android.openvpn60.core;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.b.android.openvpn60.helper.LogHelper;
import com.b.android.openvpn60.model.VpnProfile;
import com.b.android.openvpn60.util.PreferencesUtil;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Created by b on 5/15/17.
 */

public class ProfileManager {
    private static final String PREFS_NAME = "VPNList";
    private static final String LAST_CONNECTED_PROFILE = "lastConnectedProfile";
    private static final String TEMPORARY_PROFILE_FILENAME = "temporary-vpn-profile";
    private static ProfileManager instance;
    private static VpnProfile lastProfile = null;
    private HashMap<String, VpnProfile> profiles = new HashMap<>();
    private static VpnProfile tmpProfile = null;
    private static Context context;
    private static LogHelper logHelper;


    private static VpnProfile get(String key) {
        if (tmpProfile != null && tmpProfile.getUUIDString().equals(key))
            return tmpProfile;

        if (instance == null)
            return null;
        return instance.profiles.get(key);
    }

    private ProfileManager() {

    }

    private static void checkInstance(Context context) {
        if (instance == null) {
            instance = new ProfileManager();
            instance.loadVPNList(context);
        }
    }

    public synchronized static ProfileManager getInstance(Context context) {
        logHelper = LogHelper.getLogHelper(ProfileManager.class.toString());
        checkInstance(context);
        return instance;
    }

    public static void setAsDisconnected(Context c) {
        SharedPreferences prefs = PreferencesUtil.getDefaultSharedPreferences(c);
        SharedPreferences.Editor prefsedit = prefs.edit();
        prefsedit.putString(LAST_CONNECTED_PROFILE, null);
        prefsedit.apply();

    }

    /**
     * Sets the profile that is connected (to connect if the service restarts)
     */
    public static void setAsConnected(Context c, VpnProfile connectedProfile) {
        SharedPreferences prefs = PreferencesUtil.getDefaultSharedPreferences(c);
        SharedPreferences.Editor prefsedit = prefs.edit();
        prefsedit.putString(LAST_CONNECTED_PROFILE, connectedProfile.getUUIDString());
        prefsedit.apply();
        lastProfile = connectedProfile;
    }

    /**
     * Returns the profile that was last connected (to connect if the service restarts)
     */
    public static VpnProfile getLastConnectedProfile(Context c) {
        SharedPreferences prefs = PreferencesUtil.getDefaultSharedPreferences(c);

        String lastConnectedProfile = prefs.getString(LAST_CONNECTED_PROFILE, null);
        if (lastConnectedProfile != null)
            return get(c, lastConnectedProfile);
        else
            return null;
    }


    public Collection<VpnProfile> getProfiles() {
        return profiles.values();
    }

    public VpnProfile getProfileByName(String name) {
        for (VpnProfile vpnp : profiles.values()) {
            if (vpnp.getName().equals(name)) {
                return vpnp;
            }
        }
        return null;
    }

    public void saveProfileList(Context context) {
        SharedPreferences sharedprefs = PreferencesUtil.getSharedPreferencesMulti(PREFS_NAME, context);
        SharedPreferences.Editor editor = sharedprefs.edit();
        editor.putStringSet("vpnlist", profiles.keySet());

        // For reasing I do not understand at all
        // Android saves my prefs file only one time
        // if I remove the debug code below :(
        int counter = sharedprefs.getInt("counter", 0);
        editor.putInt("counter", counter + 1);
        editor.apply();

    }

    public void addProfile(VpnProfile profile) {
        profiles.put(profile.getUUID().toString(), profile);

    }

    public static void setTemporaryProfile(Context c, VpnProfile tmp) {
        ProfileManager.tmpProfile = tmp;
        saveProfile(c, tmp, true, true);
    }

    public static boolean isTempProfile() {
        return lastProfile != null && lastProfile == tmpProfile;
    }

    public void saveProfile(Context context, VpnProfile profile) {
        saveProfile(context, profile, true, false);
    }

    private static void saveProfile(Context context, VpnProfile profile, boolean updateVersion, boolean isTemporary) {
        if (updateVersion)
            profile.version += 1;
        ObjectOutputStream vpnFile;
        String filename = profile.getUUID().toString() + ".vp";
        if (isTemporary)
            filename = TEMPORARY_PROFILE_FILENAME + ".vp";
        try {
            vpnFile = new ObjectOutputStream(context.openFileOutput(filename, Activity.MODE_PRIVATE));
            vpnFile.writeObject(profile);
            vpnFile.flush();
            vpnFile.close();
        } catch (IOException e) {
            logHelper.logException(e);
            throw new RuntimeException(e);
        }
    }


    private void loadVPNList(Context context) {
        profiles = new HashMap<>();
        SharedPreferences listpref = PreferencesUtil.getSharedPreferencesMulti(PREFS_NAME, context);
        Set<String> vlist = listpref.getStringSet("vpnlist", null);
        if (vlist == null) {
            vlist = new HashSet<>();
        }
        // Always try to load the temporary profile
        vlist.add(TEMPORARY_PROFILE_FILENAME);

        for (String vpnentry : vlist) {
            try {
                ObjectInputStream vpnfile = new ObjectInputStream(context.openFileInput(vpnentry + ".vp"));
                VpnProfile vp = ((VpnProfile) vpnfile.readObject());

                // Sanity check
                if (vp == null || vp.name == null || vp.getUUID() == null)
                    continue;

                vp.upgradeProfile();
                if (vpnentry.equals(TEMPORARY_PROFILE_FILENAME)) {
                    tmpProfile = vp;
                } else {
                    profiles.put(vp.getUUID().toString(), vp);
                }

            } catch (IOException | ClassNotFoundException e) {
                if (!vpnentry.equals(TEMPORARY_PROFILE_FILENAME))
                    logHelper.logException(e);
            }
        }
    }


    public void removeProfile(Context context, VpnProfile profile) {
        String vpnentry = profile.getUUID().toString();
        profiles.remove(vpnentry);
        saveProfileList(context);
        context.deleteFile(vpnentry + ".vp");
        if (lastProfile == profile)
            lastProfile = null;

    }

    public static VpnProfile get(Context context, String profileUUID) {
        return get(context, profileUUID, 0, 10);
    }

    public static VpnProfile get(Context context, String profileUUID, int version, int tries) {
        checkInstance(context);
        VpnProfile profile = get(profileUUID);
        int tried = 0;
        while ((profile == null || profile.version < version) && (tried++ < tries)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
                logHelper.logException(ignored);
            }
            instance.loadVPNList(context);
            profile = get(profileUUID);
            int ver = profile == null ? -1 : profile.version;
        }

        if (tried > 5)

        {
            int ver = profile == null ? -1 : profile.version;
            VpnStatus.logError(String.format(Locale.US, "Used x %d tries to get current version (%d/%d) of the profile", tried, ver, version));
        }
        return profile;
    }

    public static VpnProfile getLastConnectedVpn() {
        return lastProfile;
    }

    public static VpnProfile getAlwaysOnVPN(Context context) {
        checkInstance(context);
        SharedPreferences prefs = PreferencesUtil.getDefaultSharedPreferences(context);

        String uuid = prefs.getString("alwaysOnVpn", null);
        return get(uuid);

    }

    public static void updateLRU(Context c, VpnProfile profile) {
        profile.lastUsed = System.currentTimeMillis();
        // LRU does not change the profile, no need for the service to refresh
        if (profile!= tmpProfile)
            saveProfile(c, profile, false, false);
    }
}
