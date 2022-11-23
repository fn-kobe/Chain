package hashlocking;

import account.AccountManager;
import com.scu.suhong.transaction.Transaction;
import org.junit.Test;
import util.FileHelper;
import util.RSAUtility;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class HashLockingProcessorTest {

    @Test
    public void process() throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidKeySpecException {
        String from = "from";
        String to = "to";
        int value = 2;
        String keyWord = "hashLockingKeyword";
        AccountManager.getInstance().addValue(from, value);
        // This should be done when miner process the setting transaction,
        // While we do not pass through miner, then we have to add it manually
        // As we will recall the value in hash locking processor
        AccountManager.getInstance().addValue(to, value);

        String  publicKey = RSAUtility.getPublicKeyString();
        String  privateKey = RSAUtility.getPrivateKeyString();
        System.out.printf("Public key %s.\nPrivate key %s\n", publicKey, privateKey);
        String publicKeyFileName = "publicKeyFile";
        FileHelper.deleteFile(publicKeyFileName);
        FileHelper.createFile(publicKeyFileName, publicKey);


        HashLockingProcessor processor = HashLockingProcessor.getInstance();
        assert null != processor;

        Transaction t = new Transaction();
        t.setFrom(from);
        t.setToAndValue(to, 0);// we only indicate to transfer the value
        t.setData(HashLockingProcessor.getHashLockingSettingFlag() + publicKeyFileName + "_" + keyWord);
        t.setId();
        processor.process(t);

        t = new Transaction();
        t.setFrom(from);
        t.setToAndValue(to, value);
        t.setData(HashLockingProcessor.getHashLockingDataFlag() + RSAUtility.getEncryptedByPrivateKey(keyWord, privateKey));
        t.setId();

        assert processor.process(t);
        assert value == AccountManager.getInstance().getBalance(to);
    }
}