package com.scu.suhong.graph;

import account.AccountManager;
import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.io.DOTExporter;
import org.jgrapht.io.ExportException;
import org.jgrapht.io.GraphExporter;
import util.FileLogger;
import util.TimeHelper;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

public class JGraphTWrapper {
    static Logger logger = FileLogger.getLogger();

    private Graph<String, DefaultEdge> directedGraph;
    HashMap<String, List<Integer>> graphEdgeWeightList;
    static String replaceSymbol = "_";
    Long totalRunTime = 0l;
    int interactionId = 0;

    public void setInteractionId(int interactionId) {
        this.interactionId = interactionId;
    }

    public static String getReplaceSymbol() {
        return replaceSymbol;
    }

    public JGraphTWrapper() {
        resetGraph();
    }

    // @Return true, the node is newly added, false it is not added due to former existence
    public boolean tryAddVertex(String vertexName) {
        String replacedVertexName = getGraphFormat(vertexName);
        if (directedGraph.containsVertex(replacedVertexName)) return false;

        directedGraph.addVertex(replacedVertexName);
        return true;
    }

    public boolean removeVertex(Set<String> vertexs) {
        return directedGraph.removeAllVertices(vertexs);
    }

    public void addEdge(String start, String end, int weight) {
        String replacedStart = JGraphTWrapper.getGraphFormat(start);
        String replacedEnd = JGraphTWrapper.getGraphFormat(end);
        tryAddVertex(replacedStart);
        tryAddVertex(replacedEnd);

        processEdgesAdd(replacedStart, replacedEnd, weight);
    }

    public boolean isAllConnected() {
        List<Set<String>> connectedDiagram = getStronglyConnectedVertex();
        if (connectedDiagram.isEmpty() || connectedDiagram.size() != 1) return false;
        return directedGraph.vertexSet().size() == getStronglyConnectedVertex().get(0).size();
    }

    public boolean hasDuplicatedEdges() {
        return graphEdgeWeightList.size() > 1;
    }

    public boolean mergeDuplicatedEdges() {
        if (!hasDuplicatedEdges()) return false;
        Set<DefaultEdge> edgeSet = directedGraph.edgeSet();
        for (DefaultEdge edge : edgeSet) {
            if (isDuplicatedEdge(edge)) {
                // merge all weight into one weight
                List<Integer> oneWeighList = new ArrayList<>();
                oneWeighList.add(calculateWeightList(graphEdgeWeightList.get(getEdgeKey(edge))));
                graphEdgeWeightList.replace(getEdgeKey(edge),oneWeighList );
            }
        }
        return true;
    }

    int calculateWeightList(List<Integer> weightList) {
        int totalWeight = 0;
        for (Integer w : weightList) totalWeight += w;
        return totalWeight;
    }

    void logUsedTimeWhenDone(int interactionId, DefaultEdge edge, Long startEpoc){
        String start = directedGraph.getEdgeSource(edge);
        String end = directedGraph.getEdgeTarget(edge);
        long doneTime = TimeHelper.getEpoch();
        System.out.printf("[JGraphTWrapper][CJE][%d] %s:%s %d from %d to %d\n"
                , interactionId, start, end, (doneTime - startEpoc), startEpoc, doneTime);

        // This is the last time for this sender, we record it
        // Or it the sender sends out one tranaction with two receiver, it will be calculated twice
        // Aussuming one sender only send out one transaction(this transaction may send to several receivers)
        if (1 == directedGraph.degreeOf(start)) {
            totalRunTime += doneTime - startEpoc;
            System.out.printf("[JGraphTWrapper][CJE][%d] Total done time of sender %s is %d " +
                    "(Assuming one sender only send out one transaction(this transaction may send to several receivers))\n",
                    interactionId, start, totalRunTime);
        }

        Set<DefaultEdge> egdeSet = directedGraph.edgeSet();
        // This is done before edge remove, then if only one edge left, after remove there is no edge no.
        // We can record time for the whole graph here
        if (1 == egdeSet.size()){
            System.out.printf("[JGraphTWrapper][CJE][%d] *** Graph total done time is %d\n", interactionId, totalRunTime);
        }
    }

    public void addTotalRunTime(Long newDoneTime){
        totalRunTime += newDoneTime;
        System.out.printf("[JGraphTWrapper][CJE][%d] Add new done time %d\n", interactionId, newDoneTime);
        System.out.printf("[JGraphTWrapper][CJE][%d] Total done time is %d\n",interactionId, totalRunTime);
    }

