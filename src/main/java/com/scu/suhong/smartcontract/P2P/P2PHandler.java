package com.scu.suhong.smartcontract.P2P;

import account.AccountManager;
import com.scu.suhong.block.Block;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.graph.CircleAndNonCirclePath;
import com.scu.suhong.graph.JGraphTWrapper;
import com.scu.suhong.transaction.Transaction;
import org.jetbrains.annotations.Nullable;
import org.jgrapht.graph.DefaultEdge;
import util.TimeHelper;

import java.util.*;

// Currently, we only process smart contract has one condition - i.e. requires one dependent smart contract
// And one state to match another one smart contract
// To enhance this to more conditions in future research
public class P2PHandler {
	static P2PHandler instance = new P2PHandler();

	public final static String p2pHandlerTxKeyword = "P2PSC";
	public final static String p2pHandlerSeparator = ":";
	public final static String p2pAllMatchType = "all";
	public final static String p2pGroupMatchType = "group";
	public final static String p2pAddressMatchType = "address";

	public final static String p2pConditionTypeAddress = "address";
	public final static String p2pConditionTypeAsset = "asset";
	public final static String p2pLock = "lock";
	public final static String p2pStateTypeAddress = "address";
	// currently we only set all matching type to 0, which will make all this kind of transaction
	public final static String p2pAllMatchTypeId = "0";

	Map<String, JGraphTWrapper> jGraphTWrapperMap = new HashMap<>();
	Map<String, Transaction> hashTransactionMap = new HashMap<>();
	Map<String, String> hasMatchedConditionMap = new HashMap<>();
	Map<String, String> hasMatchedStateMap = new HashMap<>();
	Map<String, String> proposalSCMap = new HashMap<>();
	Map<String, String> completedSCGroupMap = new HashMap<>();// Currently used to output cooperation status
	final static int commonWeightForAllP2PSC = 100;

	public final static String valueSeparator = "#";
	int tacticSelection = 0;

	// Counters to measure the different aspects.
	// Currently, we only compare all group and address, then only three map item in each counter
	// Enhance this if want to
	Map<String, Integer> timeCounter = new HashMap<>();
	Map<String, Integer> ongoingUserCounter = new HashMap<>();
	Map<String, Integer> frozenAssetCounter = new HashMap<>();

	String timeCounterName = "timeCounter";
	String ongoingUserCounterName = "ongoingUserCounter";
	String frozenAssetValueCounterName = "frozenAssetValueCounter";

	public static P2PHandler getInstance() {
		return instance;
	}

	private P2PHandler() {
	}

	public void tryAddNewBlock(Block block) {
		System.out.println("[P2PHandler][DEBUG] Try add new block");
		for (AbstractTransaction t : block.getTransactions()) {
			System.out.println("[P2PHandler][DEBUG] Try process transaction " + t.getId());
			if (t instanceof Transaction) {
				process((Transaction) t);
			}
			System.out.println("[P2PHandler][DEBUG] Finished to process transaction " + t.getId());
		}
	}

	void outputCounter(){
		P2PUtility.outputCounter(timeCounter, timeCounterName);
		P2PUtility.outputCounter(ongoingUserCounter, ongoingUserCounterName);
		P2PUtility.outputCounter(frozenAssetCounter, frozenAssetValueCounterName);
	}

	public boolean process(Transaction t) {
		// <p2pHandlerKeyword>:<p2pSCType>:<p2pSCTypeId>:<conditionForOther>:<state>:<scCode>
		if (!isP2PSC(t)) return false;

		String[] txParsedDataList = t.getData().split(p2pHandlerSeparator);
		String p2pSCType = txParsedDataList[1];
		String p2pSCTypeId = txParsedDataList[2];
		String conditionForOther = txParsedDataList[3];
		String groupKey = P2PUtility.makeKey(p2pSCType, p2pSCTypeId);
		if (InternalCodeRunner.isSCOutOfWork(groupKey) || InternalCodeRunner.doesTheCodeSimulateOutOfWork(getP2PSCCode(t), groupKey)){
			System.out.printf("[P2PHandler][ERROR] Smart contract %s from %s has been out of work or it is to simulate out of work.\n",
							groupKey, AccountManager.getShortAddress(t.getFrom()));
			outputCounter();
			return false;
		}

		// 1. prepare state as declared in the state field
		if (!prepareState(t)) return false;

		//2. (a) Add into the graph, and (b) judge the completion
		if (p2pSCType.equals(p2pAllMatchType)) {
			System.out.println("[P2PHandler][INFO] Try to process in all matching way");
			return processAllType(t, conditionForOther, groupKey);
		}
		else if (p2pSCType.equals(p2pGroupMatchType)) {
			System.out.println("[P2PHandler][INFO] Try to process in group matching way");
			return processGroupType(t, conditionForOther, groupKey);
		}
		else if (p2pSCType.contains(p2pAddressMatchType)) {
			System.out.println("[P2PHandler][INFO] Try to process in address matching way");
			return processAddressType(t, conditionForOther, groupKey);
		} else  {
			System.out.printf("[P2PHandler][ERROR] No p2pHandler keyword is in transaction %d\n", t.getId());
			return false;
		}
	}

