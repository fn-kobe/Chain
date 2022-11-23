package com.scu.suhong.graph;

import org.jgrapht.io.ComponentNameProvider;

public class EdgeNameProvider<T> implements ComponentNameProvider<T> {
    public EdgeNameProvider() {
    }

    public String getName(T component) {
        return "___" + component.toString();
    }
}
