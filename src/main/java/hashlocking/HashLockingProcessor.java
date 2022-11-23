package hashlocking;

import account.AccountManager;
import com.scu.suhong.block.Block;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.transaction.Transaction;
import util.FileHelper;
import util.RSAUtility;
import util.TimeHelper;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

// notice, currently, we only support hashlocking in seaquence - one complete and then another
// As we use global single variety to record one exchange.
// If want to run in parallel, please use a list or other container to do it.
public class HashLockingProcessor {
    static final String hashLockingSettingFlag = "hashLockingSetting_";
    static final String hashLockingDataFlag = "hashLockingData_";
    static HashLockingProcessor instance = null;
    String promisedSender = "";
    String promisedReceiver = "";
    int promisedValue = 0;
    String publicKeyFileName = "";
    String keyword = "";
    String hashlockingId = "";
    String publicKeyContent = "";
    Long startEpoc = 0l;

    // Indicate whether the processor is validly setting or not.
    // This is valid only after correctly setting and process can process hash locking data transaction
    boolean isValid = false;

    private HashLockingProcessor() {
    }

    public static HashLockingProcessor getInstance() {
        if (null != instance) return instance;

        instance = new HashLockingProcessor();
        return instance;
    }

    static public String getHashLockingDataFlag() {
        return hashLockingDataFlag;
    }

    public static String getHashLockingSettingFlag() {
        return hashLockingSettingFlag;
    }

    public void tryAddNewBlock(Block block) {
        for (AbstractTransaction transaction : block.getTransactions()) {
            if (!(transaction instanceof Transaction)) continue;

            process((Transaction) transaction);
        }
    }

    public boolean process(Transaction t) {
        String d = t.getData();
        if (d.contains(hashLockingSettingFlag)){
            return processHashLockingSetting(t);
        }

        if (d.contains(hashLockingDataFlag)){
            return processHashLockingData(t);
        }

        System.out.printf("[HashLockingProcessor][INFO] Skip to process transaction %d as its data '%s' has no hash locking flag'%s'\n"
                , t.getId(), t.getData(), hashLockingDataFlag);
        return false;
    }

    public boolean processHashLockingSetting(Transaction t) {
        String d = t.getData();
        int position = d.indexOf("_");
        if (-1 == position) {
            System.out.printf("[HashLockingProcessor][ERROR] No hash locking setting in transaction %d with data %s"
                    , t.getId(), t.getData());
            return false;
        }

        String parameters = d.substring(position + 1);
        String[] parameterArray = parameters.split("_");
        if (parameterArray.length < 3){
            System.out.printf("[HashLockingProcessor][ERROR] Hash-locking setting error in transaction %d with setting data %d\n",
                    t.getId(), t.getData());
            return false;
        }

        hashlockingId = parameterArray[0];
        String publicKeyFileName = parameterArray[1];
        String keyword = parameterArray[2];
        if (null == keyword || keyword.isEmpty() || null == publicKeyFileName || publicKeyFileName.isEmpty()){
            System.out.printf("[HashLockingProcessor][ERROR] Hashlocking setting error in transaction %d with setting data %d\n",
                    t.getId(), t.getData());
            return false;
        }
        this.publicKeyFileName = publicKeyFileName;
        this.keyword = keyword;

        // recall value and give it to receiver when hash matches
        AccountManager.getInstance().subValue(t.getTo(), promisedValue);
        promisedSender = t.getFrom();
        promisedReceiver = t.getTo();
        promisedValue = t.getValue();

        startEpoc = TimeHelper.getEpoch();
        System.out.printf("[HashLockingProcessor][INFO] Hashlocking begin at epoc %d\n", startEpoc);
        isValid = true;

        return true;
    }

    public boolean processHashLockingData(Transaction t) {
        String d = t.getData();
        int position = d.indexOf("_");
        if (-1 == position) {
            System.out.printf("[HashLockingProcessor][ERROR] No hash locking data in transaction %d with data %s"
                    , t.getId(), t.getData());
            return false;
        }

        if (!isValid){
            System.out.printf("[HashLockingProcessor][ERROR] Skip to process hash locking data in transaction %d as not in valid state"
                    , t.getId());
            return false;
        }

        String parameters = d.substring(position + 1);
        String[] parameterArray = parameters.split("_");
        if (parameterArray.length < 3){
            System.out.printf("[HashLockingProcessor][ERROR] Hash-locking setting error in transaction %d with setting data %d\n",
                    t.getId(), t.getData());
            return false;
        }

        if (!hashlockingId.equals(parameterArray[0])){
            System.out.printf("[HashLockingProcessor][ERROR] Hash-locking identifier %d not the same as in setting %d\n",
                    parameterArray[0], hashlockingId);
            return false;
        }

        //  keywordToIdentifier is used to identify current hash lock in block or used by external tool to find
        // Not used in blockchain currently, if want to, please use it when required
        String keywordToIdentifier = parameterArray[1];

        String encodedKeyword = parameterArray[2];
        String decodedWord = getDecodedWord(encodedKeyword);
        System.out.printf("[HashLockingProcessor][DEBUG] expect keyword '%s', decodedWord '%s', encodedKeyword '%s' \n"
                , keyword, decodedWord, encodedKeyword);
        if (keyword.equals(decodedWord)){
            AccountManager.getInstance().addValue(promisedReceiver, promisedValue);
            Long endEpoc = TimeHelper.getEpoch();
            System.out.printf("[HashLockingProcessor][INFO] *** Hashlocking completes at %d. Total time is %d\n"
                    , endEpoc, endEpoc - startEpoc);
            return true;
        }

        return false;
    }

    String getDecodedWord(String encodedWord) {
        if (publicKeyContent.isEmpty()) {
            publicKeyContent = FileHelper.loadContentFromFile(publicKeyFileName).trim();
            System.out.printf("[HashLockingProcessor][DEBUG] Loaded public key is '%s'\n", publicKeyContent);
        }
        if (publicKeyContent.isEmpty()) return "";

        System.out.println("[HashLockingProcessor][DEBUG] Hash locking checking public key " + publicKeyContent);

        try {
            return RSAUtility.getDecryptedByPublicKey(encodedWord, publicKeyContent);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public void reset(List<Block> blockList){
        for (Block block : blockList) {
            tryAddNewBlock(block);
        }
    }
}