	boolean processAddressType(Transaction t, String conditionForOther, String groupKey) {
		if (!isAddressSC(t)) return false;
		return doProcessGroupOrAddressType(t, conditionForOther, groupKey);
	}

	boolean processAllType(Transaction t, String conditionForOther, String groupKey) {
		String typeId = getP2PSCTypeId(t);
		if (!typeId.equals(p2pAllMatchTypeId)){
			System.out.printf("[P2PHandler][WARN] Type id is %s instead of 0, which will make the all matching be divided into different groups. " +
							"The original design is to make all this kind of P2P smart contract in one group.\n", typeId);
			return false;
		}
		return doProcessGroupOrAddressType(t, conditionForOther, groupKey, false, true);
	}

	boolean isAddressSC(Transaction t) {
		String condition = getP2PSCCondition(t);
		if (!condition.isEmpty() && !getP2PSCConditionType(t).equals(p2pConditionTypeAddress)){
			System.out.printf("[P2PHandler][ERROR] Not a valid address matching smart contract '%s', as its required condition '%s' is invalid\n",
							t.getHash(), condition);
			return false;
		}
		String state = getP2PSCState(t);
		if (!state.isEmpty() && !getP2PSCStateType(t).equals(p2pStateTypeAddress)){
			System.out.printf("[P2PHandler][ERROR] Not a valid address matching transaction '%s', as its required state '%s' is invalid\n",
							t.getHash(), state);
			return false;
		}
		return true;
	}

	boolean processGroupType(Transaction t, String conditionForOther, String groupKey) {
		return doProcessGroupOrAddressType(t, conditionForOther, groupKey);
	}

	boolean doProcessGroupOrAddressType(Transaction t, String conditionForOther, String groupKey) {
		return doProcessGroupOrAddressType(t, conditionForOther, groupKey, true, false);
	}

	boolean doProcessGroupOrAddressType(Transaction t, String conditionForOther, String groupKey,
																			boolean setCompletionFlagToPreventFutureProcess, boolean isAllType) {
		long startEpoc = TimeHelper.getEpoch();

		if (!check(t, conditionForOther, groupKey)) {
			updateTimeCounter(startEpoc, groupKey);
			return false;
		}

		increaseOngoingUserCounter(t.getFrom(),groupKey);
		addFrozenAssetValueCounter(t.getFrom(), getP2PSCStateAssetValue(t), groupKey);
		System.out.printf("[P2PHandler][INFO] Begin to process P2P smart contract with data %s\n", t.getData());
		hashTransactionMap.put(t.getHash(), t);
		// 1. check and set the condition and state matching status
		JGraphTWrapper jGraphTWrapper = jGraphTWrapperMap.get(groupKey);
		processConditionAndState(t, jGraphTWrapper);

		//2.b judge the completion. this waste some resources
		// A leading SC is a proposal SC (in non circle path) or current SC (circle path)
		List<String> leadingSCList = findPossibleLeadingSC(t, groupKey, jGraphTWrapper, isAllType);
		if (leadingSCList.isEmpty()) {
			updateTimeCounter(startEpoc, groupKey);
			return true;
		}

		// First, we find the paths from the graph with circle  or a line path of leading sc
		CircleAndNonCirclePath graphPath = jGraphTWrapper.getSuccessivePath(leadingSCList);

		// Path from graph must be a completed path. If several paths, we choose the one according to tactic selection algorithm
		List<DefaultEdge> completedAndTacticSelectedPath = findCompletedAndTacticSelectedPath(graphPath, jGraphTWrapper);
		//3. Process cooperation completion
		if (!completedAndTacticSelectedPath.isEmpty()) { // found correct path
			System.out.printf("[P2PHandler][INFO] %sP2P smart contract %s has completed after process of %s\n",
							(isAllType?"One path of ":""), groupKey, AccountManager.getShortAddress(t.getFrom()));
			// a. process asset, b. run code, c. set flag
			processCompletion(completedAndTacticSelectedPath, jGraphTWrapper);
			handleCompletionFlag(groupKey, setCompletionFlagToPreventFutureProcess, jGraphTWrapper, completedAndTacticSelectedPath);
		} else {
			System.out.printf("[P2PHandler][INFO] P2P smart contract %s has not completed\n", groupKey);
		}
		updateTimeCounter(startEpoc, groupKey);
		return true;
	}

