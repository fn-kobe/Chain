package com.scu.suhong.graph;

import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.*;
import org.jgrapht.io.*;

import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GraphUtility {
    private Graph<String, DefaultEdge> directedGraph;
    private Graph<String, DefaultEdge> directedSubGraph;

    public GraphUtility() {
        directedGraph = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);
        directedGraph.addVertex("a");
        directedGraph.addVertex("b");
        directedGraph.addVertex("c");
        directedGraph.addVertex("d");
        directedGraph.addVertex("e");
        directedGraph.addVertex("f");
        directedGraph.addVertex("h");
        directedGraph.addVertex("i");
        directedGraph.addEdge("a", "b");
        directedGraph.addEdge("b", "c");
        directedGraph.addEdge("c", "d");
        directedGraph.addEdge("d", "a");
        directedGraph.addEdge("c", "e");
        //directedGraph.addEdge("e", "a");
        directedGraph.addEdge("f", "h");
        directedGraph.addEdge("f", "i");
    }

    public void testStrongConn() {
        StrongConnectivityAlgorithm<String, DefaultEdge> scAlg =
                new KosarajuStrongConnectivityInspector<String, DefaultEdge>(directedGraph);
        List<Set<String>> stronglyConnetedSet =
                scAlg.stronglyConnectedSets();

        System.out.println("Strongly connected components:");
        for (int i = 0; i < stronglyConnetedSet.size(); i++) {
            System.out.println(stronglyConnetedSet.get(i));
        }
        System.out.println();

    }

    public void testWeakConn() {
        ConnectivityInspector<String, DefaultEdge> connectivityInspector = new ConnectivityInspector<String, DefaultEdge>(directedGraph);
        List<Set<String>> weaklyConnectedSet = connectivityInspector.connectedSets();
        System.out.println("Weakly connected components:");
        for (int i = 0; i < weaklyConnectedSet.size(); i++) {
            System.out.println(weaklyConnectedSet.get(i));
        }
        System.out.println();

    }

    public void subGraph() {
        Set<String> subNode = new HashSet<String>();
        subNode.add("a");
        subNode.add("d");
        subNode.add("c");
        subNode.add("f");
        directedSubGraph = new AsSubgraph(directedGraph, subNode, null);
        System.out.println(directedSubGraph.vertexSet());
        System.out.println(directedSubGraph.edgeSet());

    }

    public void export() throws ExportException {
        ComponentNameProvider<URL> vertexIdProvider =
                new ComponentNameProvider<URL>()
                {
                    public String getName(URL url)
                    {
                        return url.getHost().replace('.', '_');
                    }
                };
        ComponentNameProvider<URL> vertexLabelProvider =
                new ComponentNameProvider<URL>()
                {
                    public String getName(URL url)
                    {
                        return url.toString();
                    }
                };
        GraphExporter<URL, DefaultEdge> exporter = new DOTExporter<>(
                new StringComponentNameProvider(), new StringComponentNameProvider(), null);
        Writer writer = new StringWriter();
        exporter.exportGraph((DefaultDirectedGraph)directedGraph, writer);
        System.out.println(writer.toString());
    }


    public static void main(String args[]) {
        GraphUtility test = new GraphUtility();
//        test.testStrongConn();
//        test.testWeakConn();
//        test.subGraph();
        // use helper classes to define how vertices should be rendered,
        // adhering to the DOT language restrictions
        try {
            test.export();
        } catch (ExportException e) {
            e.printStackTrace();
        }
    }
}
