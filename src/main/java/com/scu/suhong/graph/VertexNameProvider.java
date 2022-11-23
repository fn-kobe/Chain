package com.scu.suhong.graph;

import org.jgrapht.io.ComponentNameProvider;

public class VertexNameProvider<T> implements ComponentNameProvider<T>{
        public VertexNameProvider() {
        }

        public String getName(T component) {
            return "_" + component.toString();
        }
}
