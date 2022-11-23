package util;

import org.junit.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

public class RSAUtilityTest {
    @Test
    public void generateRSAKeyPair() {
        KeyPair keyPair = RSAUtility.generateRSAKeyPair(512);
        assert null != keyPair;
        assert !RSAUtility.getPrivateKeyString().isEmpty();
        assert !RSAUtility.getPublicKeyString().isEmpty();

        System.out.println(RSAUtility.getPublicKeyString());
        System.out.println(RSAUtility.getPrivateKeyString());
    }

    @Test
    public void testEncodeDecode() throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidKeySpecException {
        String originalString = "test";
        KeyPair keyPair = RSAUtility.generateRSAKeyPair(512);
        assert null != keyPair;

        String encodedString = RSAUtility.getEncryptedByPublicKey(originalString, keyPair.getPublic());
        String decodedString = RSAUtility.getDecryptedByPrivateKey(encodedString, keyPair.getPrivate());

        assert decodedString.equals(originalString);
    }

    @Test
    public void generateRSAKeyPairToFile() throws IOException {
        int keySize = 512;
        String fileNamePrefix = "RSA_" + keySize + "_";
        String publicPostfix = ".pub";
        String privatePostfix = ".pri";
        String fileName = null;
        for (int i = 0; i < 20; ++i) {
            KeyPair keyPair = RSAUtility.generateRSAKeyPair(512);
            assert null != keyPair;
            fileName = fileNamePrefix + TimeHelper.getEpoch() + publicPostfix;
            FileOutputStream keyfos = new FileOutputStream(fileName);
            keyfos.write(RSAUtility.getPublicKeyString().getBytes());
            keyfos.close();

            fileName = fileNamePrefix + TimeHelper.getEpoch() + privatePostfix;
            keyfos = new FileOutputStream(fileName);
            keyfos.write(RSAUtility.getPrivateKeyString().getBytes());
            keyfos.close();

            ThreadHelper.safeSleepSecond(1);
        }
    }

    @Test
    public void testDecodeEncode() throws NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, InvalidKeySpecException, NoSuchPaddingException {
        String pubKey = RSAUtility.getPublicKeyString();
        String priKey = RSAUtility.getPrivateKeyString();
        System.out.println("Public Key:" + pubKey);
        System.out.println("Private Key:" + priKey);
        String originalString = "test string";

        System.out.printf("\n[Test] Begin to test encoding by public key\n");
        String cipherText = RSAUtility.getEncryptedByPublicKey(originalString, pubKey);
        System.out.println("CHIPHER:" + cipherText);
        String decryptedText = RSAUtility.getDecryptedByPrivateKey(cipherText, priKey);
        System.out.println("DECRYPTED STRING:" + decryptedText);
        assert decryptedText.equals(originalString);

        System.out.printf("\n[Test] Begin to test encoding by private key\n");
        cipherText = RSAUtility.getEncryptedByPrivateKey(originalString, priKey);
        System.out.println("CHIPHER:" + cipherText);
        decryptedText = RSAUtility.getDecryptedByPublicKey(cipherText, pubKey);
        System.out.println("DECRYPTED STRING:" + decryptedText);
        assert decryptedText.equals(originalString);
    }

    @Test
    public void testDecodeEncodeFromFile() throws NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, InvalidKeySpecException, NoSuchPaddingException {
        String publicFileName = "RSA512a.pub";
        String privateFileName = "RSA512a.pri";
        String pubKey = FileHelper.loadContentFromFile(publicFileName).trim();
        String priKey = FileHelper.loadContentFromFile(privateFileName).trim();
        System.out.println("Public Key:" + pubKey);
        System.out.println("Private Key:" + priKey);
        String originalString = "hashLockingKeyword";

        System.out.printf("\n[Test] Begin to test encoding by public key\n");
        String cipherText = RSAUtility.getEncryptedByPublicKey(originalString, pubKey);
        System.out.println("CHIPHER:" + cipherText);
        String decryptedText = RSAUtility.getDecryptedByPrivateKey(cipherText, priKey);
        System.out.println("DECRYPTED STRING:" + decryptedText);
        assert decryptedText.equals(originalString);

        System.out.printf("\n[Test] Begin to test encoding by private key\n");
        cipherText = RSAUtility.getEncryptedByPrivateKey(originalString, priKey);
        System.out.println("CHIPHER:" + cipherText);
        decryptedText = RSAUtility.getDecryptedByPublicKey(cipherText, pubKey);
        System.out.println("DECRYPTED STRING:" + decryptedText);
        assert decryptedText.equals(originalString);
    }
}