    public boolean removeEdgeAndRemoveVertexIfEmpty(int interactionId, DefaultEdge edge, Long startEpoc) {
        String start = directedGraph.getEdgeSource(edge);
        String target = directedGraph.getEdgeTarget(edge);
        logUsedTimeWhenDone(interactionId, edge, startEpoc);
        boolean r = directedGraph.removeEdge(edge);
        if (0 == directedGraph.degreeOf(start)) r = directedGraph.removeVertex(start) ? r : false;
        if (0 == directedGraph.degreeOf(target)) r = directedGraph.removeVertex(target) ? r : false;
        System.out.printf("[JGraphTWrapper][CJE][%d] Graph left edges after deletion %d\n", interactionId, directedGraph.edgeSet().size());
        return r;
    }

    public boolean subEdgeWeight(int interactionId, DefaultEdge edge, int toBeSubWeight, Long startEpoc) {
        // we first search the same and remove
        List<Integer> edgeWeightList = graphEdgeWeightList.get(getEdgeKey(edge));
        int minWeightWhichIsBiggerThanToBeSubWeight = Integer.MAX_VALUE;
        for (Integer w : edgeWeightList){
            if (w == toBeSubWeight){
                System.out.printf("[JGraphTWrapper][Info][%d] All outgoing value of egde (%s:%s) has been used, remove it from exchange\n",
                        interactionId, directedGraph.getEdgeSource(edge), directedGraph.getEdgeTarget(edge));
                return removeWeightAndRemoveEdgeIfNoWeight(interactionId, edge, w, startEpoc);
            }
            if (w > toBeSubWeight && (minWeightWhichIsBiggerThanToBeSubWeight < w || minWeightWhichIsBiggerThanToBeSubWeight == Integer.MAX_VALUE)){
                minWeightWhichIsBiggerThanToBeSubWeight = w;
            }
        }

        if (minWeightWhichIsBiggerThanToBeSubWeight == Integer.MAX_VALUE){
            System.out.printf("[JGraphTWrapper][WARN][%d] No bigger or equal edge is found, skip sub edge weight\n", interactionId);
            return false;
        }
        System.out.printf("[JGraphTWrapper][Debug][%d] all weight before sub %s\n", interactionId, edgeWeightList.toString());
        edgeWeightList.remove((Integer) minWeightWhichIsBiggerThanToBeSubWeight);
        edgeWeightList.add(minWeightWhichIsBiggerThanToBeSubWeight - toBeSubWeight);
        System.out.printf("[JGraphTWrapper][Debug][%d] all weight after sub %s\n", interactionId, edgeWeightList.toString());
        graphEdgeWeightList.replace(getEdgeKey(edge), edgeWeightList);
        return true;
    }

    boolean removeWeightAndRemoveEdgeIfNoWeight(int interactionId, DefaultEdge e, int removeWeight, Long startEpoc){
        String key = getEdgeKey(e);
        List<Integer> edgeWeightList = graphEdgeWeightList.get(key);
        edgeWeightList.remove((Integer) removeWeight);

        if (edgeWeightList.isEmpty()){
            System.out.printf("[JGraphTWrapper][%d] weight list is empty now, try to remove edge\n", interactionId);
            graphEdgeWeightList.remove(key);
            return removeEdgeAndRemoveVertexIfEmpty(interactionId, e, startEpoc);
        }
        else {
            System.out.printf("[JGraphTWrapper][%d] weight list is not empty, try to shorten the weight list\n", interactionId);
            graphEdgeWeightList.replace(getEdgeKey(e), edgeWeightList);
        }

        return true;
    }

    public boolean removeEdge(int interactionId, String start, String end, int weight, Long startEpoc) {
        System.out.println("[JGraphTWrapper][" + interactionId + "] remove edge of " + start + " : " + end
                + " with weight " + weight);
        DefaultEdge edge = directedGraph.getEdge(start, end);
        if (null == edge) return false;

        List<Integer> edgeWeightList = graphEdgeWeightList.get(getEdgeKey(edge));
        for (Integer w : edgeWeightList){
            if (w == weight){
                return removeWeightAndRemoveEdgeIfNoWeight(interactionId, edge, w, startEpoc);
            }
        }
        return false;
    }

    // -n means normal transaction and + mean conditional transaction
    public List<Integer> getEdgeAllUnTypedWeights(String start, String end) {
        List<Integer> unTypedWeightList = new ArrayList<>();
        List<Integer> typedWeightList = getEdgeAllTypedWeights(start, end);

        for (Integer weight : typedWeightList) {
            if (weight < 0) unTypedWeightList.add(-weight);
            else unTypedWeightList.add(weight);
        }
        return unTypedWeightList;
    }

