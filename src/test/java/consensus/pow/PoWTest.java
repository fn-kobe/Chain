package consensus.pow;

import com.scu.suhong.Hash.MD5Hash;
import com.scu.suhong.block.BlockException;
import junit.framework.TestCase;

import java.security.NoSuchAlgorithmException;

public class PoWTest extends TestCase {

    public void testFindBlockNounce() throws PoWException, NoSuchAlgorithmException {
        String previousHash = "abc";
        String transactionRootHash = "1e";
        int nounce = PoW.findBlockNounce(previousHash,transactionRootHash);
        if ( 0 == nounce){
            System.out.println("Failed to find the matched nounce!");
        } else {
            System.out.println("Nounce found: " + nounce);
            assert(MiningConfiguration.isHashMatched(MD5Hash.getValue(previousHash + transactionRootHash + nounce)));
        }
    }

    public void testFindGenesesBlockNounce() throws PoWException, NoSuchAlgorithmException, BlockException {
        MiningConfiguration.setDifficulty(6);
        int nounce = PoW.findGenesesBlockNounce();
        if ( 0 == nounce){
            System.out.println("Failed to find the matched nounce!");
        } else {
            System.out.println("Nounce found: " + nounce);
            assert(MiningConfiguration.isHashMatched(MD5Hash.getValue(String.valueOf(nounce))));
        }
    }
}