package com.scu.suhong.smartcontract.P2P;

import account.AccountManager;
import com.scu.suhong.transaction.Transaction;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class P2PHandlerTest {

	@Test
	public void testProcessInvalidTx(){
		P2PHandler p2PHandler = P2PHandler.getInstance();

		System.out.println("[TEST] Test invalid transaction");
		Transaction notP2PTx = new Transaction();
		notP2PTx.setId();
		assert !p2PHandler.process(notP2PTx);
	}

	@Test
	public void testProcessGroupAsset2SC(){
		P2PHandler p2PHandler = P2PHandler.getInstance();
		System.out.println("\n[TEST] Test a simple P2P smart contract with another one");
		System.out.println("[TEST] Begin to send proposal (condition) smart contract");

		// an exchange between user 1 and user 2
		String user1 = "user1";
		String user2 = "user2";
		int initialAssetValue = 100;
		AccountManager.getInstance().addValue(user1, initialAssetValue);
		AccountManager.getInstance().addValue(user2, initialAssetValue);
		assert initialAssetValue == AccountManager.getInstance().getBalance(user1);
		assert initialAssetValue == AccountManager.getInstance().getBalance(user2);

		String groupId = "234";
		String requiredValue1 = "2";
		String givenValue1 = "1";
		String code = "OP_log (giving 1 asset to exchange for 2 asset)";
		Transaction p2pProposalSC = createGroupAssetP2PSC(groupId, user1, requiredValue1, givenValue1, code, 100, 1);
		System.out.println("[TEST] Begin to send matching (state) smart contract with hash " + p2pProposalSC.getHash());
		assert p2PHandler.process(p2pProposalSC);

		code = "OP_log(giving 2 assets for 1 asset)";
		String requiredValue2 = "1";
		String givenValue2 = "2";
		Transaction p2pMatchSC = createGroupAssetP2PSC(groupId, user2, requiredValue2, givenValue2, code, 104, 2);
		System.out.println("[TEST] Begin to send matching smart contract with hash " + p2pMatchSC.getHash());
		assert p2PHandler.process(p2pMatchSC);

		assert p2PHandler.isSCGroupOrAddressMatchCompleted(p2PHandler.makeGroupKey(groupId));
		assert initialAssetValue - Integer.parseInt(givenValue1) + Integer.parseInt(requiredValue1)  == AccountManager.getInstance().getBalance(user1);
		assert initialAssetValue - Integer.parseInt(givenValue2) + Integer.parseInt(requiredValue2)  == AccountManager.getInstance().getBalance(user2);
	}

	@Test
	public void testProcessGroup3SC(){
		P2PHandler p2PHandler = P2PHandler.getInstance();
		System.out.println("\n[TEST] Test a simple P2P smart contract with another one");
		System.out.println("[TEST] Begin to send proposal (condition) smart contract");

		// an exchange between user 1 and user 2
		String user1 = "user1";
		String user2 = "user2";
		String user3 = "user3";
		int initialAssetValue = 100;
		AccountManager.getInstance().addValue(user1, initialAssetValue);
		AccountManager.getInstance().addValue(user2, initialAssetValue);
		AccountManager.getInstance().addValue(user3, initialAssetValue);
		assert initialAssetValue == AccountManager.getInstance().getBalance(user1);
		assert initialAssetValue == AccountManager.getInstance().getBalance(user2);
		assert initialAssetValue == AccountManager.getInstance().getBalance(user3);

		String groupId = "234";
		String requiredValue1 = "3";
		String givenValue1 = "1";
		String code = "OP_log (giving 1 asset to exchange for 3 asset)";
		Transaction p2pProposalSC = createGroupAssetP2PSC(groupId, user1, requiredValue1, givenValue1, code, 100, 1);
		System.out.println("[TEST] Begin to send matching (state) smart contract with hash " + p2pProposalSC.getHash());
		assert p2PHandler.process(p2pProposalSC);

		code = "OP_log(giving 2 assets for 1 asset)";
		String requiredValue2 = "1";
		String givenValue2 = "2";
		Transaction p2pMatchSC1 = createGroupAssetP2PSC(groupId, user2, requiredValue2, givenValue2, code, 104, 2);
		System.out.println("[TEST] Begin to send matching smart contract with hash " + p2pMatchSC1.getHash());
		assert p2PHandler.process(p2pMatchSC1);

		code = "OP_log(giving 3 assets for 2 asset)";
		String requiredValue3 = "2";
		String givenValue3 = "3";
		Transaction p2pMatchSC2 = createGroupAssetP2PSC(groupId, user3, requiredValue3, givenValue3, code, 104, 2);
		System.out.println("[TEST] Begin to send matching smart contract with hash " + p2pMatchSC2.getHash());
		assert p2PHandler.process(p2pMatchSC2);

		assert p2PHandler.isSCGroupOrAddressMatchCompleted(p2PHandler.makeGroupKey(groupId));
		assert initialAssetValue - Integer.parseInt(givenValue1) + Integer.parseInt(requiredValue1)  == AccountManager.getInstance().getBalance(user1);
		assert initialAssetValue - Integer.parseInt(givenValue2) + Integer.parseInt(requiredValue2)  == AccountManager.getInstance().getBalance(user2);
		assert initialAssetValue - Integer.parseInt(givenValue3) + Integer.parseInt(requiredValue3)  == AccountManager.getInstance().getBalance(user3);
	}

	@Test
	public void testProcessGroupAssetOutOfWork(){
		P2PHandler p2PHandler = P2PHandler.getInstance();
		System.out.println("\n[TEST] Test a simple P2P smart contract with another one");
		System.out.println("[TEST] Begin to send proposal (condition) smart contract");

		// an exchange between user 1 and user 2
		String user1 = "user1";
		String user2 = "user2";
		int initialAssetValue = 100;
		AccountManager.getInstance().addValue(user1, initialAssetValue);
		AccountManager.getInstance().addValue(user2, initialAssetValue);
		assert initialAssetValue == AccountManager.getInstance().getBalance(user1);
		assert initialAssetValue == AccountManager.getInstance().getBalance(user2);

		String groupId = "234";
		String requiredValue1 = "2";
		String givenValue1 = "1";
		String code = "OP_log(giving 1 assets for 2 asset)";
		Transaction p2pProposalSC = createGroupAssetP2PSC(groupId, user1, requiredValue1, givenValue1, code, 100, 1);
		System.out.println("[TEST] Begin to send matching (state) smart contract with hash " + p2pProposalSC.getHash());
		assert p2PHandler.process(p2pProposalSC);

		code = "out_of_work";
		String requiredValue2 = "1";
		String givenValue2 = "2";
		Transaction p2pMatchSC = createGroupAssetP2PSC(groupId, user2, requiredValue2, givenValue2, code, 104, 2);
		System.out.println("[TEST] Begin to send matching smart contract with hash " + p2pMatchSC.getHash());
		assert !p2PHandler.process(p2pMatchSC);

		assert !p2PHandler.isSCGroupOrAddressMatchCompleted(p2PHandler.makeGroupKey(groupId));
		assert initialAssetValue - Integer.parseInt(givenValue1)  == AccountManager.getInstance().getBalance(user1);
		assert initialAssetValue  == AccountManager.getInstance().getBalance(user2);
	}

	Transaction createGroupAssetP2PSC(String groupId, String user, String requiredValue, String givenValue, String code, int blockIndex, int txIndex){
		Transaction p2pSC = new Transaction();
		p2pSC.setFrom(user);
		p2pSC.setBlockIndex(blockIndex);
		p2pSC.setTxIndex(txIndex);// used to find minimum path
		p2pSC.setId();

		String data = P2PHandler.p2pHandlerTxKeyword;
		data += P2PHandler.p2pHandlerSeparator + P2PHandler.p2pGroupMatchType + P2PHandler.p2pHandlerSeparator + groupId;

		String conditionForOther = P2PHandler.p2pConditionTypeAsset + P2PHandler.valueSeparator + P2PHandler.p2pLock + P2PHandler.valueSeparator + requiredValue;
		data += P2PHandler.p2pHandlerSeparator + conditionForOther;

		String state = P2PHandler.p2pConditionTypeAsset + P2PHandler.valueSeparator + P2PHandler.p2pLock + P2PHandler.valueSeparator + givenValue;
		data += P2PHandler.p2pHandlerSeparator + state;

		data += P2PHandler.p2pHandlerSeparator + code;

		System.out.printf("[TEST][DEBUG] The P2P smart contract is %s\n", data);
		p2pSC.setData(data);
		return p2pSC;
	}

	@Test
	public void testProcessAllMatchTwiceSC(){
		P2PHandler p2PHandler = P2PHandler.getInstance();
		System.out.println("\n[TEST] Test a simple P2P smart contract with another one");
		System.out.println("[TEST] Begin to send the proposal (condition) smart contract");

		// an exchange between user 1 and user 2
		String user1 = "user1";
		String user2 = "user2";
		String user3 = "user3";
		String user4 = "user4";
		String user5 = "user5";
		int initialAssetValue = 100;
		AccountManager.getInstance().addValue(user1, initialAssetValue);
		AccountManager.getInstance().addValue(user2, initialAssetValue);
		AccountManager.getInstance().addValue(user3, initialAssetValue);
		AccountManager.getInstance().addValue(user4, initialAssetValue);
		AccountManager.getInstance().addValue(user5, initialAssetValue);
		assert initialAssetValue == AccountManager.getInstance().getBalance(user1);
		assert initialAssetValue == AccountManager.getInstance().getBalance(user2);
		assert initialAssetValue == AccountManager.getInstance().getBalance(user3);
		assert initialAssetValue == AccountManager.getInstance().getBalance(user4);
		assert initialAssetValue == AccountManager.getInstance().getBalance(user5);

		// Two completed round tests
		String allMatchingId = "0";// all matching id is 0
		String requiredValue1 = "2";
		String givenValue1 = "1";
		String code = "OP_log (giving 1 asset to exchange for 2 asset)";
		Transaction p2pProposalSC = createAllAssetP2PSC(allMatchingId, user1, requiredValue1, givenValue1, code, 100, 1);
		System.out.println("[TEST] Begin to send matching (state) smart contract with hash " + p2pProposalSC.getHash());
		assert p2PHandler.process(p2pProposalSC);

		code = "OP_log(giving 2 assets for 1 asset)";
		String requiredValue2 = "1";
		String givenValue2 = "2";
		Transaction p2pMatchSC1 = createAllAssetP2PSC(allMatchingId, user2, requiredValue2, givenValue2, code, 101, 2);
		System.out.println("[TEST] Begin to send matching smart contract with hash " + p2pMatchSC1.getHash());
		assert p2PHandler.process(p2pMatchSC1);
		assert initialAssetValue - Integer.parseInt(givenValue1) + Integer.parseInt(requiredValue1)  == AccountManager.getInstance().getBalance(user1);
		assert initialAssetValue - Integer.parseInt(givenValue2) + Integer.parseInt(requiredValue2)  == AccountManager.getInstance().getBalance(user2);

		code = "OP_log(giving 10 assets for 20 asset)";
		String requiredValue3 = "20";
		String givenValue3 = "10";
		Transaction p2pMatchSC2 = createAllAssetP2PSC(allMatchingId, user3, requiredValue3, givenValue3, code, 103, 5);
		System.out.println("[TEST] Begin to send matching smart contract with hash " + p2pMatchSC2.getHash());
		assert p2PHandler.process(p2pMatchSC2);

		code = "OP_log(giving 20 assets for 10 asset)";
		String requiredValue4 = "10";
		String givenValue4 = "20";
		Transaction p2pMatchSC3 = createAllAssetP2PSC(allMatchingId, user4, requiredValue4, givenValue4, code, 104, 2);
		System.out.println("[TEST] Begin to send matching smart contract with hash " + p2pMatchSC3.getHash());
		assert p2PHandler.process(p2pMatchSC3);
		assert initialAssetValue - Integer.parseInt(givenValue3) + Integer.parseInt(requiredValue3)  == AccountManager.getInstance().getBalance(user3);
		assert initialAssetValue - Integer.parseInt(givenValue4) + Integer.parseInt(requiredValue4)  == AccountManager.getInstance().getBalance(user4);
	}

	@Test
	public void testProcessAllMatchTypeIDWarningSC(){
		P2PHandler p2PHandler = P2PHandler.getInstance();
		System.out.println("\n[TEST] Test an all P2P smart contract with another one");

		Transaction p2pMatchSC3 = createAllAssetP2PSC("1", "user", "0", "0", "code", 104, 2);
		assert !p2PHandler.process(p2pMatchSC3);
	}

	Transaction createAllAssetP2PSC(String groupId, String user, String requiredValue, String givenValue, String code, int blockIndex, int txIndex){
		Transaction p2pSC = new Transaction();

		p2pSC.setFrom(user);
		p2pSC.setBlockIndex(blockIndex);
		p2pSC.setTxIndex(txIndex);// used to find minimum path
		p2pSC.setId();

		String data = P2PHandler.p2pHandlerTxKeyword;
		data += P2PHandler.p2pHandlerSeparator + P2PHandler.p2pAllMatchType + P2PHandler.p2pHandlerSeparator + groupId;

		String conditionForOther = P2PHandler.p2pConditionTypeAsset + P2PHandler.valueSeparator + P2PHandler.p2pLock + P2PHandler.valueSeparator + requiredValue;
		data += P2PHandler.p2pHandlerSeparator + conditionForOther;

		String state = P2PHandler.p2pConditionTypeAsset + P2PHandler.valueSeparator + P2PHandler.p2pLock + P2PHandler.valueSeparator + givenValue;
		data += P2PHandler.p2pHandlerSeparator + state;

		data += P2PHandler.p2pHandlerSeparator + code;

		p2pSC.setData(data);
		return p2pSC;
	}

	@Test
	public void testProcessGroupAddress2SC(){
		P2PHandler p2PHandler = P2PHandler.getInstance();
		System.out.println("\n[TEST] Test a simple P2P smart contract with another one");
		System.out.println("[TEST] Begin to send proposal (condition) smart contract");
		Transaction p2pProposalSC = new Transaction();
		p2pProposalSC.setBlockIndex(100);
		p2pProposalSC.setTxIndex(1);// used to find minimum path
		p2pProposalSC.setId();
		String groupId = "234";
		String data = P2PHandler.p2pHandlerTxKeyword;
		data += P2PHandler.p2pHandlerSeparator + P2PHandler.p2pGroupMatchType + P2PHandler.p2pHandlerSeparator + groupId;

		String requiredAddress = "user1";
		String conditionForOther = P2PHandler.p2pConditionTypeAddress + P2PHandler.valueSeparator + requiredAddress;
		data += P2PHandler.p2pHandlerSeparator + conditionForOther;

		String state = "";
		data += P2PHandler.p2pHandlerSeparator + state;

		String code = "NOP";
		data += P2PHandler.p2pHandlerSeparator + code;

		p2pProposalSC.setData(data);
		System.out.println("[TEST] Begin to send matching (state) smart contract with hash " + p2pProposalSC.getHash());
		assert p2PHandler.process(p2pProposalSC);

		Transaction p2pMatchSC = new Transaction();
		p2pMatchSC.setFrom(requiredAddress);
		p2pProposalSC.setBlockIndex(101);
		p2pMatchSC.setTxIndex(3);// used to find minimum path
		p2pMatchSC.setId();
		groupId = "234";
		data = P2PHandler.p2pHandlerTxKeyword;
		data += P2PHandler.p2pHandlerSeparator + P2PHandler.p2pGroupMatchType + P2PHandler.p2pHandlerSeparator + groupId;

		conditionForOther ="";
		data += P2PHandler.p2pHandlerSeparator + conditionForOther;

		state = P2PHandler.p2pConditionTypeAddress + P2PHandler.valueSeparator + requiredAddress;
		data += P2PHandler.p2pHandlerSeparator + state;

		code = "NOP";
		data += P2PHandler.p2pHandlerSeparator + code;

		p2pMatchSC.setData(data);
		System.out.println("[TEST] Begin to send matching smart contract with hash " + p2pMatchSC.getHash());
		assert p2PHandler.process(p2pMatchSC);
		assert p2PHandler.isSCGroupOrAddressMatchCompleted(p2PHandler.makeGroupKey(groupId));
	}

	@Test
	public void testProcessGroupAddress3SC(){
		String user1 = "user1";
		String user2 = "user2";
		String user3 = "user3";
		P2PHandler p2PHandler = P2PHandler.getInstance();
		System.out.println("\n[TEST] Test a simple P2P smart contract with another one");
		System.out.println("[TEST] Begin to send proposal (condition) smart contract");
		Transaction p2pProposalSC = new Transaction();
		p2pProposalSC.setBlockIndex(100);
		p2pProposalSC.setTxIndex(1);// used to find minimum path
		p2pProposalSC.setFrom(user1);
		p2pProposalSC.setId();
		String groupId = "234";
		String data = P2PHandler.p2pHandlerTxKeyword;
		data += P2PHandler.p2pHandlerSeparator + P2PHandler.p2pGroupMatchType + P2PHandler.p2pHandlerSeparator + groupId;

		String requiredAddress1 = user2;
		String conditionForOther = P2PHandler.p2pConditionTypeAddress + P2PHandler.valueSeparator + requiredAddress1;
		data += P2PHandler.p2pHandlerSeparator + conditionForOther;

		String state = "";
		data += P2PHandler.p2pHandlerSeparator + state;

		String code = "NOP";
		data += P2PHandler.p2pHandlerSeparator + code;

		p2pProposalSC.setData(data);
		System.out.println("[TEST] Begin to send matching (state) smart contract with hash " + p2pProposalSC.getHash());
		assert p2PHandler.process(p2pProposalSC);

		Transaction p2pSC2 = new Transaction();
		p2pSC2.setFrom(requiredAddress1);
		p2pSC2.setBlockIndex(101);
		p2pSC2.setTxIndex(3);// used to find minimum path
		p2pSC2.setFrom(user2);
		p2pSC2.setId();
		groupId = "234";
		data = P2PHandler.p2pHandlerTxKeyword;
		data += P2PHandler.p2pHandlerSeparator + P2PHandler.p2pGroupMatchType + P2PHandler.p2pHandlerSeparator + groupId;

		String requiredAddress2 = user3;
		conditionForOther = P2PHandler.p2pConditionTypeAddress + P2PHandler.valueSeparator + requiredAddress2;
		data += P2PHandler.p2pHandlerSeparator + conditionForOther;

		state = P2PHandler.p2pConditionTypeAddress + P2PHandler.valueSeparator + requiredAddress1;
		data += P2PHandler.p2pHandlerSeparator + state;

		code = "NOP";
		data += P2PHandler.p2pHandlerSeparator + code;

		p2pSC2.setData(data);
		System.out.println("[TEST] Begin to send matching smart contract with hash " + p2pSC2.getHash());
		assert p2PHandler.process(p2pSC2);
		assert !p2PHandler.isSCGroupOrAddressMatchCompleted(p2PHandler.makeGroupKey(groupId));

		Transaction p2pSC3 = new Transaction();
		p2pSC3.setFrom(requiredAddress2);
		p2pSC3.setBlockIndex(103);
		p2pSC3.setTxIndex(2);// used to find minimum path
		p2pSC3.setFrom(user3);
		p2pSC3.setId();
		groupId = "234";
		data = P2PHandler.p2pHandlerTxKeyword;
		data += P2PHandler.p2pHandlerSeparator + P2PHandler.p2pGroupMatchType + P2PHandler.p2pHandlerSeparator + groupId;

		conditionForOther ="";
		data += P2PHandler.p2pHandlerSeparator + conditionForOther;

		state = P2PHandler.p2pConditionTypeAddress + P2PHandler.valueSeparator + requiredAddress2;
		data += P2PHandler.p2pHandlerSeparator + state;

		code = "End action in last P2P smart contract";
		data += P2PHandler.p2pHandlerSeparator + code;

		p2pSC3.setData(data);
		System.out.println("[TEST] Begin to send matching smart contract with hash " + p2pSC3.getHash());
		assert p2PHandler.process(p2pSC3);
		assert p2PHandler.isSCGroupOrAddressMatchCompleted(p2PHandler.makeGroupKey(groupId));
	}

	@Test
	public void testProcessInvalidAddressTx(){
		P2PHandler p2PHandler = P2PHandler.getInstance();

		System.out.println("[TEST] Test invalid address transaction");
		Transaction invalidAddressTypeSC = new Transaction();
		invalidAddressTypeSC.setBlockIndex(100);
		invalidAddressTypeSC.setTxIndex(1);// used to find minimum path
		invalidAddressTypeSC.setId();
		String groupId = "123";
		String data = P2PHandler.p2pHandlerTxKeyword;
		data += P2PHandler.p2pHandlerSeparator + P2PHandler.p2pAddressMatchType + P2PHandler.p2pHandlerSeparator + groupId;

		String requiredAddress = "user1";
		String invalidAddressCondition = "invalidAddressCondition";
		String conditionForOther = invalidAddressCondition + P2PHandler.valueSeparator + requiredAddress;
		data += P2PHandler.p2pHandlerSeparator + conditionForOther;

		String state = "";
		data += P2PHandler.p2pHandlerSeparator + state;

		String code = "NOP";
		data += P2PHandler.p2pHandlerSeparator + code;

		invalidAddressTypeSC.setData(data);
		assert !p2PHandler.process(invalidAddressTypeSC);
	}

	@Test
	public void testProcessAddress2SC(){
		P2PHandler p2PHandler = P2PHandler.getInstance();
		System.out.println("\n[TEST] Test a simple P2P smart contract with another one");
		System.out.println("[TEST] Begin to send proposal (condition) smart contract");
		Transaction p2pProposalSC = new Transaction();
		p2pProposalSC.setBlockIndex(100);
		p2pProposalSC.setTxIndex(1);// used to find minimum path
		p2pProposalSC.setId();
		String groupId = "234";
		String data = P2PHandler.p2pHandlerTxKeyword;
		data += P2PHandler.p2pHandlerSeparator + P2PHandler.p2pAddressMatchType + P2PHandler.p2pHandlerSeparator + groupId;

		String requiredAddress = "user1";
		String conditionForOther = P2PHandler.p2pConditionTypeAddress + P2PHandler.valueSeparator + requiredAddress;
		data += P2PHandler.p2pHandlerSeparator + conditionForOther;

		String state = "";
		data += P2PHandler.p2pHandlerSeparator + state;

		String code = "NOP";
		data += P2PHandler.p2pHandlerSeparator + code;

		System.out.println("[TEST][DEBUG] The address matching smart contract is " + data);
		p2pProposalSC.setData(data);
		System.out.println("[TEST] Begin to send matching (state) smart contract with hash " + p2pProposalSC.getHash());
		assert p2PHandler.process(p2pProposalSC);

		Transaction p2pMatchSC = new Transaction();
		p2pMatchSC.setFrom(requiredAddress);
		p2pProposalSC.setBlockIndex(101);
		p2pMatchSC.setTxIndex(3);// used to find minimum path
		p2pMatchSC.setId();
		groupId = "234";
		data = P2PHandler.p2pHandlerTxKeyword;
		data += P2PHandler.p2pHandlerSeparator + P2PHandler.p2pAddressMatchType + P2PHandler.p2pHandlerSeparator + groupId;

		conditionForOther ="";
		data += P2PHandler.p2pHandlerSeparator + conditionForOther;

		state = P2PHandler.p2pConditionTypeAddress + P2PHandler.valueSeparator + requiredAddress;
		data += P2PHandler.p2pHandlerSeparator + state;

		code = "NOP";
		data += P2PHandler.p2pHandlerSeparator + code;

		p2pMatchSC.setData(data);
		System.out.println("[TEST] Begin to send matching smart contract with hash " + p2pMatchSC.getHash());
		assert p2PHandler.process(p2pMatchSC);
		assert p2PHandler.isSCGroupOrAddressMatchCompleted(p2PHandler.makeAddressKey(groupId));
	}

	@Test
	public void testProcessAddress3SC(){
		P2PHandler p2PHandler = P2PHandler.getInstance();
		System.out.println("\n[TEST] Test a simple P2P smart contract with another one");
		System.out.println("[TEST] Begin to send proposal (condition) smart contract");
		Transaction p2pProposalSC = new Transaction();
		p2pProposalSC.setBlockIndex(100);
		p2pProposalSC.setTxIndex(1);// used to find minimum path
		p2pProposalSC.setId();
		String groupId = "234";
		String data = P2PHandler.p2pHandlerTxKeyword;
		data += P2PHandler.p2pHandlerSeparator + P2PHandler.p2pAddressMatchType + P2PHandler.p2pHandlerSeparator + groupId;

		String requiredAddress1 = "user1";
		String conditionForOther = P2PHandler.p2pConditionTypeAddress + P2PHandler.valueSeparator + requiredAddress1;
		data += P2PHandler.p2pHandlerSeparator + conditionForOther;

		String state = "";
		data += P2PHandler.p2pHandlerSeparator + state;

		String code = "NOP";
		data += P2PHandler.p2pHandlerSeparator + code;

		p2pProposalSC.setData(data);
		System.out.println("[TEST] Begin to send matching (state) smart contract with hash " + p2pProposalSC.getHash());
		assert p2PHandler.process(p2pProposalSC);

		Transaction p2pSC2 = new Transaction();
		p2pSC2.setFrom(requiredAddress1);
		p2pSC2.setBlockIndex(101);
		p2pSC2.setTxIndex(3);// used to find minimum path
		p2pSC2.setId();
		groupId = "234";
		data = P2PHandler.p2pHandlerTxKeyword;
		data += P2PHandler.p2pHandlerSeparator + P2PHandler.p2pAddressMatchType + P2PHandler.p2pHandlerSeparator + groupId;

		String requiredAddress2 = "user2";
		conditionForOther = P2PHandler.p2pConditionTypeAddress + P2PHandler.valueSeparator + requiredAddress2;
		data += P2PHandler.p2pHandlerSeparator + conditionForOther;

		state = P2PHandler.p2pConditionTypeAddress + P2PHandler.valueSeparator + requiredAddress1;
		data += P2PHandler.p2pHandlerSeparator + state;

		code = "NOP";
		data += P2PHandler.p2pHandlerSeparator + code;

		p2pSC2.setData(data);
		System.out.println("[TEST] Begin to send matching smart contract with hash " + p2pSC2.getHash());
		assert p2PHandler.process(p2pSC2);
		assert !p2PHandler.isSCGroupOrAddressMatchCompleted(p2PHandler.makeGroupKey(groupId));

		Transaction p2pSC3 = new Transaction();
		p2pSC3.setFrom(requiredAddress2);
		p2pSC3.setBlockIndex(103);
		p2pSC3.setTxIndex(2);// used to find minimum path
		p2pSC3.setId();
		groupId = "234";
		data = P2PHandler.p2pHandlerTxKeyword;
		data += P2PHandler.p2pHandlerSeparator + P2PHandler.p2pAddressMatchType + P2PHandler.p2pHandlerSeparator + groupId;

		conditionForOther ="";
		data += P2PHandler.p2pHandlerSeparator + conditionForOther;

		state = P2PHandler.p2pConditionTypeAddress + P2PHandler.valueSeparator + requiredAddress2;
		data += P2PHandler.p2pHandlerSeparator + state;

		code = "End action in last P2P smart contract";
		data += P2PHandler.p2pHandlerSeparator + code;

		p2pSC3.setData(data);
		System.out.println("[TEST] Begin to send matching smart contract with hash " + p2pSC3.getHash());
		assert p2PHandler.process(p2pSC3);
		assert p2PHandler.isSCGroupOrAddressMatchCompleted(p2PHandler.makeAddressKey(groupId));
	}
}