	void updateTimeCounter(long startEpoc, String groupKey){
		long endEpoc = TimeHelper.getEpoch();
		int value = 0;
		if (timeCounter.containsKey(groupKey)) value += timeCounter.get(groupKey) + Math.toIntExact(endEpoc - startEpoc);
		P2PUtility.updateCounter(value, groupKey, timeCounter, timeCounterName);
	}

	void increaseOngoingUserCounter(String user, String groupKey){
		System.out.printf("[P2PHandler][INFO] P2P Begin to add user %s to ongoing counter list of %s\n", user, groupKey);
		int value = 1;
		if (ongoingUserCounter.containsKey(groupKey)) value += ongoingUserCounter.get(groupKey);
		P2PUtility.updateCounter(value, groupKey, ongoingUserCounter, ongoingUserCounterName);
	}

	void decreaseOngoingCounter(String user, String groupKey){
		System.out.printf("[P2PHandler][INFO] P2P Begin to remove user '%s' to ongoing counter list of %s\n", user, groupKey);
		if (!ongoingUserCounter.containsKey(groupKey)) {
			System.out.printf("[P2PHandler][ERROR] Ongoing counter list of %s does not exist for %s\n", groupKey, user);
			return;
		}
		P2PUtility.updateCounter(ongoingUserCounter.get(groupKey) - 1, groupKey, ongoingUserCounter, ongoingUserCounterName);
	}

	void addFrozenAssetValueCounter(String user, int value, String groupKey){
		System.out.printf("[P2PHandler][INFO] P2P Begin to add user %s to frozen counter list of %s\n", user, groupKey);
		if (frozenAssetCounter.containsKey(groupKey)) value += frozenAssetCounter.get(groupKey);
		P2PUtility.updateCounter(value, groupKey, frozenAssetCounter, frozenAssetValueCounterName);
	}

	void decreaseFrozenAssetValueCounter(String user, int value, String groupKey){
		System.out.printf("[P2PHandler][INFO] P2P Begin to remove user %s to frozen counter list of %s\n", user, groupKey);
		if (!frozenAssetCounter.containsKey(groupKey)) {
			System.out.printf("[P2PHandler][ERROR] Frozen counter list of %s does not exist for %s\n", groupKey, user);
			return;
		}
		P2PUtility.updateCounter(frozenAssetCounter.get(groupKey) - value, groupKey, frozenAssetCounter, frozenAssetValueCounterName);
	}

	void processConditionAndState(Transaction t, JGraphTWrapper jGraphTWrapper) {
		List<String> conditionSCList = foundItsConditionTransaction(t);
		List<String> stateSCList = foundItsStateTransaction(t);
		for (String c : conditionSCList) {
			System.out.printf("[P2PHandler][DEBUG] This P2P SC matches condition of another P2P SC\n");
			jGraphTWrapper.addEdge(c, t.getHash(), commonWeightForAllP2PSC);// edge from condition to state (as head)
			hasMatchedConditionMap.put(t.getHash(), t.getHash());
			hasMatchedStateMap.put(c, c);
		}

		for (String s : stateSCList) {
			System.out.printf("[P2PHandler][DEBUG] Another P2P SC's state match condition of this SC\n");
			jGraphTWrapper.addEdge(t.getHash(), s, commonWeightForAllP2PSC);// edge from condition to state (as head)
			hasMatchedStateMap.put(t.getHash(), t.getHash());
			hasMatchedConditionMap.put(s,s);
		}
	}

