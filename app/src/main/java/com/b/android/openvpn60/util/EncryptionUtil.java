package com.b.android.openvpn60.util;

import android.util.Base64;

import com.b.android.openvpn60.constant.AppConstants;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by b on 2/8/2018.
 */

public class EncryptionUtil {

    private static final Logger LOGGER = Logger.getLogger(EncryptionUtil.class.getName());
    private static String SALT;
    private static int iterations = 65536;
    private static int keySize = 256;
    private static byte[] ivBytes;
    private static SecretKey secretKey;


    static {
        try {
            //SALT = getSalt();
            SALT = AppConstants.SALT.toString();
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Exception: ", exception);
        }
    }


    // AES-256 Encryption
    public static String startEncryption(String clearText) {
        try {
            byte[] saltBytes = SALT.getBytes();
            char[] charSet = clearText.toCharArray();
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            PBEKeySpec spec = new PBEKeySpec(charSet, saltBytes, iterations, keySize);
            secretKey = skf.generateSecret(spec);
            SecretKeySpec secretSpec = new SecretKeySpec(secretKey.getEncoded(), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretSpec);
            AlgorithmParameters params = cipher.getParameters();
            ivBytes = params.getParameterSpec(IvParameterSpec.class).getIV();
            byte[] encryptedTextBytes = cipher.doFinal(String.valueOf(charSet).getBytes("UTF-8"));
            LOGGER.log(Level.INFO, android.util.Base64.encodeToString(encryptedTextBytes, 16));
            return android.util.Base64.encodeToString(encryptedTextBytes, 16);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException
                | InvalidKeyException | InvalidParameterSpecException | IllegalBlockSizeException
                | BadPaddingException | UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE, "Exception: ", e);
        }
        return null;
    }

    // AES-256 Decryption
    public static String startDecryption(String encoded) {
        //cipher.replaceAll("_", "/").replaceAll("-", "\\+");
        try {
            byte[] ivAndCipherText = Base64.decode(encoded, Base64.NO_WRAP);
            byte[] iv = Arrays.copyOfRange(ivAndCipherText, 0, 16);
            byte[] cipherText = Arrays.copyOfRange(ivAndCipherText, 16, ivAndCipherText.length);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(SALT.getBytes("utf-8"), "AES"), new IvParameterSpec(iv));
            AlgorithmParameters params = cipher.getParameters();
            return new String(cipher.doFinal(cipherText), "utf-8");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception: ", e);
            return null;
        }
    }

    // Generate random salt
    public static String getSalt() throws Exception {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte[] salt = new byte[20];
        sr.nextBytes(salt);
        return new String(salt);
    }

}
