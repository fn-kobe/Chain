package util;

import javax.crypto.*;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSAUtility {
    static int defaultKeySize = 512;
    static KeyPair keyPair = null;
    public static synchronized KeyPair generateRSAKeyPair(int keysize) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("Blowfish");
            keyGenerator.init(448);
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(512);
            return keyPair = keyPairGenerator.genKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getPublicKeyString(){
        if (null == keyPair) generateRSAKeyPair(defaultKeySize);

        return new String(Base64.getEncoder().encode(keyPair.getPublic().getEncoded()));
    }

    public static String getPrivateKeyString(){
        if (null == keyPair) generateRSAKeyPair(defaultKeySize);

        return new String(Base64.getEncoder().encode(keyPair.getPrivate().getEncoded()));
    }

    public static String getEncryptedByPublicKey(String data, PublicKey publicKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedbytes = cipher.doFinal(data.getBytes());
        return new String(Base64.getEncoder().encode(encryptedbytes));
    }

    public static String getDecryptedByPrivateKey(String data, PrivateKey privateKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] encryptedbytes = cipher.doFinal(Base64.getDecoder().decode(data.getBytes()));
        return new String(encryptedbytes);
    }

    public static String getEncryptedByPublicKey(String data, String Key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(Key.getBytes())));
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedbytes = cipher.doFinal(data.getBytes());
        return new String(Base64.getEncoder().encode(encryptedbytes));
    }

    public static String getEncryptedByPrivateKey(String data, String Key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(Key.getBytes())));
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        byte[] encryptedbytes = cipher.doFinal(data.getBytes());
        return new String(Base64.getEncoder().encode(encryptedbytes));
    }

    public static String getDecryptedByPrivateKey(String data, String key) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(key.getBytes())));
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] encryptedbytes = cipher.doFinal(Base64.getDecoder().decode(data.getBytes()));
        return new String(encryptedbytes);
    }

    public static String getDecryptedByPublicKey(String data, String key) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(key.getBytes())));
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        byte[] encryptedbytes = cipher.doFinal(Base64.getDecoder().decode(data.getBytes()));
        return new String(encryptedbytes);
    }
}