	private boolean check(Transaction t, String conditionForOther, String groupKey) {
		if (completedSCGroupMap.containsKey(groupKey)){
			System.out.printf("[P2PHandler][WARN] Cooperation has already been done\n");
			return false;
		}

		//2.a  Add into the graph
		if (!jGraphTWrapperMap.containsKey(groupKey)) {
			if (conditionForOther.isEmpty()){
				System.out.printf("[P2PHandler][ERROR] First transaction has no cooperation condition for others. Skip to process it further\n");
				return false;
			}
			System.out.printf("[P2PHandler][INFO] Begin to process the first transaction\n");
			jGraphTWrapperMap.put(groupKey, new JGraphTWrapper());
			proposalSCMap.put(groupKey, t.getHash());
		}
		return true;
	}

	@Nullable
	List<String> findPossibleLeadingSC(Transaction t, String groupKey, JGraphTWrapper jGraphTWrapper, boolean isAllType) {
		if (isAllType) { // all type
			List<String> currentOrItsBelongingProposalTxs = findPreviousProposalTxs(t.getHash(), jGraphTWrapper, new ArrayList<>());
			currentOrItsBelongingProposalTxs.add(t.getHash());

			List<String> checkTxList = new ArrayList<>();
			for (String k : currentOrItsBelongingProposalTxs) {
				// Judge whether it is a completed smart contract
				if (!isSCCompleted(hashTransactionMap.get(k))) continue;
				// only process complete sc
				checkTxList.add(k);
			}
			return currentOrItsBelongingProposalTxs;
		}

		// group or address only use proposal tx
		List<String> r = new ArrayList<>();
		String proposalTxs = proposalSCMap.get(groupKey);
		if (!isSCCompleted(hashTransactionMap.get(proposalTxs))) {
			System.out.println("[P2PHandler][Debug] proposal sc is ongoing.");
		} else {
			r.add(proposalTxs);
		}
		return r;
	}

	private void handleCompletionFlag(String groupKey, boolean setCompletionFlag,
																		JGraphTWrapper jGraphTWrapper, List<DefaultEdge> foundPath) {
		if (setCompletionFlag) {
			completedSCGroupMap.put(groupKey, groupKey);
		}
		String lastTxHash = "";
		for (DefaultEdge e : foundPath){
			Transaction t = hashTransactionMap.remove(jGraphTWrapper.getEdgeSource(e));
			decreaseOngoingCounter(t.getFrom(), groupKey);
			decreaseFrozenAssetValueCounter(t.getFrom(), getP2PSCStateAssetValue(t),groupKey);
			lastTxHash = jGraphTWrapper.getEdgeTraget(e);
		}
		if (hashTransactionMap.containsKey(lastTxHash)) {
			Transaction t = hashTransactionMap.remove(lastTxHash);
			decreaseOngoingCounter(t.getFrom(), groupKey);
			decreaseFrozenAssetValueCounter(t.getFrom(), getP2PSCStateAssetValue(t),groupKey);
		}
	}

	public void processCompletion(List<DefaultEdge> path, JGraphTWrapper jGraphTWrapper){
		Transaction lastStateTransaction = null;
		for (DefaultEdge edge : path) {
			String stateTxHash = jGraphTWrapper.getEdgeTraget(edge);
			Transaction stateTx = hashTransactionMap.get(stateTxHash);
			lastStateTransaction = stateTx;
			String conditionTxHash = jGraphTWrapper.getEdgeSource(edge);
			Transaction conditionTx = hashTransactionMap.get(conditionTxHash);
			String state = getP2PSCState(stateTx);
			String[] stateValues = state.split(valueSeparator);
			String stateType = stateValues[0];
			if (stateType.equals(p2pConditionTypeAddress)) { // address#<sender>
				// no just action before code execution
			} else if (stateType.equals(p2pConditionTypeAsset)) {// asset#lock#3
				String action = stateValues[1];
				if (action.equals(p2pLock)) {
					try {
						int value = Integer.parseInt(stateValues[2]);
						AccountManager.getInstance().addValue(conditionTx.getFrom(), value);// add to receiver
						System.out.printf("[P2PHandler][INFO] Transfer %d asset(s) to P2P smart contract owner %s\n", value, conditionTx.getFrom());
					} catch (NumberFormatException e) {
						System.out.printf("[P2PHandler][ERROR] P2P smart contract failed to add to receiver\n");
						e.printStackTrace();
					}
				} else {
					System.out.printf("[P2PHandler][ERROR] P2P smart contract state of asset %s is not supported in completion\n", action);
				}
			} else if (stateType.isEmpty()) {
				System.out.println("[P2PHandler][INFO] Proposal or internal state transaction is found  in completion");
			} else {
				System.out.printf("[P2PHandler][ERROR] P2P smart contract state '%s' is not supported in completion\n", stateType);
			}

			//run code
			InternalCodeRunner.run(getP2PSCCode(conditionTx));
		}//end for

		// Process to run code of last state
		if (!isCircle(path, jGraphTWrapper) && null != lastStateTransaction){
			InternalCodeRunner.run(getP2PSCCode(lastStateTransaction));
		}
	}

