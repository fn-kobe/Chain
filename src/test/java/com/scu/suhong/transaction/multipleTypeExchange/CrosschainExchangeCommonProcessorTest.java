package com.scu.suhong.transaction.multipleTypeExchange;

import Service.BlockchainService;
import account.AccountManager;
import com.scu.suhong.block.Block;
import com.scu.suhong.block.BlockChain;
import com.scu.suhong.block.BlockException;
import com.scu.suhong.miner.Miner;
import com.scu.suhong.transaction.CommonCrosschainTransaction;
import com.scu.suhong.transaction.Transaction;
import consensus.pow.MiningConfiguration;
import org.json.JSONObject;
import org.junit.Test;
import util.ThreadHelper;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class CrosschainExchangeCommonProcessorTest {

    @Test
    public void process() {
        AccountManager accountManager = AccountManager.getInstance();
        int exchangeId = 10090;
        String user1 = "user1";
        String user2 = "user2";
        int value = 10;
        int initialValue = 100;
        accountManager.addValue(user1, initialValue);
        accountManager.addValue(user2, initialValue);
        assert initialValue == accountManager.getBalance(user1);
        assert initialValue == accountManager.getBalance(user2);
        String defaultAssetType = "internalCoin";
        String chainId = MiningConfiguration.getBlockchainStringId();
        String agreementData = "tradingTerms";
        //CRTX_<...>;<requiredData - chainID:from:to:assetType:value:data>;<providedData>
        String data = String.format("CRTX_test;%s:%s:%s:%s:%d:%s;%s",
                chainId,user2, user1, defaultAssetType, value, agreementData, agreementData);
        CommonCrosschainTransaction t = createTransaction(exchangeId, user1, user2, value, data);
        CrosschainExchangeCommonProcessor processor = CrosschainExchangeCommonProcessor.getInstance();
        processor.testSetTimeoutValue(5);
        processor.processNewTx(t);

        String missingData = "missingData";
        data = String.format("CRTX_test;%s:%s:%s:%s:%d:%s;%s",
                chainId,user1, user2, defaultAssetType, value, agreementData, missingData);
        t = createTransaction(exchangeId, user2, user1, value, data);
        processor.processNewTx(t);

        ThreadHelper.safeSleepSecond(3);// Enlarge completion time of the exchange
        data = String.format("CRTX_test;%s:%s:%s:%s:%d:%s;%s",
                chainId,user1, user2, defaultAssetType, value, agreementData, agreementData);
        t = createTransaction(exchangeId, user2, user1, value, data);
        processor.processNewTx(t);

        assert accountManager.getBalance(user1) == initialValue;
        assert accountManager.getBalance(user2) == initialValue - value;// missing one init value
        //Timeout and asset restored
        ThreadHelper.safeSleepSecond(CrosschainExchangeCommonProcessorThread.getSleepInterval() * 2);
        assert accountManager.getBalance(user2) == initialValue;// missing one init value
    }

    @Test
    public void processTwoRequirement() {
        AccountManager accountManager = AccountManager.getInstance();
        int exchangeId = 10090;
        String user1 = "user1";
        String user2 = "user2";
        int value = 10;
        int value2 = 20;
        int initialValue = 100;
        accountManager.addValue(user1, initialValue);
        accountManager.addValue(user2, initialValue);
        assert initialValue == accountManager.getBalance(user1);
        assert initialValue == accountManager.getBalance(user2);
        String defaultAssetType = "internalCoin";
        String chainId = MiningConfiguration.getBlockchainStringId();
        String agreementData = "tradingTerms";
        //CRTX_<...>;<requiredData - chainID:from:to:assetType:value:data>;<providedData>
        String data = String.format("CRTX_test;%s:%s:%s:%s:%d:%s;%s;;CRTX_test;%s:%s:%s:%s:%d:%s;%s",
                chainId,user2, user1, defaultAssetType, value, agreementData, agreementData,
                chainId,user2, user1, defaultAssetType, value2, agreementData, agreementData);

        CommonCrosschainTransaction t = createTransaction(exchangeId, user1, user2, value, data);
        CrosschainExchangeCommonProcessor processor = CrosschainExchangeCommonProcessor.getInstance();
        processor.processNewTx(t);

        data = String.format("CRTX_test;%s:%s:%s:%s:%d:%s;%s",
                chainId,user1, user2, defaultAssetType, value, agreementData, agreementData);
        t = createTransaction(exchangeId, user2, user1, value2, data);
        processor.processNewTx(t);

        data = String.format("CRTX_test;%s:%s:%s:%s:%d:%s;%s",
                chainId,user1, user2, defaultAssetType, value, agreementData, agreementData);
        t = createTransaction(exchangeId, user2, user1, value, data);
        processor.processNewTx(t);

        ThreadHelper.safeSleepSecond(1);// Enlarge completion time of the exchange
        assert accountManager.getBalance(user1) == initialValue + value2;
        assert accountManager.getBalance(user2) == initialValue - value2;// missing one init value
    }

    int id = 0;// to for tx differentiation
    CommonCrosschainTransaction createTransaction(int exchangeId, String from, String to, int value, String data) {
        CommonCrosschainTransaction t = new CommonCrosschainTransaction();

        t.setInteractionId(exchangeId);
        t.setFrom(from);
        t.setToAndValue(to, value);
        t.setData(data);
        t.setId(++id);
        t.setHash();
        return t;
    }

    @Test
    public void testFromBlock() throws UnknownHostException, BlockException {

        MiningConfiguration.testSetRequiredZeroCount(0);
        BlockChain blockChain = BlockChain.getInstance();
        blockChain.testRest();
        String blockData = "{\"b\":{\"ts\":[{\"super\":{\"pr\":\"\",\"d\":\"CRTX_11;222:789002:789001:internalCoin:2:Agree;Agree;;CRTX_11;222:789002:789001:internalCoin:10:Agree;Agree\",\"pu\":\"\",\"h\":\"E920950880D6CD23D35995DCD4DDC5D3\",\"exchangeId\":0,\"miningTime\":1588666946301,\"blockIndex\":\"13\",\"t\":\"\",\"gas\":0,\"from\":\"111?789001\",\"id\":1,\"to\":\"111?789002\",\"value\":\"2\"},\"interactionId\":10081,\"CommonCrosschainTransaction\":\"CommonCrosschainTransaction\"},{\"super\":{\"pr\":\"\",\"d\":\"CRTX_21;111:789001:789002:internalCoin:2:Agree;Agree\",\"pu\":\"\",\"h\":\"EC50EB97A4C906DBBC1B507053C243FC\",\"exchangeId\":0,\"miningTime\":1588666946301,\"blockIndex\":\"13\",\"t\":\"\",\"gas\":0,\"from\":\"222?789002\",\"id\":2,\"to\":\"222?789001\",\"value\":\"2\"},\"interactionId\":10081,\"CommonCrosschainTransaction\":\"CommonCrosschainTransaction\"}]},\"h\":{\"p\":\"0000000C91B7D0459D6B0E0EA163B149\",\"timeStamp\":\"2020.05.05_04.22.26\",\"index\":1,\"n\":1875497120,\"miner\":\"111?45678_02\"},\"hash\":\"000008573D8E79A30DA4A3D1C23525B3\"}";
        Block block = Block.createFromJson(new JSONObject(blockData));
        assert null != block;
        System.out.println("\n[Test] Begin to process first block");
        blockChain.addBlock(block);

        blockData = "{\"b\":{\"ts\":[{\"super\":{\"pr\":\"\",\"d\":\"CRTX_22;111:789001:789002:internalCoin:2:Agree;DisAgree\",\"pu\":\"\",\"h\":\"F0002393D96E02DA434973174D54BFA3\",\"exchangeId\":0,\"miningTime\":1588666946301,\"blockIndex\":\"13\",\"t\":\"\",\"gas\":0,\"from\":\"222?789002\",\"id\":3,\"to\":\"222?789001\",\"value\":\"10\"},\"interactionId\":10081,\"CommonCrosschainTransaction\":\"CommonCrosschainTransaction\"},{\"super\":{\"pr\":\"\",\"d\":\"CRTX_23;111:789001:789002:internalCoin:2:Agree;Agree\",\"pu\":\"\",\"h\":\"905513AFF5C9D23F349926759360491B\",\"exchangeId\":0,\"miningTime\":1588666946301,\"blockIndex\":\"2\",\"t\":\"\",\"gas\":0,\"from\":\"222?789002\",\"id\":4,\"to\":\"222?789001\",\"value\":\"10\"},\"interactionId\":10081,\"CommonCrosschainTransaction\":\"CommonCrosschainTransaction\"}]},\"h\":{\"p\":\"2D912169978F50E686C4AB32C3F783B1\",\"timeStamp\":\"2020.05.05_04.22.26\",\"index\":13,\"n\":1875497120,\"miner\":\"111?45678_02\"},\"hash\":\"000008573D8E79A30DA4A3D1C23525B3\"}   ";

        System.out.println("\n[Test] Begin to process second block");
        block = Block.createFromJson(new JSONObject(blockData));
        blockChain.addBlock(block);
        ThreadHelper.safeSleepSecond(2);
    }
}