    public List<Integer> getEdgeAllTypedWeights(String start, String end) {
        List<Integer> weightList = new ArrayList<>();
        if (!directedGraph.containsEdge(start, end)) return weightList;

        if (graphEdgeWeightList.containsKey(makeEdgesKey(start, end))) {
            weightList.addAll(graphEdgeWeightList.get(makeEdgesKey(start, end)));
        }
        return weightList;
    }

    public void processEdgesAdd(String start, String end, int weight) {
        String key = makeEdgesKey(start, end);
        List<Integer> weightList;
        if (graphEdgeWeightList.containsKey(key)) {
            weightList = graphEdgeWeightList.get(key);
            weightList.add(weight);
            graphEdgeWeightList.replace(key, weightList);
            System.out.println("[JGraphTWrapper][" + interactionId + "] Add weight to existing edge for " + start + " : " + end
                    + " with weight count - " + weightList.size());
        } else {
            // add all weight into the edge weight list, then in the diagram it is set to 0
            DefaultEdge e = directedGraph.addEdge(start, end);
            directedGraph.setEdgeWeight(e,0);
            logger.debug("[JGraphTWrapper][" + interactionId + "] Add new edge of " + start + " : " + end);

            weightList = new ArrayList<>();
            weightList.add(weight);
            graphEdgeWeightList.put(key, weightList);
        }
    }

    String getEdgeKey(DefaultEdge e) {
        return makeEdgesKey(directedGraph.getEdgeSource(e), directedGraph.getEdgeTarget(e));
    }

    boolean isDuplicatedEdge(DefaultEdge e) {
        return graphEdgeWeightList.containsKey(getEdgeKey(e));
    }

    String makeEdgesKey(String start, String end) {
        return start + "_" + end;
    }

    public Graph<String, DefaultEdge> getDirectedGraph() {
        return directedGraph;
    }

    // If weekly connected also strongly connected, then this will make all CTx matched
    public List<Set<String>> getWeaklyConnectedAlsoStronglyConnectedDiagrams() {
        List<Set<String>> stronglyConnectedVertex = getStronglyConnectedVertex();
        List<Set<String>> weaklyConnectedVertex = getWeaklyConnectedVertex();
        List<Set<String>> result = new ArrayList<Set<String>>();
        for (Set<String> s : stronglyConnectedVertex) {
            for (Set<String> w : weaklyConnectedVertex) {
                if (isWeaklyConnectedAlsoStronglyConnected(s, w)) result.add(s);
            }
        }
        return result;
    }

    public boolean isIncomingWeightMatchedOutComingWeight(Set<String> vertexs) {
        for (String v : vertexs) {
            if (getOutGoingEdgeWeight(v) != getIncomingEdgeWeight(v)) {
                return false;
            }
        }
        return true;
    }

    // The action is to change the first edge's weight
    public void changeEdgeWeight(String start, String end, int weight) {
        String key = getEdgeKey(directedGraph.getEdge(start, end));
        List<Integer> weightList = graphEdgeWeightList.get(key);
        weightList.set(0, weight);
        graphEdgeWeightList.replace(key, weightList);
    }

    public int getOutGoingEdgeWeight(String vertex) {
        return getAllEdgeWeight(directedGraph.outgoingEdgesOf(vertex));
    }

    public int getIncomingEdgeWeight(String vertex) {
        return getAllEdgeWeight(directedGraph.incomingEdgesOf(vertex));
    }

    int getAllEdgeWeight(Set<DefaultEdge> edges) {
        int r = 0;
        for (DefaultEdge e : edges) {
            r += getEdgeWeight(e);
        }
        return r;
    }

    boolean isWeaklyConnectedAlsoStronglyConnected(Set<String> s, Set<String> w) {
        if (s.size() != w.size()) return false;

        boolean isMatched = false;
        for (String sv : s) {
            isMatched = false;
            for (String wv : w) {
                if (sv.equals(wv)) {
                    isMatched = true;
                    break;
                }
            }
            if (!isMatched) return false;
        }

        return true;
    }

    public List<Set<String>> getStronglyConnectedVertex() {
        StrongConnectivityAlgorithm<String, DefaultEdge> scAlg =
                new KosarajuStrongConnectivityInspector<String, DefaultEdge>(directedGraph);
        return scAlg.stronglyConnectedSets();
    }

    public boolean isStronglyConnected(){
        StrongConnectivityAlgorithm<String, DefaultEdge> scAlg =
                new KosarajuStrongConnectivityInspector<String, DefaultEdge>(directedGraph);
        return scAlg.isStronglyConnected();
    }

