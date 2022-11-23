package com.scu.suhong.transaction;

import account.AccountManager;
import com.scu.suhong.block.Block;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.graph.JGraphTWrapper;
import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.io.ExportException;
import util.FileLogger;
import util.StringHelper;
import util.TimeHelper;

import java.io.File;
import java.time.Instant;
import java.util.*;

public class ConditionalAssociationTransactionHelper {
    static Logger logger = FileLogger.getLogger();

    static ConditionalAssociationTransactionHelper instance;
    Map<Integer, JGraphTWrapper> jGraphTWrapperMap = new HashMap<>();
    AccountManager accountManager = AccountManager.getInstance();
    int dumpCount = 0;
    Map<String, Long> edgeTimeStampMap = new HashMap();
    Map<Integer, ConditionalAssociationTransaction> processedTransactionList = new HashMap<>();
    private boolean testProhibitDump = false;

    private ConditionalAssociationTransactionHelper() {
    }

    public synchronized static ConditionalAssociationTransactionHelper getInstance() {
        if (null == instance){
            instance = new ConditionalAssociationTransactionHelper();
        }
        return instance;
    }

    public void reset(List<Block> blockList){
        jGraphTWrapperMap = new HashMap<>();
        dumpCount = 0;
        // We don't update processed transaction list as we don't want to process twice
        // As the processed time for loop separation will be much further now
        // To improve this if not OK. Like use block sealing time or reset mechanism!

        for (Block block : blockList) {
            tryAddNewBlock(block);
        }
    }

    public void resetGraphTWrapper(){
        jGraphTWrapperMap = new HashMap<>();
    }

    public void callbackCTx(ConditionalAssociationTransaction ctx) {
        // TO DO
        System.out.println("TO DO");
        assert false;
    }

    String makeEdgeKey(String s, String e){
        return s + "_" +e;
    }

    String makeEdgeKey(int interactionId, DefaultEdge edge){
        JGraphTWrapper jGraphTWrapper = getJGraphTWrapper(interactionId);
        String start = jGraphTWrapper.getEdgeSource(edge);
        String end = jGraphTWrapper.getEdgeTraget(edge);
        return makeEdgeKey(start, end);
    }

    JGraphTWrapper getJGraphTWrapper(int interactionId){
        JGraphTWrapper jGraphTWrapper = jGraphTWrapperMap.get(interactionId);
        if (null == jGraphTWrapper){
            jGraphTWrapper = new JGraphTWrapper();
            jGraphTWrapper.setInteractionId(interactionId);
            jGraphTWrapperMap.put(interactionId, jGraphTWrapper);
        }
        return jGraphTWrapper;
    }

    public void processCTx(ConditionalAssociationTransaction ctx) {
        if (processedTransactionList.containsKey(ctx.getId())){
            System.out.printf("[ConditionalAssociationTransactionHelper] Conditional transaction has been processed with id %s. Content: %s\n",
                    ctx.getId(), ctx.getJson().toString());
            return;
        }
        processedTransactionList.put(ctx.getId(), ctx);
        Condition c = ctx.getCondition();
        JGraphTWrapper jGraphTWrapper = getJGraphTWrapper(ctx.getInteractionId());

        Set<String> toList = ctx.getToList();
        for (String to : toList) {
            String start = c.getFrom(JGraphTWrapper.getReplaceSymbol());
            String end = getGraphFormat(to);
            jGraphTWrapper.addEdge(start, end, c.getValue(to));
            edgeTimeStampMap.put(makeEdgeKey(start, end), StringHelper.safeGetEpocFromString(ctx.getData()));
        }
        if (!tryFreezeAccount(c, ctx.getIncomingAssetType())){
            logger.error(String.format("[ConditionalAssociationTransactionHelper][WARN] %s doesn't have enough money", c.getFrom()));
            return;
        }
        logger.info(String.format("[ConditionalAssociationTransactionHelper][Debug] Balance of %s has been frozen with value %d", c.getFrom(), c.getValue()));

        // TO DO, add the timeout mechanism
        doGraphCalculation(ctx.getInteractionId());
    }

    public void doGraphCalculation(int interactionId){
        doGraphCalculation(interactionId, "");
    }
    public void doGraphCalculation(int interactionId, String msg){
        tryDumpDiagram(interactionId, msg);
        if (diagramAlgorithmType == DiagramAlgorithmType.E_Compute_All_Matched) computeAllMatched(interactionId);
        else if (diagramAlgorithmType == DiagramAlgorithmType.E_Compute_All_Small_Ring) computeAllSmallRing(interactionId);
        else {
            logger.error("[ConditionalAssociationTransactionHelper][ERROR] diagramAlgorithmType is not support: " + diagramAlgorithmType);
        }
    }

