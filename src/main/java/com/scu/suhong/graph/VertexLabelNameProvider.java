package com.scu.suhong.graph;

import org.jgrapht.io.ComponentNameProvider;

public class VertexLabelNameProvider <T> implements ComponentNameProvider<T> {
    public VertexLabelNameProvider() {
    }

    public String getName(T component) {
        return "__" + component.toString();
    }
}
