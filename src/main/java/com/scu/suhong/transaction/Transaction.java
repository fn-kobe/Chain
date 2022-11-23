package com.scu.suhong.transaction;

import account.AccountManager;
import com.scu.suhong.Hash.MD5Hash;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.dynamic_definition.TransactionHelper;
import consensus.pow.MiningConfiguration;
import org.json.JSONObject;
import util.FileLogger;
import util.JSONObjectHelper;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.Set;

/*
 *  Note, setHash should be called, before store or send to other node.
 *  as it indicate one transaction is ready
 * */
public class Transaction implements Serializable, AbstractTransaction {
    static org.apache.log4j.Logger logger = FileLogger.getLogger();
    final int minRandom = 1;
    final int maxRandom = Integer.MAX_VALUE;
    protected String hash = "";
    String assetType = "";
    String publicKey = "";
    String privateKey = "";
    String from = "";
    String to = "";
    int id = -1;
    int exchangeId = 0;//default id
    Long miningTime = 0l;// used to record the time when mining to block for time calculation
    static final String internalTxPrefix = "internalTx:";
    private String keyValueParameter = "";

    /* We simpfy the transaction here to just set the hash of this transaction
               to make sure the Data is send by the owner
               TO DO add the history link here if needed
             */
    int blockIndex = -1;
    int txIndex = -1;
    // Data should be encrypted
    String data = "";
    // support multi-receiver
    //format: to1_value1_to2_value2_...   value1 + value2 + .. = value
    ToValuePair toValuePair = new ToValuePair();
    private int gas;

    public Transaction() {
    }

	public Transaction(Transaction t) {
		setAssetType(t.getAssetType());
		setData(t.getData());
		setPublicKey(t.getPublicKey());
		setPrivateKey(t.getPrivateKey());
		setHash(t.getHash());
		setFrom(t.getFrom());
		setToAndValue(t.getTo(), t.getValue());
		setBlockIndex(t.getBlockIndex());
		setTxIndex(t.getTxIndex());
		setMiningTime(t.getMiningTime());
		setId(t.getId());
		setExchangeId(t.getExchangeId());
		setKeyValueParameter(t.getKeyValueParameter());
	}

    public static Transaction createFromJson(JSONObject object) {
        if (!object.has("d")) { // One transaction must have Data
            return null;
        }
        Transaction t = new Transaction();
        t.setAssetType(JSONObjectHelper.safeGetString(object, "t"));
        t.setData(JSONObjectHelper.safeGetString(object, "d"));
        t.setPublicKey(JSONObjectHelper.safeGetString(object, "pu"));
        t.setPrivateKey(JSONObjectHelper.safeGetString(object, "pr"));
        t.setHash(JSONObjectHelper.safeGetString(object, "h"));
        t.setGas(JSONObjectHelper.safeGetInt(object, "gas"));
        t.setFrom(JSONObjectHelper.safeGetString(object, "from"));
        t.setBlockIndex(JSONObjectHelper.safeGetInt(object, "blockIndex"));
        t.setTxIndex(JSONObjectHelper.safeGetInt(object, "txIndex"));
        t.setMiningTime(JSONObjectHelper.safeGetLong(object, "miningTime"));
        t.setToAndValue(JSONObjectHelper.safeGetString(object, "to"), JSONObjectHelper.safeGetString(object, "value"));
        t.setId(JSONObjectHelper.safeGetInt(object, "id"));
        t.setExchangeId(JSONObjectHelper.safeGetInt(object, "exchangeId"));
		    t.setKeyValueParameter(JSONObjectHelper.safeGetString(object, "keyValueParameter"));
		    return t;

    }

