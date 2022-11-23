package com.scu.suhong.graph;

import org.jgrapht.graph.DefaultEdge;

import java.util.ArrayList;
import java.util.List;

// Currently for (1) Directed graph, if non-circled, please consider revision
// 								(2) One path is either a circle a single path without circle, not the mixed
public class CircleAndNonCirclePath {
	public CircleAndNonCirclePath() {
		circledPathList = new ArrayList<>();
		nonCircledPathList = new ArrayList<>();
	}

	List<List<DefaultEdge>> circledPathList;
	List<List<DefaultEdge>> nonCircledPathList;

	public void addCircledPath(List<DefaultEdge> path){
		circledPathList.add(path);
	}

	public void addNonCircledPath(List<DefaultEdge> path){
		nonCircledPathList.add(path);
	}

	public List<List<DefaultEdge>> getCircledPathList() {
		return circledPathList;
	}

	public List<List<DefaultEdge>> getNonCircledPathList() {
		return nonCircledPathList;
	}

	// In class assuming, all nodes are the same and then circle are the same
	boolean areTwoCirclePathSame(List<DefaultEdge> path1, List<DefaultEdge> path2){
		for (DefaultEdge e : path1){
			if (!path2.contains(e)) return false;
		}
		return true;
	}

	// We assume that a path will go to leaf
	boolean areTwoNonCirclePathSame(List<DefaultEdge> path1, List<DefaultEdge> path2){
		if (path1.size() != path2.size()) return false;

		// all shorter path should be in longer path, they are in the same path
		for (DefaultEdge e : path1){
			if (!path2.contains(e)) return false;
		}

		return true;
	}

	// We assume that a path will go to leaf
	boolean DoesNonCirclePathContain(List<DefaultEdge> longerPath, List<DefaultEdge> shorterPath){
		assert longerPath.size() >= shorterPath.size();

		// all shorter path should be in longer path, they are in the same path
		for (DefaultEdge e : shorterPath){
			if (!longerPath.contains(e)) return false;
		}

		return true;
	}
}