    public boolean planAssociationCTx(List<ConditionalAssociationTransaction> conditionalAssociationTransactionList){
        return planAssociationCTx(conditionalAssociationTransactionList, true);
    }
    public boolean planAssociationCTx(List<ConditionalAssociationTransaction> conditionalAssociationTransactionList, boolean dumpLast){
        JGraphTWrapper jGraphTWrapper = null;
        int interactionId = 0;
        for (ConditionalAssociationTransaction ctx : conditionalAssociationTransactionList){
            if (0 == interactionId) interactionId = ctx.getInteractionId();
            if (null == jGraphTWrapper) jGraphTWrapper = getJGraphTWrapper(interactionId);
            Condition c = ctx.getCondition();
            Set<String> toList = ctx.getToList();
            for (String to : toList) {
                jGraphTWrapper.addEdge(c.getFrom(), to, getW(ctx, to));
            }
        }
        tryDumpDiagram(interactionId);

        if (!dumpLast) return true;

        if (!jGraphTWrapper.isAllConnected()){
            System.out.println("[ConditionalAssociationTransactionHelper] The ctx can not be formed");
            return false;
        }

        boolean r = jGraphTWrapper.mergeDuplicatedEdges();
        tryDumpDiagram(interactionId);
        return r;
    }

    enum DiagramAlgorithmType{
        E_Compute_All_Matched, // used to finish an exchange when all associated transactions appear
        E_Compute_All_Small_Ring // use sub exchange mechanism to complete some transaction in a big exchange to shorten whole exchange time
    };

    DiagramAlgorithmType diagramAlgorithmType = DiagramAlgorithmType.E_Compute_All_Small_Ring;

    public void setDiagramAlgorithmType(DiagramAlgorithmType diagramAlgorithmType) {
        this.diagramAlgorithmType = diagramAlgorithmType;
    }

    public void setMultiEdgeShowOptions(int interactionId, JGraphTWrapper.MultiEdgeShowOptions multiEdgeShowOptions) {
        getJGraphTWrapper(interactionId).setMultiEdgeShowOptions(multiEdgeShowOptions);
    }

    private void computeAllMatched(int interactionId) {
        JGraphTWrapper jGraphTWrapper = getJGraphTWrapper(interactionId);
        List<Set<String>> connectedDiagrams = jGraphTWrapper.getWeaklyConnectedAlsoStronglyConnectedDiagrams();
        if (connectedDiagrams.isEmpty()) return;

        for (Set<String> d : connectedDiagrams) {
            if (jGraphTWrapper.isIncomingWeightMatchedOutComingWeight(d)) {
                logger.info("[ConditionalAssociationTransactionHelper] Conditional ring matched");
                addValueToAccount(interactionId, d);
                jGraphTWrapper.resetGraph();
            } // else weight is not matched, waiting for next
        }
    }

    void computeAllSmallRing(int interactionId) {
        System.out.printf("[ConditionalAssociationTransactionHelper][INFO] Begin to compute the small ring for exchange %d - using sub exchange\n", interactionId);
        int smallestWeight = 0;
        JGraphTWrapper jGraphTWrapper = getJGraphTWrapper(interactionId);
        List<Graph<String, DefaultEdge>> connectedGraphList = jGraphTWrapper.getStronglyConnectedGraph();
        for (Graph<String, DefaultEdge> graph : connectedGraphList){
            if (graph.edgeSet().isEmpty()) continue;
            dumpVertexes(graph.vertexSet());

            smallestWeight = findSmallestWeight(interactionId, graph.vertexSet());
            if (smallestWeight == Integer.MAX_VALUE || smallestWeight == 0){
                logger.info("[ConditionalAssociationTransactionHelper] Cannot find appropriate weight for small ring algorithm");
                return;
            }
            logger.debug("[ConditionalAssociationTransactionHelper] find smallest weight " + smallestWeight);

            Set<DefaultEdge> es = graph.edgeSet();
            for (DefaultEdge e : es){
                if (smallestWeight == jGraphTWrapper.getWeight(e)){
                    Long startTime = edgeTimeStampMap.get(makeEdgeKey(interactionId, e));
                    long doneTime = TimeHelper.getEpoch();
                    System.out.printf("[ConditionalAssociationTransactionHelper][CJE] %s %d from %d to %d\n",
                            makeEdgeKey(interactionId, e), doneTime - startTime,  startTime, doneTime);
                    jGraphTWrapper.addTotalRunTime(doneTime - startTime);

                    jGraphTWrapper.removeEdgeAndRemoveVertexIfEmpty(interactionId, e, edgeTimeStampMap.get(makeEdgeKey(interactionId, e)));
                } else {
                    jGraphTWrapper.subEdgeWeight(interactionId, e, smallestWeight, edgeTimeStampMap.get(makeEdgeKey(interactionId, e)));
                }
            }
            logger.info("[ConditionalAssociationTransactionHelper] Conditional small ring matched with weight " + smallestWeight);
            addValueToAccount(graph.vertexSet(), smallestWeight);
        }
        logger.debug("[ConditionalAssociationTransactionHelper] Finish to compute small ring");
    }

    void dumpVertexes(Set<String> edges){
        if (edges.isEmpty()) return;

        String r = "Vertexes: \n";
        for (String e : edges){
            r += e + ", ";
        }
        System.out.println(r);
    }