	boolean isCircle(List<DefaultEdge> edgeList, JGraphTWrapper jGraphTWrapper){
		if (edgeList.isEmpty()) return false;

		// A circle is first condition tx is last state tx
		String firstConditionTx = jGraphTWrapper.getEdgeSource(edgeList.get(0));
		String lastStateTx = jGraphTWrapper.getEdgeSource(edgeList.get(edgeList.size() -1));
		return firstConditionTx.equals(lastStateTx);
	}

	public String makeGroupKey(String p2pSCTypeId) {
		return P2PUtility.makeKey(p2pGroupMatchType, p2pSCTypeId);
	}

	public String makeAddressKey(String p2pSCTypeId) {
		return P2PUtility.makeKey(p2pAddressMatchType, p2pSCTypeId);
	}


	boolean isSCGroupOrAddressMatchCompleted(String groupKey){
		return completedSCGroupMap.containsKey(groupKey);
	}

	List<String> findPreviousProposalTxs(String txId,JGraphTWrapper jGraphTWrapper, List<String> checkedVertexList){
		List<String> r = new ArrayList<>();
		checkedVertexList.add(txId);

		Set<DefaultEdge> incomingEdges = jGraphTWrapper.getAllIncomingEdges(txId);
		if (incomingEdges.isEmpty()) {
			r.add(txId);
			return r;
		}

		for (DefaultEdge e : incomingEdges){
			String source = jGraphTWrapper.getEdgeSource(e);
			if (checkedVertexList.contains(source)) continue;

			List<String> re = findPreviousProposalTxs(source, jGraphTWrapper, checkedVertexList);
			if (!re.isEmpty()) r.addAll(re);
		}

		return r;
	}

	List<DefaultEdge> findCompletedAndTacticSelectedPath(CircleAndNonCirclePath path, JGraphTWrapper jGraphTWrapper){
		List<DefaultEdge> r = new ArrayList<>();
		int min = 0;
		List<List<DefaultEdge>> circlePathList = path.getCircledPathList();
		for (List<DefaultEdge> cp : circlePathList){
			if (!isPathCompleted(cp, jGraphTWrapper)) continue;
			int foundMin = getIndexFromCompletedCirclePath(cp, jGraphTWrapper);
			if (min < foundMin){
				min = foundMin;
				r = cp;
			}
		}

		List<List<DefaultEdge>> nonCirclePathList = path.getNonCircledPathList();
		for (List<DefaultEdge> ncp : nonCirclePathList){
			if (!isPathCompleted(ncp, jGraphTWrapper)) continue;
			int foundMin = getIndexFromCompletedNonCirclePath(ncp, jGraphTWrapper);
			if (min < foundMin){
				min = foundMin;
				r = ncp;
			}
		}

		return r;
	}

	boolean isPathCompleted(List<DefaultEdge> edgeList, JGraphTWrapper jGraphTWrapper){
		if (edgeList.isEmpty()) return false;

		//only last edge need to be checked as others has the edge(then has state)
		DefaultEdge lastEdge = edgeList.get(edgeList.size() -1);

		String lastTxHash = jGraphTWrapper.getEdgeTraget(lastEdge);
		if (getP2PSCCondition(hashTransactionMap.get(lastTxHash)).isEmpty()) return true;

		return hasMatchedStateMap.containsKey(lastTxHash);
	}

	int getIndexFromCompletedCirclePath(List<DefaultEdge> edgeList, JGraphTWrapper jGraphTWrapper){
		return getIndexFromCompletedPath(edgeList, jGraphTWrapper, true);
	}

