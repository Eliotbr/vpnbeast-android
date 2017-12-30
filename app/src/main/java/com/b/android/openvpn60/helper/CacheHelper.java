package com.b.android.openvpn60.helper;

import java.util.UUID;

/**
 * Created by b on 5/15/17.
 */

public class CacheHelper {
    public static final int PCKS12ORCERTPASSWORD = 2;
    public static final int AUTH_PASSWORD = 3;
    private static CacheHelper cacheHelper;
    private final UUID uuid;
    private String keyOrPKCSPassword;
    private String authPassword;


    private CacheHelper(UUID uuid) {
        this.uuid = uuid;
    }

    public static CacheHelper getInstance(UUID uuid) {
        if (cacheHelper == null || !cacheHelper.uuid.equals(uuid)) {
            cacheHelper = new CacheHelper(uuid);
        }
        return cacheHelper;
    }

    public static String getPKCS12orCertificatePassword(UUID uuid, boolean resetPw) {
        String pwcopy = getInstance(uuid).keyOrPKCSPassword;
        if (resetPw)
            getInstance(uuid).keyOrPKCSPassword = null;
        return pwcopy;
    }

    public static String getAuthPassword(UUID uuid, boolean resetPW) {
        String pwcopy = getInstance(uuid).authPassword;
        if (resetPW)
            getInstance(uuid).authPassword = null;
        return pwcopy;
    }

    public static void setCachedPassword(String uuid, int type, String password) {
        CacheHelper instance = getInstance(UUID.fromString(uuid));
        switch (type) {
            case PCKS12ORCERTPASSWORD:
                instance.keyOrPKCSPassword = password;
                break;
            case AUTH_PASSWORD:
                instance.authPassword = password;
                break;
        }
    }
}