package util;

import junit.framework.TestCase;

import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class EncryptionTest extends TestCase {

    public void testEncryptText() {
    }

    public void testDecryptText() {
    }

    public void testMakeKey() throws NoSuchAlgorithmException, NoSuchPaddingException, IOException {
        Encryption encryption = new Encryption();
        encryption.makeKey();
    }
}