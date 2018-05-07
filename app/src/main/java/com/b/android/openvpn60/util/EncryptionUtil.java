package com.b.android.openvpn60.util;

import java.io.UnsupportedEncodingException;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.ArrayList;
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


public class EncryptionUtil {

    // AES-256 Encryption on client side
    public static String encrypt(String word) {
        try {
            byte[] ivBytes;
            String password="S3lyaWVGdWNraW5nSXJ2aW5nV2FzSGVyZQ==";
            /*you can give whatever you want for password. This is for testing purpose*/
            SecureRandom random = new SecureRandom();
            byte bytes[] = new byte[20];
            random.nextBytes(bytes);
            byte[] saltBytes = bytes;
            // Derive the key
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(),saltBytes,65556,256);
            SecretKey secretKey = factory.generateSecret(spec);
            SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), "AES");
            //encrypting the word
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secret);
            AlgorithmParameters params = cipher.getParameters();
            ivBytes = params.getParameterSpec(IvParameterSpec.class).getIV();
            byte[] encryptedTextBytes = cipher.doFinal(word.getBytes("UTF-8"));
            //prepend salt and vi
            byte[] buffer = new byte[saltBytes.length + ivBytes.length + encryptedTextBytes.length];
            System.arraycopy(saltBytes, 0, buffer, 0, saltBytes.length);
            System.arraycopy(ivBytes, 0, buffer, saltBytes.length, ivBytes.length);
            System.arraycopy(encryptedTextBytes, 0, buffer, saltBytes.length + ivBytes.length, encryptedTextBytes.length);
            return android.util.Base64.encodeToString(buffer, android.util.Base64.DEFAULT);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException |
                InvalidParameterSpecException | UnsupportedEncodingException | IllegalBlockSizeException | BadPaddingException exception) {
            Logger.getLogger(EncryptionUtil.class.getName()).log(Level.SEVERE, "", exception);
        }
        return null;
    }


    public static ArrayList<String> getEncryptedUserList(String userPass, String createDate, String lastDate,
                                                         String uuid, String status) {
        ArrayList<String> encryptedList = new ArrayList<>();
        encryptedList.add(encrypt(userPass));
        encryptedList.add(encrypt(createDate));
        if (lastDate != null)
            encryptedList.add(encrypt(lastDate));
        else
            encryptedList.add(null);
        encryptedList.add(encrypt(uuid));
        encryptedList.add(encrypt(status));
        return encryptedList;
    }


    public static ArrayList<String> getEncryptedServerList(String serverIp, String serverPort, String serverStatus,
                                                           String serverUuid, String serverCert) {
        ArrayList<String> encryptedList = new ArrayList<>();
        encryptedList.add(encrypt(serverIp));
        encryptedList.add(encrypt(serverPort));
        encryptedList.add(encrypt(serverStatus));
        encryptedList.add(encrypt(serverUuid));
        encryptedList.add(encrypt(serverCert));
        return encryptedList;
    }


    public static ArrayList<String> getEncryptedMemberList(String memberStatus, String email, String createDate, String firstName,
                                                           String lastName, String startDate, String endDate) {
        ArrayList<String> encryptedList = new ArrayList<>();
        encryptedList.add(encrypt(memberStatus));
        encryptedList.add(encrypt(email));
        encryptedList.add(encrypt(createDate));
        encryptedList.add(encrypt(firstName));
        encryptedList.add(encrypt(lastName));
        encryptedList.add(encrypt(startDate));
        encryptedList.add(encrypt(endDate));
        return encryptedList;
    }
}
