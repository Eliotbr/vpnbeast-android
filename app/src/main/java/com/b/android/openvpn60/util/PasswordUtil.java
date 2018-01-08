package com.b.android.openvpn60.util;

import java.util.UUID;

/**
 * Created by b on 5/15/17.
 */

public class PasswordUtil {
    public static final int PCKS12ORCERTPASSWORD = 2;
    public static final int AUTHPASSWORD = 3;
    private static PasswordUtil mInstance;
    final private UUID mUuid;
    private String mKeyOrPkcs12Password;
    private String mAuthPassword;

    private PasswordUtil(UUID uuid) {
        mUuid = uuid;
    }

    public static PasswordUtil getInstance(UUID uuid) {
        if (mInstance == null || !mInstance.mUuid.equals(uuid)) {
            mInstance = new PasswordUtil(uuid);
        }
        return mInstance;
    }

    private void test() {

    }

    public static String getPKCS12orCertificatePassword(UUID uuid, boolean resetPw) {
        String pwcopy = getInstance(uuid).mKeyOrPkcs12Password;
        if (resetPw)
            getInstance(uuid).mKeyOrPkcs12Password = null;
        return pwcopy;
    }


    public static String getAuthPassword(UUID uuid, boolean resetPW) {
        String pwcopy = getInstance(uuid).mAuthPassword;
        if (resetPW)
            getInstance(uuid).mAuthPassword = null;
        return pwcopy;
    }

    public static void setCachedPassword(String uuid, int type, String password) {
        PasswordUtil instance = getInstance(UUID.fromString(uuid));
        switch (type) {
            case PCKS12ORCERTPASSWORD:
                instance.mKeyOrPkcs12Password = password;
                break;
            case AUTHPASSWORD:
                instance.mAuthPassword = password;
                break;
        }
    }


}
