/*
 * Computoser is a music-composition algorithm and a website to present the results
 * Copyright (C) 2012-2014  Bozhidar Bozhanov
 *
 * Computoser is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Computoser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Computoser.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.music.util;

import java.util.HashSet;
import java.util.Set;

public class Graph<T> {
    private Set<Node<T>> nodes = new HashSet<Node<T>>();
    public void addNode(Node<T> node) {
        nodes.add(node);
    }

    public static class Node<T> {
        public final T value;
        public final Set<Edge<T>> inEdges;
        public final Set<Edge<T>> outEdges;

        public Node(T value) {
            this.value = value;
            inEdges = new HashSet<Edge<T>>();
            outEdges = new HashSet<Edge<T>>();
        }

        public Node<T> addEdge(Node<T> node) {
            Edge<T> e = new Edge<T>(this, node);
            outEdges.add(e);
            node.inEdges.add(e);
            return this;
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    private static class Edge<T> {
        public final Node<T> from;
        public final Node<T> to;

        public Edge(Node<T> from, Node<T> to) {
            this.from = from;
            this.to = to;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(Object obj) {
            Edge<T> e = (Edge<T>) obj;
            return e.from == from && e.to == to;
        }
    }

}