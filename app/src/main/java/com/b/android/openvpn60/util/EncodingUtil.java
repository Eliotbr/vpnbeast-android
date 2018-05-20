package com.b.android.openvpn60.util;

import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by b on 2/10/2018.
 */

public class EncodingUtil {
    private static final Logger LOGGER = Logger.getLogger(EncodingUtil.class.toString());


    private EncodingUtil() {

    }

    // Encode with android.util.Base64
    public static String encodeString(String clearText) {
        try {
            byte[] bytes = clearText.getBytes("UTF-8");
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE, "Exception: ", e);
        }
        return null;
    }

    // Decode with android.util.Base64
    public static String decodeString(String encodedText) {
        try {
            return new String(Base64.decode(encodedText, Base64.DEFAULT));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception: ", e);
        }
        return null;
    }
}