    private int findSmallestWeight(int interactionId, Set<String> vertexList) {
        JGraphTWrapper jGraphTWrapper = getJGraphTWrapper(interactionId);
        int smallestWeight = Integer.MAX_VALUE;
        for (String v : vertexList){
            int outWeight = jGraphTWrapper.getOutGoingEdgeWeight(v);
            int inWeight = jGraphTWrapper.getIncomingEdgeWeight(v);

            smallestWeight = outWeight < smallestWeight ? outWeight : smallestWeight;
            smallestWeight = inWeight < smallestWeight ? inWeight : smallestWeight;
        }
        return smallestWeight;
    }

    public void removeCTx(ConditionalAssociationTransaction ctx) {
        Condition c = ctx.getCondition();
        if (getJGraphTWrapper(ctx.getInteractionId()).removeEdge(ctx.getInteractionId(), c.getFrom(), c.getTo(), getW(ctx), edgeTimeStampMap.get(makeEdgeKey(c.getFrom(), c.getTo())))) {
            unFreezeAccount(c);
        }
    }

    public void tryDumpDiagram(int interactionId) {
        tryDumpDiagram(interactionId, "");
    }
    public void tryDumpDiagram(int interactionId, String msg) {
        if (testProhibitDump) return;
        logger.info("[ConditionalAssociationTransactionHelper] Try to dump the diagram");
        if (createDumpFolder()) getJGraphTWrapper(interactionId).export(getFileName(interactionId), msg);
    }

    String getDumpFolderName(){
        return "DiagramDump";
    }

    boolean createDumpFolder(){
        File file = new File(getDumpFolderName());
        if (!file.exists()) return file.mkdir();
        return true;
    }

    String getFileName(int interactionId){
        Calendar cal = Calendar.getInstance();
        cal.setTime(Date.from(Instant.now()));
        return getDumpFolderName() + File.separator + "D-" + interactionId + "_" + String.format("%1$tY-%1$tm-%1$td-%1$tk-%1$tM-%1$tS-", cal) + (++dumpCount) + ".gv";
    }

    public void tryAddNewBlock(Block block) {
        List<AbstractTransaction> transactionList = block.getTransactions();
        for (AbstractTransaction transaction : transactionList){
            // If want to support the normal transaction, please comments here
            if (transaction instanceof ConditionalAssociationTransaction) {
                processCTx((ConditionalAssociationTransaction) transaction);
            }
        }
    }

    public void removeBlock(Block block) {
        List<AbstractTransaction> transactionList = block.getTransactions();
        for (AbstractTransaction transaction : transactionList){
            // If want to support the normal transaction, please comments here
            if (transaction instanceof ConditionalAssociationTransaction) {
                removeCTx((ConditionalAssociationTransaction) transaction);
            }
        }
    }

    private void addValueToAccount(int interactionId, Set<String> d) {
        // as incoming weight is equal to out-going weight for the whole diagram
        // we just get incoming of the first one
        for (String v : d) {
            accountManager.addValue(getAddressFormat(v), getJGraphTWrapper(interactionId).getIncomingEdgeWeight(v));
        }
    }

    private void addValueToAccount(Set<String> d, int value) {
        // as incoming weight is equal to out-going weight for the whole diagram
        // we just get incoming of the first one
        for (String v : d) {
            accountManager.addValue(getAddressFormat(v), value);
        }
    }

    private boolean tryFreezeAccount(Condition c, String assetType) {
        if (!accountManager.canTransferValue(c.getFrom(), assetType,c.getValue())) {
            return false;
        }
        accountManager.subValue(c.getFrom(), assetType, Double.valueOf(c.getValue()));
        return true;
    }

    public void unFreezeAccount(int interactionId) {
        JGraphTWrapper jGraphTWrapper = getJGraphTWrapper(interactionId);
        tryDumpDiagram(interactionId, "Unfrozen");
        Set<DefaultEdge> edgeList = jGraphTWrapper.getAllEdges();
        for (DefaultEdge e : edgeList){
            accountManager.addValue(getAddressFormat(jGraphTWrapper.getEdgeSource(e)), jGraphTWrapper.getEdgeWeight(e));
        }
        jGraphTWrapper.resetGraph();
    }

    private boolean unFreezeAccount(Condition c) {
        accountManager.addValue(c.getFrom(), c.getValue());
        return true;
    }

    private int getW(ConditionalAssociationTransaction ctx) {
        int w = ctx.getCondition().getValue();
        if (ctx.isNormalTransaction) w = -w;
        return w;
    }

    private int getW(ConditionalAssociationTransaction ctx, String to) {
        int w = ctx.getCondition().getValue(to);
        if (ctx.isNormalTransaction) w = -w;
        return w;
    }

    public void exportGraph(int interactionId) {
        try {
            getJGraphTWrapper(interactionId).export();
        } catch (ExportException e) {
            e.printStackTrace();
        }
    }

    String getGraphFormat(String accountName){
        return JGraphTWrapper.getGraphFormat(accountName);

    }

    String getAddressFormat(String graphName){
        return JGraphTWrapper.getAddressFormat(graphName);
    }

    public void setTestProhibitDump() {
        this.testProhibitDump = true;
    }
}
