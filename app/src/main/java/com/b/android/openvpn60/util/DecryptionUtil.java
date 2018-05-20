package com.b.android.openvpn60.util;

/**
 * Created by b on 3/1/2018.
 */

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DecryptionUtil {

    private DecryptionUtil() {

    }

    // AES256 Decryption on the client side
    public static String decrypt(String encryptedText) {
        try {
            String password="S3lyaWVGdWNraW5nSXJ2aW5nV2FzSGVyZQ=="; //NOSONAR
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            //strip off the salt and iv
            ByteBuffer buffer = ByteBuffer.wrap(android.util.Base64.decode(encryptedText, android.util.Base64.DEFAULT));
            byte[] saltBytes = new byte[20];
            buffer.get(saltBytes, 0, saltBytes.length);
            byte[] ivBytes1 = new byte[cipher.getBlockSize()];
            buffer.get(ivBytes1, 0, ivBytes1.length);
            byte[] encryptedTextBytes = new byte[buffer.capacity() - saltBytes.length - ivBytes1.length];
            buffer.get(encryptedTextBytes);
            // Deriving the key
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, 65556, 256);
            SecretKey secretKey = factory.generateSecret(spec);
            SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), "AES");
            cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(ivBytes1));
            return new String(cipher.doFinal(encryptedTextBytes), "UTF-8");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | InvalidAlgorithmParameterException
                | IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException | NoSuchPaddingException exception) {
            Logger.getLogger(DecryptionUtil.class.getName()).log(Level.SEVERE, "", exception);
        }
        return null;
    }

    public static ArrayList<String> getDecryptedUserList(String userPass, String createDate, String lastDate,
                                                         String uuid, String status) {
        ArrayList<String> decryptedList = new ArrayList<>();
        decryptedList.add(decrypt(userPass));
        decryptedList.add(decrypt(createDate));
        if (lastDate != null)
            decryptedList.add(decrypt(lastDate));
        decryptedList.add(decrypt(uuid));
        decryptedList.add(decrypt(status));
        return decryptedList;
    }

    public static ArrayList<String> getDecryptedServerList(String serverName, String serverIp, String serverPort, String serverUuid,
                                                           String serverCert, String serverStatus) {
        ArrayList<String> decryptedList = new ArrayList<>();
        decryptedList.add(decrypt(serverName));
        decryptedList.add(decrypt(serverIp));
        decryptedList.add(decrypt(serverPort));
        decryptedList.add(decrypt(serverUuid));
        decryptedList.add(decrypt(serverCert));
        decryptedList.add(decrypt(serverStatus));
        return decryptedList;
    }
}
