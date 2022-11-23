package com.scu.suhong.graph;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.io.ExportException;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JGraphTWrapperTest {
    int interactionId = 111;

    @Test
    public void testAddEdge() {
        JGraphTWrapper jGraphTWrapper = new JGraphTWrapper();
        assert jGraphTWrapper.tryAddVertex("a");
        assert jGraphTWrapper.tryAddVertex("b");
        assert !jGraphTWrapper.tryAddVertex("b");
        assert jGraphTWrapper.tryAddVertex("d");

        String testEdgeSource = "a";
        String testEdgeTarget = "b";
        jGraphTWrapper.addEdge(testEdgeSource, testEdgeTarget, -11);

        Set<DefaultEdge> egs = jGraphTWrapper.getDirectedGraph().getAllEdges(testEdgeSource, testEdgeTarget);
        assert egs.size() == 1;
        for (DefaultEdge e : egs) {
            System.out.println(jGraphTWrapper.getDirectedGraph().getEdgeWeight(e));
        }
    }

    @Test
    public void testprocessDuplicatedEdges() {
        JGraphTWrapper jGraphTWrapper = new JGraphTWrapper();
        jGraphTWrapper.tryAddVertex("a");
        jGraphTWrapper.tryAddVertex("b");
        jGraphTWrapper.tryAddVertex("c");
        jGraphTWrapper.tryAddVertex("d");

        jGraphTWrapper.addEdge("a", "b", 1);
        jGraphTWrapper.addEdge("a", "b", -1);
        List<Integer> allTypedWeight = jGraphTWrapper.getEdgeAllTypedWeights("a", "b");
        assert allTypedWeight.size() == 2;
        jGraphTWrapper.addEdge("a", "b", -1);
        jGraphTWrapper.addEdge("a", "b", 5);
        allTypedWeight = jGraphTWrapper.getEdgeAllTypedWeights("a", "b");
        assert allTypedWeight.size() == 4;
        assert allTypedWeight.get(0) == 1;
        assert allTypedWeight.get(3) == 5;
        assert allTypedWeight.get(2) == -1;

        List<Integer> allUnTypedWeight = jGraphTWrapper.getEdgeAllUnTypedWeights("a", "b");
        assert allUnTypedWeight.size() == 4;
        assert allUnTypedWeight.get(0) == 1;
        assert allUnTypedWeight.get(1) == 1;
        assert allUnTypedWeight.get(2) == 1;

    }

    @Test
    public void testIsWeaklyConnectedAlsoStronglyConnected() {
        JGraphTWrapper jGraphTWrapper = new JGraphTWrapper();
        Set<String> s = new HashSet<>();
        Set<String> w = new HashSet<>();
        assert jGraphTWrapper.isWeaklyConnectedAlsoStronglyConnected(s, w);

        jGraphTWrapper.tryAddVertex("a");
        jGraphTWrapper.tryAddVertex("b");
        jGraphTWrapper.tryAddVertex("c");
        jGraphTWrapper.tryAddVertex("d");
        jGraphTWrapper.tryAddVertex("e");

        jGraphTWrapper.addEdge("a", "b", 1);
        jGraphTWrapper.addEdge("b", "c", 1);
        jGraphTWrapper.addEdge("c", "d", 1);
        jGraphTWrapper.addEdge("d", "a", 1);
        jGraphTWrapper.addEdge("c", "e", 1);
        s = jGraphTWrapper.getStronglyConnectedVertex().get(0);
        w = jGraphTWrapper.getWeaklyConnectedVertex().get(0);
        assert !jGraphTWrapper.isWeaklyConnectedAlsoStronglyConnected(s, w);
        List<Set<String>> connectedDiagram = jGraphTWrapper.getWeaklyConnectedAlsoStronglyConnectedDiagrams();
        assert connectedDiagram.isEmpty();

        // e <--> a
        jGraphTWrapper.addEdge("e", "a", 1);
        s = jGraphTWrapper.getStronglyConnectedVertex().get(0);
        w = jGraphTWrapper.getWeaklyConnectedVertex().get(0);
        assert jGraphTWrapper.isWeaklyConnectedAlsoStronglyConnected(s, w);
        connectedDiagram = jGraphTWrapper.getWeaklyConnectedAlsoStronglyConnectedDiagrams();
        assert connectedDiagram.size() == 1;
        assert connectedDiagram.get(0).size() == 5;

        // make x->y
        jGraphTWrapper.tryAddVertex("x");
        jGraphTWrapper.tryAddVertex("y");
        jGraphTWrapper.addEdge("x", "y", 1);
        connectedDiagram = jGraphTWrapper.getWeaklyConnectedAlsoStronglyConnectedDiagrams();
        assert connectedDiagram.size() == 1;
        assert connectedDiagram.get(0).size() == 5;

        // make x y bi-connected
        jGraphTWrapper.addEdge("y", "x", 1);
        connectedDiagram = jGraphTWrapper.getWeaklyConnectedAlsoStronglyConnectedDiagrams();
        assert connectedDiagram.size() == 2;
        assert connectedDiagram.get(0).size() == 2;

        // remove the first
        assert jGraphTWrapper.removeVertex(connectedDiagram.get(1));
        connectedDiagram = jGraphTWrapper.getWeaklyConnectedAlsoStronglyConnectedDiagrams();
        assert connectedDiagram.size() == 1;
        assert connectedDiagram.get(0).size() == 2;

        // remove the last connected
        assert jGraphTWrapper.removeVertex(connectedDiagram.get(0));
        connectedDiagram = jGraphTWrapper.getWeaklyConnectedAlsoStronglyConnectedDiagrams();
        assert connectedDiagram.size() == 0;
        assert connectedDiagram.isEmpty();
    }

    @Test
    public void testIsWeightMatched() throws ExportException {
        JGraphTWrapper jGraphTWrapper = new JGraphTWrapper();
        Set<String> s = new HashSet<>();
        Set<String> w = new HashSet<>();
        assert jGraphTWrapper.isWeaklyConnectedAlsoStronglyConnected(s, w);

        jGraphTWrapper.tryAddVertex("a");
        jGraphTWrapper.tryAddVertex("b");
        jGraphTWrapper.tryAddVertex("c");
        jGraphTWrapper.tryAddVertex("d");
        jGraphTWrapper.tryAddVertex("e");

        jGraphTWrapper.addEdge("a", "b", 2);
        jGraphTWrapper.addEdge("b", "c", 1);
        jGraphTWrapper.addEdge("c", "d", 1);
        jGraphTWrapper.addEdge("d", "a", 1);
        jGraphTWrapper.addEdge("c", "e", 1);
        jGraphTWrapper.addEdge("e", "a", 1);
        s = jGraphTWrapper.getStronglyConnectedVertex().get(0);
        w = jGraphTWrapper.getWeaklyConnectedVertex().get(0);
        assert jGraphTWrapper.isWeaklyConnectedAlsoStronglyConnected(s, w);
        List<Set<String>> connectedDiagram = jGraphTWrapper.getWeaklyConnectedAlsoStronglyConnectedDiagrams();
        assert !jGraphTWrapper.isIncomingWeightMatchedOutComingWeight(connectedDiagram.get(0));

        jGraphTWrapper.changeEdgeWeight("b", "c", 2);
        assert jGraphTWrapper.isIncomingWeightMatchedOutComingWeight(connectedDiagram.get(0));

        jGraphTWrapper.changeEdgeWeight("b", "c", 1);
        assert !jGraphTWrapper.isIncomingWeightMatchedOutComingWeight(connectedDiagram.get(0));

        jGraphTWrapper.addEdge("b", "c", 1);
        assert jGraphTWrapper.isIncomingWeightMatchedOutComingWeight(connectedDiagram.get(0));

        jGraphTWrapper.changeEdgeWeight("b", "c", 0);
        assert !jGraphTWrapper.isIncomingWeightMatchedOutComingWeight(connectedDiagram.get(0));

//        jGraphTWrapper.addEdge("b", "c", -1);
//        assert jGraphTWrapper.isIncomingWeightMatchedOutComingWeight(connectedDiagram.get(0));

        jGraphTWrapper.export();
    }

    @Test
    public void testExport() {
        JGraphTWrapper jGraphTWrapper = new JGraphTWrapper();
        jGraphTWrapper.export("testDiagramExport", "");
    }

    @Test
    public void testRemoveEdge() {
        JGraphTWrapper jGraphTWrapper = new JGraphTWrapper();
        String s1 = "s1";
        String t1 = "t1";
        int w1 = 1;
        jGraphTWrapper.addEdge(s1, t1, w1);
        assert jGraphTWrapper.removeEdge(interactionId, s1, t1, w1, 0l);

        jGraphTWrapper.addEdge(s1, t1, w1);
        jGraphTWrapper.addEdge(s1, t1, w1);
        assert jGraphTWrapper.removeEdge(interactionId, s1, t1, w1, 0l);
        assert jGraphTWrapper.removeEdge(interactionId, s1, t1, w1, 0l);
        assert !jGraphTWrapper.removeEdge(interactionId, s1, t1, w1, 0l);

        String s2 = "s2";
        String t2 = "t2";
        int w2 = 2;
        jGraphTWrapper.addEdge(s2, t2, w1);
        jGraphTWrapper.addEdge(s1, t1, w1);
        jGraphTWrapper.addEdge(s2, t2, w2);
        jGraphTWrapper.export("testDiagramExport", "");
        assert jGraphTWrapper.removeEdge(interactionId, s2, t2, w1, 0l);
        assert jGraphTWrapper.removeEdge(interactionId, s1, t1, w1, 0l);
        assert !jGraphTWrapper.removeEdge(interactionId, s2, t2, w1, 0l);
        assert jGraphTWrapper.removeEdge(interactionId, s2, t2, w2, 0l);
        assert !jGraphTWrapper.removeEdge(interactionId, s2, t2, w2, 0l);
    }

    @Test
    public void testGetWeaklyConnectedVertex() {
        JGraphTWrapper jGraphTWrapper = new JGraphTWrapper();
        jGraphTWrapper.tryAddVertex("v1");
        assert jGraphTWrapper.getWeaklyConnectedVertex().get(0).contains("v1");
    }

    @Test
    public void testIsWeaklyConnect() {
        String v1 = "v1";
        String v2 = "v2";
        String v3 = "v3";
        String v4 = "v4";
        JGraphTWrapper jGraphTWrapper = new JGraphTWrapper();
        jGraphTWrapper.tryAddVertex(v1);
        assert jGraphTWrapper.isWeaklyConnect(v1, v1);

        jGraphTWrapper.tryAddVertex(v2);
        assert !jGraphTWrapper.isWeaklyConnect(v1, v2);

        jGraphTWrapper.tryAddVertex(v3);
        jGraphTWrapper.tryAddVertex(v4);
        jGraphTWrapper.addEdge(v1, v2, 2);
        assert jGraphTWrapper.isWeaklyConnect(v1, v2);
        jGraphTWrapper.addEdge(v3, v4, 2);
        assert !jGraphTWrapper.isWeaklyConnect(v1, v3);
    }

    @Test
    public void testgetNonCirclePath() throws ExportException {
        String v1 = "v1";
        String v2 = "v2";
        String v3 = "v3";
        String v4 = "v4";
        String v5 = "v5";
        String v6 = "v6";
        JGraphTWrapper jGraphTWrapper = new JGraphTWrapper();
        jGraphTWrapper.tryAddVertex(v1);
        jGraphTWrapper.tryAddVertex(v2);
        jGraphTWrapper.tryAddVertex(v3);
        jGraphTWrapper.tryAddVertex(v4);

        jGraphTWrapper.addEdge(v1, v2, 2);
        jGraphTWrapper.addEdge(v2, v3, 2);
        jGraphTWrapper.addEdge(v3, v4, 2);

        System.out.printf("\n[Test] *** Begin to first test a non circle \n");
        CircleAndNonCirclePath path = jGraphTWrapper.getSuccessivePath(v1);
        assert 1 == path.getNonCircledPathList().size();
        assert path.getCircledPathList().isEmpty();

        System.out.printf("\n[Test] **** Begin to add more edges to form a circle \n");
        jGraphTWrapper.addEdge(v4,v1,2);
        path = jGraphTWrapper.getSuccessivePath(v1);
        assert 1 == path.getCircledPathList().size();
        assert path.getNonCircledPathList().isEmpty();

        System.out.printf("\n[Test] **** Begin to test one circle and one path \n");
        jGraphTWrapper.addEdge(v1,v5,2);
        jGraphTWrapper.addEdge(v5,v6,2);
        path = jGraphTWrapper.getSuccessivePath(v1);
        assert 1 == path.getCircledPathList().size();
				//System.out.printf("[TEST] ******* the dump path is %s \n", jGraphTWrapper.dumpPath(path.getCircledPathList().get(0)));
        assert 1 == path.getNonCircledPathList().size();
        
        System.out.printf("\n[Test] **** Begin to test two circles \n");
        jGraphTWrapper.addEdge(v6,v1,2);
        path = jGraphTWrapper.getSuccessivePath(v1);
        assert 2 == path.getCircledPathList().size();
        assert 0 == path.getNonCircledPathList().size();
    }

    @Test
    public void testAddressGraphAccount() {
        String accountAddress = "111?12345";
        String grpahAddress = "111_12345";

        assert accountAddress.equals(JGraphTWrapper.getAddressFormat(grpahAddress));
        assert grpahAddress.equals(JGraphTWrapper.getGraphFormat(accountAddress));
    }
}