	int getIndexFromCompletedNonCirclePath(List<DefaultEdge> edgeList,
																				 JGraphTWrapper jGraphTWrapper){
		if (edgeList.isEmpty())  return Integer.MAX_VALUE;

		Transaction t = hashTransactionMap.get(jGraphTWrapper.getEdgeSource(edgeList.get(0)));
		if (!isProposalTxForNonCircle(t)){
			System.out.printf("[P2PHandler][DEBUG] not a proposal tx for non circle path. Skip to process its further non circle validation\n");
			return Integer.MAX_VALUE;
		}

		return getIndexFromCompletedPath(edgeList, jGraphTWrapper, false);
	}

	int getIndexFromCompletedPath(List<DefaultEdge> edgeList,
																JGraphTWrapper jGraphTWrapper, boolean isCircle){
		if (0 == tacticSelection) return getFirstIndexFromCompletedPath(edgeList, jGraphTWrapper, isCircle);
		if (1 == tacticSelection) return getLastIndexFromCompletedPath(edgeList, jGraphTWrapper, isCircle);
		if (2 == tacticSelection) return getAverageIndexFromCompletedPath(edgeList, jGraphTWrapper, isCircle);
		return Integer.MAX_VALUE;
	}

	int getFirstIndexFromCompletedPath(List<DefaultEdge> edgeList, JGraphTWrapper jGraphTWrapper, boolean isCircle){
		int min = Integer.MAX_VALUE;
		DefaultEdge lastEdge = null;
		for (DefaultEdge e : edgeList){
			Transaction t = hashTransactionMap.get(jGraphTWrapper.getEdgeSource(e));
			// Judge complete
			if (!isSCCompleted(t)) return Integer.MIN_VALUE;
			// find min
			if (min > t.getUnifiedIndex()){
				min = t.getUnifiedIndex();
			}
			lastEdge = e;
		}
		if (min > hashTransactionMap.get(jGraphTWrapper.getEdgeSource(lastEdge)).getUnifiedIndex()){
			min = hashTransactionMap.get(jGraphTWrapper.getEdgeSource(lastEdge)).getUnifiedIndex();
		}
		return min;
	}

	int getLastIndexFromCompletedPath(List<DefaultEdge> edgeList, JGraphTWrapper jGraphTWrapper, boolean isCircle){
		int maxTxIndex = Integer.MAX_VALUE;
		DefaultEdge lastEdge = null;
		for (DefaultEdge e : edgeList){
			Transaction t = hashTransactionMap.get(jGraphTWrapper.getEdgeSource(e));
			// Judge complete
			if (!isSCCompleted(t)) return Integer.MIN_VALUE;
			// find min
			if (maxTxIndex == Integer.MAX_VALUE || maxTxIndex < t.getUnifiedIndex()){// Integer.MAX_VALUE is unchaged
				maxTxIndex = t.getUnifiedIndex();
			}
			lastEdge = e;
		}
		if (maxTxIndex < hashTransactionMap.get(jGraphTWrapper.getEdgeSource(lastEdge)).getUnifiedIndex()){
			maxTxIndex = hashTransactionMap.get(jGraphTWrapper.getEdgeSource(lastEdge)).getUnifiedIndex();
		}
		return maxTxIndex;
	}

	int getAverageIndexFromCompletedPath(List<DefaultEdge> edgeList, JGraphTWrapper jGraphTWrapper, boolean isCircle){
		int total = 0;
		DefaultEdge lastEdge = null;
		for (DefaultEdge e : edgeList){
			Transaction t = hashTransactionMap.get(jGraphTWrapper.getEdgeSource(e));
			// Judge complete
			if (!isSCCompleted(t)) return Integer.MIN_VALUE;
			// find min
			total += t.getUnifiedIndex();
			lastEdge = e;
		}

		if (!isCircle){
			// add last one
			total += hashTransactionMap.get(jGraphTWrapper.getEdgeSource(lastEdge)).getUnifiedIndex();
			return total/ (edgeList.size() + 1);
		}

		// circle
		return total/edgeList.size();
	}

	boolean processMinAveragePath(CircleAndNonCirclePath path){

		return true;
	}

	// Min max
	boolean processMinLastPath(CircleAndNonCirclePath path){

		return true;
	}


