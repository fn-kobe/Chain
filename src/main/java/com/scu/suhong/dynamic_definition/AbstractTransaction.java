package com.scu.suhong.dynamic_definition;

import org.json.JSONObject;

public interface AbstractTransaction extends Comparable {
    void postAction();

    int getId();

    String getBlockchainId();

    int getBlockIndex();

    int getTxIndex();

	int getUnifiedIndex();

	void setBlockIndex(int blockId);

    // Tx has its order in a block
    void setTxIndex(int blockId);

    boolean check();

    JSONObject getJson();

    boolean doesMarkedAsDisappear();

    boolean isExternalTransaction();

    String getHash();

    String getData();

    // To prompt the key information of the transaction
    String getIndication();

    void setData(String data);

    void setHash();

    // Used for judge whether one transaction has been put to the blockchaian or not
    boolean isSimilar(AbstractTransaction anotherTransaction);

    /* This is checked when the transaction is in the check of the mining
    *  Default returns true. Override this function to real business logic
     */
    boolean isValid();

    String Dump();

    String checkFailMessage();

    String getOwner();

    String getOwnerWithoutChainId();

    void setOwner(String owner);

    int getGas();

    void setGas(int gas);

    // 0 is to define an asset, 1 is to define and init an asset, 2 is to init an asset
    int getLifecyleType();

    void setLifecyleType(int lifecyleType);

    boolean isOnlyAssetDefinition();

    boolean isOnlyAssetInitiation();

    Long getMiningTime();

    void setMiningTime(Long miningTime);

    boolean isInternalTx();
}
