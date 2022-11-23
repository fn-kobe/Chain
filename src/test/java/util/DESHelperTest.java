package util;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class DESHelperTest extends TestCase {

    public void testEncryptBase64() throws Throwable {
        String dataUrl = "user:pass@10.10.1.1/big_data/data1";
        String buyerKey = "abc123456"; // at least 8 chars

        String enText = DESHelper.encryptBase64(buyerKey,dataUrl);
        String deText = DESHelper.decryptBase64(buyerKey,enText);

        assert dataUrl.equals(deText);
    }

    public void testEncrypt() throws Throwable {
        String dataUrl = "user:pass@10.10.1.1/big_data/data1";
        ByteArrayInputStream inputUrl = new ByteArrayInputStream(dataUrl.getBytes());
        ByteArrayOutputStream outputUrl = new ByteArrayOutputStream();
        String buyerKey = "abc123456"; // at least 8 chars

        DESHelper.encrypt(buyerKey,inputUrl, outputUrl);
        String output = new String(Base64.getEncoder().encode(outputUrl.toByteArray()));

        ByteArrayInputStream inputUrlForDe = new ByteArrayInputStream(Base64.getDecoder().decode(output.getBytes()));
        ByteArrayOutputStream outputUrlForDe = new ByteArrayOutputStream();
        DESHelper.decrypt(buyerKey, inputUrlForDe, outputUrlForDe );

        String deUrl = new String(outputUrlForDe.toByteArray());
        System.out.println(deUrl);
        assert deUrl.equals(dataUrl);
    }
}