	boolean isSCCompleted(Transaction sc){
		boolean r =false;
		if (!getP2PSCState(sc).isEmpty()){// If a P2P SC has state, it should match another one's condition
			r = hasMatchedConditionMap.containsKey(sc.getHash());
			if (!r){
				return false;
			}
		}
		if (!getP2PSCCondition(sc).isEmpty()){// If a P2P SC has condition, it needs another one's state
			r = hasMatchedStateMap.containsKey(sc.getHash());
		}

		return r;
	}

	boolean isProposalTxForNonCircle(Transaction t){
		return !getP2PSCCondition(t).isEmpty() && getP2PSCState(t).isEmpty();
	}

	List<String> foundItsConditionTransaction(Transaction stateTx){
		List<String> r = new ArrayList();
		Set<String> keySet = hashTransactionMap.keySet();
		Transaction conditionTx;
		for (String k : keySet){
			conditionTx = hashTransactionMap.get(k);
			if (conditionTx.getId() == stateTx.getId()) continue;//same transaction
			if (doesStateMatchCondition(stateTx, conditionTx)){
				r.add(conditionTx.getHash());
			};
		}
		return r;
	}

	List<String> foundItsStateTransaction(Transaction conditionTx){
		List<String> r = new ArrayList();
		Set<String> keySet = hashTransactionMap.keySet();
		Transaction stateTx;
		for (String k : keySet){
			stateTx = hashTransactionMap.get(k);
			if (conditionTx.getId() == stateTx.getId()) continue;//same transaction
			if (doesStateMatchCondition(stateTx, conditionTx)){
				r.add(stateTx.getHash());
			};
		}
		return r;
	}

	// A state smart contract must set its state first
	boolean prepareState(Transaction stateTransaction){
		if (!isP2PSC(stateTransaction)) {
			System.out.printf("[P2PHandler][ERROR] Not P2P smart contract transactions to set state\n");
			return false;
		}
		String state = getP2PSCState(stateTransaction);
		String[] stateValues = state.split(valueSeparator);
		String stateType = stateValues[0];
		if (stateType.equals(p2pConditionTypeAddress)){ // address#<sender>
			// Nothing to prepare as the blockchain system will gve this information
			System.out.printf("[P2PHandler][INFO] No need to prepare for address state, as its a system attribute of the smart contract\n");
			return true;
		} else if (stateType.equals(p2pConditionTypeAsset)){// asset#lock#3
			String action = stateValues[1];
			if (action.equals(p2pLock)){
				try {
					int value = getP2PSCStateAssetValue(stateTransaction);
					if (!AccountManager.getInstance().freezeValue(stateTransaction.getFrom(), value)){
						System.out.printf("[P2PHandler][ERROR] Failed to freeze %s of %d\n", stateTransaction.getFrom(), value);
						return false;
					}
					System.out.printf("[P2PHandler][INFO] Succeed to freeze %s of %d\n", stateTransaction.getFrom(), value);
					return true;
				} catch (NumberFormatException e){
					e.printStackTrace();
					return false;
				}
			} else {
				System.out.printf("[P2PHandler][ERROR] P2P smart contract state of asset %s is not supported\n", action);
				return false;
			}
		} else if (stateType.isEmpty()){
			System.out.println("[P2PHandler][INFO] Proposal or internal state transaction is found");
			return true;
		}	else {
			System.out.printf("[P2PHandler][ERROR] P2P smart contract state '%s' is not supported\n", stateType);
			return false;
		}
	}