    public List<Graph<String, DefaultEdge>> getStronglyConnectedGraph() {
        StrongConnectivityAlgorithm<String, DefaultEdge> scAlg =
                new KosarajuStrongConnectivityInspector<String, DefaultEdge>(directedGraph);
        return scAlg.getStronglyConnectedComponents();
    }

    public List<Set<String>> getWeaklyConnectedVertex() {
        ConnectivityInspector<String, DefaultEdge> connectivityInspector = new ConnectivityInspector<String, DefaultEdge>(directedGraph);
        return connectivityInspector.connectedSets();
    }

    public int getWeight(DefaultEdge edge) {
        return (int) directedGraph.getEdgeWeight(edge);
    }


    public boolean isWeaklyConnect(String v1, String v2) {
        List<Set<String>> weaklyConnectedVertex = getWeaklyConnectedVertex();
        for (Set<String> gv : weaklyConnectedVertex) {
            if (gv.contains(v1) && gv.contains(v2)) return true;
        }
        return false;
    }

    public boolean isDiagramEmpty() {
        return directedGraph.vertexSet().isEmpty();
    }

    public Set<DefaultEdge> getAllEdges() {
        return directedGraph.edgeSet();
    }

    public Set<DefaultEdge> getAllIncomingEdges(String n) {
        if (directedGraph.containsVertex(n)) {
            return directedGraph.incomingEdgesOf(n);
        }
        return new HashSet<>();
    }

    public Set<DefaultEdge> getAllOutgoingEdges(String n) {
        if (directedGraph.containsVertex(n)) {
            return directedGraph.outgoingEdgesOf(n);
        }
        return new HashSet<>();
    }

    public String getEdgeSource(DefaultEdge e) {
        return directedGraph.getEdgeSource(e);
    }

    public String getEdgeTraget(DefaultEdge e) {
        return directedGraph.getEdgeTarget(e);
    }

    public int getEdgeWeight(DefaultEdge e){
        if (graphEdgeWeightList.containsKey(getEdgeKey(e))){
            return calculateWeightList(graphEdgeWeightList.get(getEdgeKey(e)));
        }
        return 0;
    }

    public CircleAndNonCirclePath getSuccessivePath(List<String> nodeList){
        System.out.println("[JGraphTWrapper][" + interactionId + "] Begin to get all path for node " + nodeList.toString() );

        CircleAndNonCirclePath resultPath = new CircleAndNonCirclePath();
        for (String start : nodeList) {
            getSuccessivePath(start, null, new ArrayList<>(), resultPath);
        }
        return resultPath;
    }

    public CircleAndNonCirclePath getSuccessivePath(String start){
        System.out.println("[JGraphTWrapper][" + interactionId + "] Begin to get all path for node " + start );

        CircleAndNonCirclePath resultPath = new CircleAndNonCirclePath();
        getSuccessivePath(start, null, new ArrayList<>(), resultPath);
        return resultPath;
    }

    String dumpPath(List<DefaultEdge> path){
        String r = "";
        for (DefaultEdge e : path) {
            r += directedGraph.getEdgeSource(e) + "->";
            r += directedGraph.getEdgeTarget(e) + " : ";
        }
        return r;
    }

    public void getSuccessivePath(String start, DefaultEdge currentEdge,
                                  List<DefaultEdge> formerPath, CircleAndNonCirclePath resultPath){
        boolean isCircledFormed = false;
        if (doesPathContainNode(start, formerPath)){// a circle
            isCircledFormed = true;
        }

        if (null != currentEdge) formerPath.add(currentEdge); // add the edge (start as end), to save new array each time in later
        if (isCircledFormed) {
            resultPath.addCircledPath(formerPath);
            System.out.println("[JGraphTWrapper][" + interactionId + "] A circle format by node " + start + " with former edges " + dumpPath(formerPath));
            return;
        }

        Set<DefaultEdge> outgoingEdges = getAllOutgoingEdges(start);
        if (outgoingEdges.isEmpty()){//leaf node
            if (null == currentEdge) {// root is leaf
                System.out.printf("[JGraphTWrapper][%d] Out going edge is empty and no former edge. Single P2P smart contract now \n", interactionId);
                return;// just empty, as we want at least two smart contract to interact
            } else { // at least two smart contracts interact here
                resultPath.addNonCircledPath(formerPath);
                System.out.printf("[JGraphTWrapper][%d] Outgoing edge is empty and it is a leaf. One non circle path found %s\n",
                        interactionId, dumpPath(formerPath));
                return;
            }
        }

        // non leaf node
        for (DefaultEdge e : outgoingEdges){
            List<DefaultEdge> newFormerPath = new ArrayList<>();// Makes the change former list not affect parent
            newFormerPath.addAll(formerPath);
            getSuccessivePath(getEdgeTraget(e), e, newFormerPath, resultPath);
        }
    }

