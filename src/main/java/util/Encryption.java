package util;


import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class Encryption {
    private Cipher cipher;
    String algorithmName;

    public Encryption(String algorithmName) throws NoSuchPaddingException, NoSuchAlgorithmException {
        this.algorithmName = algorithmName;
        cipher = Cipher.getInstance(this.algorithmName);
    }

    //default id RSA
    public Encryption() throws NoSuchPaddingException, NoSuchAlgorithmException {
        algorithmName = "RSA";
        cipher = Cipher.getInstance(algorithmName);
    }

    public byte[] encryptText(byte[] msg, PrivateKey key)
            throws NoSuchAlgorithmException, NoSuchPaddingException,
            UnsupportedEncodingException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeyException {
        this.cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(msg);
    }

    public byte[] decryptText(byte[] msg, PublicKey key)
            throws InvalidKeyException, UnsupportedEncodingException,
            IllegalBlockSizeException, BadPaddingException {
        this.cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(msg);
    }

    // https://docs.oracle.com/javase/8/docs/api/java/security/spec/PKCS8EncodedKeySpec.html
    public PrivateKey getPrivate(String filename) throws Exception {
        byte[] keyBytes = Files.readAllBytes(new File(filename).toPath());
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance(algorithmName);
        return kf.generatePrivate(spec);
    }

    // https://docs.oracle.com/javase/8/docs/api/java/security/spec/X509EncodedKeySpec.html
    public PublicKey getPublic(String filename) throws Exception {
        byte[] keyBytes = Files.readAllBytes(new File(filename).toPath());
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance(algorithmName);
        return kf.generatePublic(spec);
    }

    public KeyPair makeKey() throws NoSuchAlgorithmException, IOException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(512);
        return keyGen.genKeyPair();
    }
}