	boolean doesStateMatchCondition(Transaction stateTransaction, Transaction conditionTransaction){
		if (!isP2PSC(stateTransaction) || !isP2PSC(conditionTransaction)) {
			System.out.printf("[P2PHandler][ERROR] Not P2P smart contract transactions to match\n");
			return false;
		}
		String condition = getP2PSCCondition(conditionTransaction);
		if (condition.isEmpty()){
			System.out.printf("[P2PHandler][DEBUG] P2P smart contracts condition is empty. Skip to find its state SC\n");
			return false;
		}
		String state = getP2PSCState(stateTransaction);
		//two step check. 1. check declaration; 2. check real status from system
		if (!condition.equals(state)){
			System.out.printf("[P2PHandler][DEBUG] P2P smart contracts %s condition '%s' and state '%s' of %s does not match\n",
							conditionTransaction.getHash(), condition, state, stateTransaction.getHash());
			return false;
		}

		String[] conditionValues = condition.split(valueSeparator);
		String conditionType = getP2PSCConditionType(condition);//conditionValues[0];
		if (conditionType.equals("address")){ // address#<sender>
			String requiredSender = conditionValues[1];
			String realSender = AccountManager.getShortAddress(stateTransaction.getFrom());
			System.out.printf("[P2PHandler][DEBUG] Required sender %s  of %s %s real sender %s of %s \n",
							requiredSender, conditionTransaction.getHash(), (requiredSender.equals(realSender)) ? "does not match" : "matches"
							, realSender, stateTransaction.getHash());
			return requiredSender.equals(realSender);

		} else if (conditionType.equals(p2pConditionTypeAsset)){// asset#lock#3
			String action = conditionValues[1];
			if (action.equals(p2pLock)) {
				try {
					int value = Integer.parseInt(conditionValues[2]);
					int frozenValue = AccountManager.getInstance().getFreezeValue(stateTransaction.getFrom()).intValue();
					System.out.printf("[P2PHandler][DEBUG] Frozen asset of %s is %d, and declared value is %d\n",
									stateTransaction.getFrom(), frozenValue, value );
					return value == frozenValue;
				} catch (NumberFormatException e) {
					e.printStackTrace();
					return false;
				}
			}
		} else if (conditionType.isEmpty()){
		System.out.println("[P2PHandler][INFO] End transaction without condition for others is found");
		return true;
	}

		System.out.printf("[P2PHandler][ERROR] P2P smart contract condition '%s' is not supported\n", condition);
		return false;
	}

	String getP2PSCCondition(Transaction t){
		return t.getData().split(p2pHandlerSeparator)[3];
	}

	String getP2PSCTypeId(Transaction t){
		return t.getData().split(p2pHandlerSeparator)[2];
	}

	String getP2PSCConditionType(Transaction t){
		return getP2PSCConditionType(getP2PSCCondition(t));
	}

	String getP2PSCConditionType(String condition){
		return condition.split(valueSeparator)[0];
	}

	String getP2PSCState(Transaction t){
		return t.getData().split(p2pHandlerSeparator)[4];
	}

	int getP2PSCStateAssetValue(Transaction t){
		String state = getP2PSCState(t);
		String[] stateValues = state.split(valueSeparator);
		String stateType = stateValues[0];
		if (stateType.equals(p2pConditionTypeAsset)) {// asset#lock#3
			String action = stateValues[1];
			if (action.equals(p2pLock)) {
				try {
					return Integer.parseInt(stateValues[2]);
				} catch (NumberFormatException e) {
					e.printStackTrace();
					System.out.println("[P2PHandler][ERROR] " + e.getMessage());
					return 0;
				}
			}
		}
		System.out.println("[P2PHandler][ERROR] not asset frozen transaction and return 0 as default");
		return 0;
	}

	String getP2PSCStateType(Transaction t){
		return getP2PSCStateType(getP2PSCState(t));
	}

	String getP2PSCStateType(String condition){
		return condition.split(valueSeparator)[0];
	}

	String getP2PSCCode(Transaction t){
		return t.getData().split(p2pHandlerSeparator)[5];
	}

	// <p2pHandlerKeyword>:<p2pSCType>:<p2pSCTypeId>:<conditionForOther>:<state>:<scCode>
	boolean isP2PSC(Transaction t){
		String data = t.getData();
		if (null == data || !data.startsWith(p2pHandlerTxKeyword + p2pHandlerSeparator)) {
			System.out.println("[P2PHandler][DEBUG] Not a P2P smart contract transaction");
			return false;
		}

		String[] txParsedDataList = data.split(p2pHandlerSeparator);
		final int minp2pHandlerLength = 6;
		if (txParsedDataList.length < minp2pHandlerLength) {
			System.out.println("[P2PHandler][ERROR] Not enough parameter for the P2P smart contract Handler");
			return false;
		}
		// We also require sender must be in a transaction, as it is used during the P2P process
		// It should be the validation of a transaction, while as it is important for P2P process we check here
		// If think this is not OK, please revise this check
		if (t.getFrom().isEmpty()){
			System.out.println("[P2PHandler][ERROR] No sender of the transaction " + t.getId());
			return false;
		}
		return true;
	}

}