    public boolean doesPathContainNode(String n, List<DefaultEdge> edgeList){
        for (DefaultEdge e : edgeList){
            if (directedGraph.getEdgeSource(e).equals(n) || directedGraph.getEdgeTarget(e).equals(n)){
                return true;
            }
        }
        return false;
    }

    public void resetGraph() {
        directedGraph = new DefaultDirectedWeightedGraph<String, DefaultEdge>(DefaultEdge.class);
        graphEdgeWeightList = new HashMap<>();
    }

    public enum MultiEdgeShowOptions{
        E_Show_Each,
        E_Show_Sum
    };

    MultiEdgeShowOptions multiEdgeShowOptions = MultiEdgeShowOptions.E_Show_Each;

    public void setMultiEdgeShowOptions(MultiEdgeShowOptions multiEdgeShowOptions) {
        this.multiEdgeShowOptions = multiEdgeShowOptions;
    }

    public String export() throws ExportException {
        return export("");
    }

    public String export(String msg) throws ExportException {
        GraphExporter<String, DefaultEdge> exporter = new DOTExporter(
                new VertexNameProvider(), new VertexLabelNameProvider(), null);
        ((DOTExporter<String, DefaultEdge>) exporter).setEdgeIDProvider(new EdgeNameProvider<>());
        Writer writer = new StringWriter();
        exporter.exportGraph((DefaultDirectedGraph) directedGraph, writer);

        String r = writer.toString();
        r = removeOriginalWeightEdge(r);
        r = handleEdgeWeightLabel(r);
        r = addDigraphMsg(r, msg);
        System.out.println(r);
        return r;
    }

    String addDigraphMsg(String original, String newMsg){
        if (null == newMsg || newMsg.isEmpty()) return original;

        String r = original;
        r = r.replaceAll("}", "label=\"" + newMsg + "\"\n}");
        return r;
    }

    private String removeOriginalWeightEdge(String original){
        String[] lines = original.split(System.getProperty("line.separator"));
        for(int i=0;i<lines.length;i++){
            if(lines[i].contains("->") && !lines[i].contains("label")){ // original lines don't have label chars
                lines[i]="";
            }
        }

        StringBuilder finalStringBuilder= new StringBuilder("");
        for(String s:lines){
            if(!s.equals("")){
                finalStringBuilder.append(s).append(System.getProperty("line.separator"));
            }
        }
        return finalStringBuilder.toString();
    }

    private String handleEdgeWeightLabel(String original) {
        String r = original;
        Set<DefaultEdge> edgeSet = directedGraph.edgeSet();
        boolean isReplaced = false;
        for (DefaultEdge e : edgeSet) {
            if (!isDuplicatedEdge(e)) continue;

            List<Integer> weightList = (graphEdgeWeightList.get(getEdgeKey(e)));
            if (multiEdgeShowOptions == MultiEdgeShowOptions.E_Show_Each) {
                for (Integer weight : weightList) {
                    r = r.replaceAll("}", "  _" + directedGraph.getEdgeSource(e) + " -> _" + directedGraph.getEdgeTarget(e)
                            + " [ label= \"" + weight + "\" ];\n}");
                }
                isReplaced = true;
            } else if (multiEdgeShowOptions == MultiEdgeShowOptions.E_Show_Sum){
                r = r.replaceAll("}", "  _" + directedGraph.getEdgeSource(e) + " -> _" + directedGraph.getEdgeTarget(e)
                        + " [ label= \"" + calculateWeightList(weightList) + "\" ];\n}");
            }
        }
        // strict will not allow the duplicated edgs
        if (isReplaced) r = r.replaceAll("strict ", "");
        return r;
    }

    public void export(String fileName, String msg) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(fileName);
            out.println(export(msg));
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ExportException e) {
            e.printStackTrace();
            out.close();
        }
    }

    static public String getGraphFormat(String accountName){
        return accountName.replace(AccountManager.getAddressConnectSymbol(), JGraphTWrapper.getReplaceSymbol());

    }

    static public String getAddressFormat(String graphName){
        return graphName.replace(JGraphTWrapper.getReplaceSymbol(), AccountManager.getAddressConnectSymbol());
    }
}
