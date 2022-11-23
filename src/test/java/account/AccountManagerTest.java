package account;

import com.scu.suhong.block.Block;
import com.scu.suhong.block.BlockBody;
import com.scu.suhong.block.BlockHeader;
import com.scu.suhong.transaction.Transaction;
import consensus.pow.MiningConfiguration;
import org.junit.Test;

public class AccountManagerTest {

    @Test
    public void testAddValue() {
        AccountManager manager = AccountManager.testGetInstance("reload");
        String user1 = "user1";
        String user2 = "user2";

        manager.addValue(user1, 3);
        manager.addValue(user1, 2);
        manager.addValue(user2, -1);

        assert 5 == manager.getBalance(user1);
        assert 0 == manager.getBalance(user2);
    }

    @Test
    public void testFullAndShortAddress() {
        String shortAddress  = "123456";
        String fullAddress  = MiningConfiguration.getBlockchainId() +AccountManager.getAddressConnectSymbol()+ "123456";
        assert shortAddress.equals(AccountManager.getShortAddress(shortAddress));
        assert shortAddress.equals(AccountManager.getShortAddress(fullAddress));
        assert fullAddress.equals(AccountManager.getFullAddress(shortAddress));
        assert fullAddress.equals(AccountManager.getFullAddress(fullAddress));
    }

    @Test
    public void testSubValue() {
        AccountManager manager = AccountManager.testGetInstance("reload");
        String user1 = "user1";
        String user2 = "user2";

        manager.addValue(user1, 3);
        manager.addValue(user1, 2);
        manager.subValue(user1, 1);

        assert 4 == manager.getBalance(user1);
        assert !manager.subValue(user2, 1);
        assert 0 == manager.getBalance(user2);
    }

    @Test
    public void testTransferValue() {
        AccountManager manager = AccountManager.testGetInstance("reload");
        String user1 = "user1";
        String user2 = "user2";

        manager.addValue(user1, 3);
        manager.addValue(user1, 2);

        manager.transferValue(user1, user2, 2);
        assert 3 == manager.getBalance(user1);
        assert 2 == manager.getBalance(user2);

        assert !manager.transferValue(user1, user2, 4);
        assert 3 == manager.getBalance(user1);
        assert 2 == manager.getBalance(user2);
    }

    @Test
    public void testProcessTransferBlock() {
        String from = "from";
        String to = "to";
        int value = 2;
        String testType = "";
        Block block = createBlock(from, to, testType, value);

        AccountManager manager = AccountManager.testGetInstance("reload");
        int initialValue = 100;
        manager.addValue(from, testType, initialValue);
        assert 100 == manager.getBalance(from);
        manager.processBlock(block);
        assert 2 == manager.getBalance(to);
        manager.processBlock(block);
        assert 96 == manager.getBalance(from);
        assert 4 == manager.getBalance(to);
    }

    @Test
    public void testProcessTransferBlockWithType() {
        String from = "from";
        String to = "to";
        int value = 2;
        String testType = "Test type";
        Block block = createBlock(from, to, testType, value);

        AccountManager manager = AccountManager.testGetInstance("reload");
        int initialValue = 100;
        manager.addValue(from, testType, initialValue);
        assert 100 == manager.getBalance(from, testType);
        manager.processBlock(block);
        assert 2 == manager.getBalance(to, testType);
        manager.processBlock(block);
        assert 96 == manager.getBalance(from, testType);
        assert 4 == manager.getBalance(to, testType);
    }

    private Block createBlock(String from, String to, String testType, int value) {
        Transaction t = new Transaction();

        String testData = "Test Data";

        t.setAssetType(testType);
        t.setData(testData);
        t.setHash(t.calculateDataHash());
        t.setFrom(from);
        t.setToAndValue(to, value);

        BlockHeader h = new BlockHeader();
        String previousHash = "00F2E456";
        h.setPreviousHash(previousHash);

        BlockBody b = new BlockBody();
        t.setHash(t.calculateDataHash());
        b.addTransaction(t);

        return new Block(h, b);
    }

    @Test
    public void testCanSubValue() {
        AccountManager manager = AccountManager.testGetInstance("reload");
        String user1 = "user1";
        manager.addValue(user1, 3);

        assert manager.canSubValue(user1, 2);
        assert manager.canSubValue(user1, 3);
        assert !manager.canSubValue(user1, 4);

        assert manager.canTransferValue(user1, 2);
    }

    @Test
    public void isExternalAddress() {
        String address = "123";
        assert !AccountManager.isExternalAddress(address);

        address = MiningConfiguration.getBlockchainId() + AccountManager.getAddressConnectSymbol() + "";
        assert !AccountManager.isExternalAddress(address);

        String otherString = "123";
        address = MiningConfiguration.getBlockchainId() + otherString + AccountManager.getAddressConnectSymbol() + "";
        assert AccountManager.isExternalAddress(address);
    }

    @Test
    public void initMulitpleAssetAccountBanlance() {
        // initMulitpleAssetAccountBalance in construction with 100 balance
        AccountManager accountManager = AccountManager.getInstance();
        assert 100 == accountManager.getBalance("789001");
        assert 100 == accountManager.getBalance("789008");
        assert 100 == accountManager.getBalance("7890016");
        assert 100 == accountManager.getBalance("789001", "assetType1");
        assert 100 == accountManager.getBalance("789009", "assetType9");
    }
}