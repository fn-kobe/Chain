package util;

import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;
import java.io.*;
import java.util.Base64;

public class DESHelper {
    // key needs to be at least 8 characters for DES
    public static void encrypt(String key, InputStream is, OutputStream os) throws Throwable {
        encryptOrDecrypt(key, Cipher.ENCRYPT_MODE, is, os);
    }

    public static String encryptBase64(String key, String input) throws Throwable {
        ByteArrayInputStream is = new ByteArrayInputStream(input.getBytes());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        encryptOrDecrypt(key, Cipher.ENCRYPT_MODE, is, os);
       return new String(Base64.getEncoder().encode(os.toByteArray()));
    }

    // key needs to be at least 8 characters for DES
    public static void decrypt(String key, InputStream is, OutputStream os) throws Throwable {
        encryptOrDecrypt(key, Cipher.DECRYPT_MODE, is, os);
    }

    // key needs to be at least 8 characters for DES
    public static String decryptBase64(String key, String encryptedText) throws Throwable {
        ByteArrayInputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(encryptedText.getBytes()));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        encryptOrDecrypt(key, Cipher.DECRYPT_MODE, is, os);
        return new String(os.toByteArray());
    }

    public static void encryptOrDecrypt(String key, int mode, InputStream is, OutputStream os) throws Throwable {

        DESKeySpec dks = new DESKeySpec(key.getBytes());
        SecretKeyFactory skf = SecretKeyFactory.getInstance("DES");
        SecretKey desKey = skf.generateSecret(dks);
        Cipher cipher = Cipher.getInstance("DES"); // DES/ECB/PKCS5Padding for SunJCE

        if (mode == Cipher.ENCRYPT_MODE) {
            cipher.init(Cipher.ENCRYPT_MODE, desKey);
            CipherInputStream cis = new CipherInputStream(is, cipher);
            doCopy(cis, os);
        } else if (mode == Cipher.DECRYPT_MODE) {
            cipher.init(Cipher.DECRYPT_MODE, desKey);
            CipherOutputStream cos = new CipherOutputStream(os, cipher);
            doCopy(is, cos);
        }
    }

    public static void doCopy(InputStream is, OutputStream os) throws IOException {
        byte[] bytes = new byte[64];
        int numBytes;
        while ((numBytes = is.read(bytes)) != -1) {
            os.write(bytes, 0, numBytes);
        }
        os.flush();
        os.close();
        is.close();
    }
}