    public String getHash() {
        if (hash == null || hash.isEmpty()) {
            hash = calculateDataHash();
        }
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    // This should be put after all data be set. Or this method will be recalled.
    public void setHash() {
        this.hash = calculateDataHash();
    }

    // blockID is not as the hash as it will change
    public String calculateDataHash() {
        if (-1 == getId()) {
            System.out.println("[Transaction][ERROR] Transaction id is not set before setting hash");
            return "";
        }

        String h = "";
        try {
            if (null != getAssetType()) {
                h += MD5Hash.getValue(getAssetType() + h);
            }
            if (null != getPublicKey()) {
                h = MD5Hash.getValue(getPublicKey() + h);
            }
            if (null != getData()) {
                h = MD5Hash.getValue(getData() + h);
            }
            if (null != getOwner()) {
                h = MD5Hash.getValue(getOwner() + h);
            }
            h = MD5Hash.getValue(getId() + h);
            h = MD5Hash.getValue(getExchangeId() + h);
            if (null != getFrom()) {
                h = MD5Hash.getValue(getFrom() + h);
            }
            if (null != toValuePair) {
                h += toValuePair.calculateDataHash();
            }
            h = MD5Hash.getValue(String.valueOf(getValue() + h));
			      h = MD5Hash.getValue(getKeyValueParameter() + h);
            return h;
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Cannot find MD5 algorithm in makeHash");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int getBlockIndex() {
        return blockIndex;
    }

    @Override
    public int getTxIndex() {
        return blockIndex;
    }

    @Override
    public int getUnifiedIndex() {
        if (0 > blockIndex || 0 > txIndex){
            System.out.println("[Transaction][ERROR] block index and transaction index is not set");
        }
        return blockIndex * 100 + txIndex;
    }

    @Override
    public void setBlockIndex(int blockId) {
        this.blockIndex = blockId;
    }

    @Override
    public void setTxIndex(int txId) {
        this.txIndex = txId;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    // default in this class, derived in EMTransaction
    public String getIncomingAssetType() {
        return assetType;
    }

    // default in this class, derived in EMTransaction
    public String getOutgoingAssetType() {
        return assetType;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    // We want to hide internal Tx prefix
    // IN this function it return data without it
    // For other transaction, it is the same as getFullData
    public String getData() {
        if (!isInternalTx()) return data;

        // Internal prefix
        return data.substring(getInternalTxPrefix().length());
    }

    public String getFullData() {
        return data;
    }

    public void setData(String data) {
        // TO DO use the encrypt
/*        try {
            Encryption encryption = new Encryption();
            this.Data = StringHelper.byteArrayToHexString(encryption.encryptText(Data.getBytes(), privateKey));
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        if (null == data) data = "";
        this.data = data;
    }

    @Override
    public void postAction() {
        System.out.println("[Transaction][WARN] post action is handled by internal balance handler. Skip in Transaction");
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int newID) {
        id = newID;
    }

    @Override
    public boolean check() {
        return AccountManager.getInstance().canProcessTransaction(this);
    }

    public void setId() {
        while (-1 == id) {
            id = generateRandomID();
        }
    }

    public String getTransactionDataHash() throws NoSuchAlgorithmException {
        return MD5Hash.getValue(data);
    }

    public boolean isValid() {
        String calculateDataHash = calculateDataHash();
        String getHash = getHash();
        if (getFrom().isEmpty()) {
            System.out.println("[Transaction][ERROR] No self address");
            return false;
        }
        if (!calculateDataHash.equals(getHash)) {
            System.out.println("[Transaction][ERROR] " + calculateDataHash + " is not the same as hash in block: " + getHash);
            return false;
        }

        // If the address is not start with blockchainID, not in the same blockchain, we don't and can't check its balance
        if (!AccountManager.getInstance().canProcessTransaction(this)) {
            Double balance = AccountManager.getInstance().getBalance(from, assetType);
            System.out.printf("[Transaction][ERROR] Account %s balance %f type %s is not enough to %d\n", from, balance, assetType, getValue());
            return false;
        }
        return true;
    }

    public String Dump() {
        return Dump("");
    }

    @Override
    public String checkFailMessage() {
        return "the balance of " + getFrom() + " is not enough.";
    }

    public String Dump(String additionalMsg) {
        return getJson().toString();
    }

    public JSONObject getJson() {
        JSONObject json = getJsonWithoutHash();
        json.put("h", getHash());
        return json;
    }

    public JSONObject getJsonWithoutHash() {
        JSONObject json = new JSONObject();
        json.put("t", getAssetType());
        json.put("id", getId());
        json.put("exchangeId", getExchangeId());
        json.put("gas", getGas());
        json.put("d", getData());
        json.put("pr", getPrivateKey());
        json.put("pu", getPublicKey());
        json.put("from", getFrom());
        json.put("to", toValuePair.getToString());
        json.put("miningTime", getMiningTime());
        json.put("value", String.valueOf(getValue()));
        json.put("blockIndex", String.valueOf(getBlockIndex()));
        json.put("txIndex", String.valueOf(getTxIndex()));
        json.put("keyValueParameter", keyValueParameter);
        return json;
    }

    public JSONObject getJsonWithoutHashAndData() {
        JSONObject json = new JSONObject();
        json.put("t", getAssetType());
        json.put("id", getId());
        json.put("exchangeId", getExchangeId());
        json.put("pr", getPrivateKey());
        json.put("pu", getPublicKey());
        json.put("from", getFrom());
        json.put("to", toValuePair.getToString());
        json.put("value", String.valueOf(getValue()));
        json.put("gas", getGas());
		    json.put("keyValueParameter", keyValueParameter);
        return json;
    }

    private void clearToList() {
        toValuePair.clear();
    }

    public void setToAndValue(String to, String value) {
        if (null == value || value.isEmpty()) {
            clearToList();
        } else {
            setToAndValue(to, Integer.parseInt(value));
        }
    }

    public void setToAndValue(String to, int value) {
        to = AccountManager.getFullAddress(to);
        toValuePair.setToAndValue(to, value);
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        if (null == from || from.isEmpty()) {
            System.out.println("[CrosschainTransaction] Sender address is empty and we skip setting it");
            return;
        }

        this.from = AccountManager.getFullAddress(from);
    }

    public String getTo() {
        return toValuePair.getToString();
    }

    public Set<String> getToList() {
        return toValuePair.getToList();
    }

    public int getValue() {
        return toValuePair.getValue();
    }

    public int getValue(String receiver) {
        return toValuePair.getValue(receiver);
    }

    public int compareTo(Object o) {
        if (!(o instanceof Transaction)) {
            return 1;
        }
        Transaction otherTransaction = (Transaction) o;
        return this.getHash().compareTo(otherTransaction.getHash());
    }

    int generateRandomID() {
        Random r = new Random();
        return r.nextInt((maxRandom - minRandom) + 1) + minRandom;
    }

    @Override
    public boolean isSimilar(AbstractTransaction anotherTransaction) {
        if (!(anotherTransaction instanceof Transaction)) return false;

        return this.getJsonWithoutHashAndData().similar(((Transaction) anotherTransaction).getJsonWithoutHashAndData());
    }

    @Override
    public boolean isExternalTransaction() {
        String address = getFrom();
        return address.contains(AccountManager.getAddressConnectSymbol())
                && !address.startsWith(MiningConfiguration.getBlockchainId() + AccountManager.getAddressConnectSymbol());
    }

    @Override
    public String getBlockchainId() {
        String address = getFrom();
        String bcId = AccountManager.getBlockchainIDFromAddress(address);

        if (bcId.isEmpty()) {
            System.out.println("[Transaction][WARN] Cannot get blockchain id from address " + address);
        }
        return bcId;
    }

    @Override
    public boolean doesMarkedAsDisappear() {
        return TransactionHelper.doesMarkedAsDisappear(this);
    }


	public String getIndication() {
		return "from " + getFrom() + " to " + getTo() + " with value " + getValue() + " with gas" + getGas() + " in exchange " +
						getExchangeId() + ", keyValueParameter " + keyValueParameter;
	}

    @Override
    public String getOwner() {
        return getFrom();
    }

    @Override
    public String getOwnerWithoutChainId(){
        return AccountManager.getShortAddress(getFrom());
    }

    @Override
    public void setOwner(String owner) {
        setFrom(owner);
    }

    @Override
    public int getGas() {
        return gas;
    }

    @Override
    public void setGas(int gas) {
        this.gas = gas;
    }

    @Override
    public int getLifecyleType() {
        return 2;//always init
    }

    @Override
    public void setLifecyleType(int lifecyleType) {
        //always init, no need to set
    }

    @Override
    public boolean isOnlyAssetDefinition() {
        return false;
    }

    @Override
    public boolean isOnlyAssetInitiation() {
        return true;
    }

    @Override
    public Long getMiningTime() {
        return miningTime;
    }

    @Override
    public void setMiningTime(Long miningTime) {
        this.miningTime = miningTime;
    }

    public int getExchangeId() {
        return exchangeId;
    }

    public void setExchangeId(int exchangeId) {
        this.exchangeId = exchangeId;
    }

    public boolean isInternalTx(){
        return getFullData().startsWith(getInternalTxPrefix());
    }

    public static String getInternalTxPrefix() {
        return internalTxPrefix;
    }

	public String getKeyValueParameter() {
		return keyValueParameter;
	}

	public void setKeyValueParameter(String keyValueParameter) {
		this.keyValueParameter = keyValueParameter;
	}
}
