package consensus.pow;

import com.scu.suhong.Hash.MD5Hash;
import com.scu.suhong.block.Block;
import com.scu.suhong.block.BlockException;
import util.FileLogger;

import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Random;

public class PoW {
    static org.apache.log4j.Logger logger = FileLogger.getLogger();
    //static Logger logger = LogManager.getLogger(PoW.class.getName());
    static boolean forceStop = false;

    static public int findGenesesBlockNounce() throws PoWException, BlockException {
        // there is no previous hash and no transaction in Geneses block
        Block block = Block.constructEmptyBlock();
        return findBlockNounce(block.getPreviousHash(), block.getTransactionHash());
    }

    static public int safeFindBlockNounce(String previousHash, String transactionRootHash) {
        try {
            return findBlockNounce(previousHash, transactionRootHash);
        } catch (PoWException e) {
            return 0;
        }
    }

    static public int findBlockNounce(String previousHash, String transactionHash) throws PoWException {

        Random random = new Random();
        String md5Hash = "";
        boolean nonZeroFound = false;
        Date enterDate = new Date();
        long enterDateMS = enterDate.getTime();
        int maxTryTime = MiningConfiguration.getMaxTryTime() * 1000; // to ms
        int tryNounce = -1;
        forceStop = false;
        try {
            //while (!forceStop &&(maxTryTime > (((new Date()).getTime()) - enterDateMS)))
//            int giveCPUCount = 0;
//            final int maxUseCPUCount = 5;
            while (!forceStop )
            {
                tryNounce = random.nextInt();
                md5Hash = MD5Hash.getValue(getHashRawData(previousHash, transactionHash, tryNounce));
                if (!MiningConfiguration.isHashMatched(md5Hash)) {
                    md5Hash ="";
                    //ThreadHelper.safeSleep(10);
                    continue;
                }
//                ++giveCPUCount;
//                if (giveCPUCount >= maxUseCPUCount) {
//                    giveCPUCount = 0;
//                    Thread.yield();
//                }
                Thread.yield();
                // found correct nounce
                logger.info("[PoW] Matched MD5 found: " + md5Hash);
                System.out.println("[PoW] Matched MD5 found: " + md5Hash);
                break;
            }
        } catch (NoSuchAlgorithmException e) {
            //logger.error("No MD5 calculation found");
            logger.info("[PoW] No MD5 calculation found\n" + e);
            throw new PoWException();
        }
        return tryNounce;
    }

    // TO DO call this method to interrupt from other node's block
    public static void setForceStop() {
        logger.info("[PoW] Stop mine work");
        PoW.forceStop = true;
    }

    private static String getHashRawData(String previousHash, String transactionRootHash, int tryNounce) {
        String result = "";
        if (null != previousHash) result += previousHash;
        if (null != transactionRootHash) result += transactionRootHash;
        result += tryNounce;
        return result;
    }

    // For find genesis block nounce
    static void find(){
        find(7, 6);
        find(8, 6);
    }
    static void find(int difficulty, int times){
        for (int i = 0; i < times; ++i) find(difficulty);
    }
    static void find(int difficulty){
        MiningConfiguration.setDifficulty(difficulty);
        int nounce = 0;
        try {
            nounce = PoW.findGenesesBlockNounce();
        } catch (PoWException e) {
            e.printStackTrace();
        } catch (BlockException e) {
            e.printStackTrace();
        }
        if ( 0 == nounce){
            System.out.println("Failed to find the matched nounce!");
        } else {
            System.out.println("Nounce found: " + nounce);
        }
    }
}
