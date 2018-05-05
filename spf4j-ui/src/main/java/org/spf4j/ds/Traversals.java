/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.ds;

import com.google.common.collect.Sets;
import gnu.trove.set.hash.THashSet;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
 *
 * @author zoly
 */
public final class Traversals {

    private Traversals() {
    }

    public interface TraversalCallback<V, E> {

        void handle(V vertex, Map<E, V> edges);
    }

    public static <V, E> void traverse(final Graph<V, E> graph, final V startNode,
            final TraversalCallback<V, E> handler, final boolean isBreadth) {
        Set<V> traversedNodes = new THashSet<V>(graph.getVertices().size());
        Deque<V> traversalQueue = new ArrayDeque<V>();
        traversalQueue.add(startNode);
        boolean done = false;
        do {
            boolean first = true;
            while (!traversalQueue.isEmpty()) {
                V node;
                if (isBreadth) {
                    node = traversalQueue.removeFirst();
                } else {
                    node = traversalQueue.removeLast();
                }
                VertexEdges<V, E> edges = graph.getEdges(node);
                if (traversedNodes.contains(node)) {
                    continue;
                }
                Map<E, V> incomming = edges.getIncomming();
                boolean hasIncomingBeenTraversed = true;
                for (V val : incomming.values()) {
                    if (!traversedNodes.contains(val)) {
                        hasIncomingBeenTraversed = false;
                        break;
                    }
                }
                if (!first && !hasIncomingBeenTraversed) {
                    continue;
                }
                handler.handle(node, incomming);
                traversedNodes.add(node);
                first = false;
                Map<E, V> outgoing = edges.getOutgoing();
                for (V next : outgoing.values()) {
                    traversalQueue.add(next);
                }
            }

            Set<V> leftNodes = Sets.difference(graph.getVertices(), traversedNodes);
            if (leftNodes.isEmpty()) {
                done = true;
            } else {
                boolean added = false;
                for (V node : leftNodes) {
                    Collection<V> incomingNodes = graph.getEdges(node).getIncomming().values();
                    for (V incoming : incomingNodes) {
                        if (traversedNodes.contains(incoming)) {
                            traversalQueue.add(node);
                            added = true;
                            break;
                        }
                    }
                    if (added) {
                        break;
                    }
                }
            }
        } while (!done);
    }


    public static final class VertexHolder<V> implements Comparable<VertexHolder<V>> {

        private final V vertex;

        private final int order;

        private final int nrImcoming;

        public VertexHolder(final V vertex, final int order, final int nrImcoming) {
            this.vertex = vertex;
            this.order = order;
            this.nrImcoming = nrImcoming;
        }

        @Override
        public int compareTo(final VertexHolder<V> o) {
            int result = this.nrImcoming - o.nrImcoming;
            if (result == 0) {
                return  this.order - o.order;
            } else {
                return result;
            }
        }

        public V getVertex() {
            return vertex;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + (this.vertex != null ? this.vertex.hashCode() : 0);
            hash = 29 * hash + this.order;
            return 29 * hash + this.nrImcoming;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final VertexHolder<V> other = (VertexHolder<V>) obj;
            return (this.compareTo(other) == 0);
        }




    }

    /**
     * Custom graph traversal, starting from a particular node and following its outgoing links
     * with the child nodes being traversed in the order of least incoming links.
     * will need to properly document and test this...
     *
     * @param <V>
     * @param <E>
     * @param graph
     * @param startNode
     * @param handler
     */

    public static <V, E> void customTraverse(final Graph<V, E> graph, final V startNode,
            final TraversalCallback<V, E> handler) {
        Set<V> traversedNodes = new THashSet<>(graph.getVertices().size());
        Queue<VertexHolder<V>> traversalQueue = new PriorityQueue<VertexHolder<V>>(16);
        int counter = 0;
        traversalQueue.add(new VertexHolder<V>(startNode, counter++, 0));
        boolean done = false;
        do {
            boolean first = true;
            while (!traversalQueue.isEmpty()) {
                V node = traversalQueue.remove().getVertex();
                VertexEdges<V, E> edges = graph.getEdges(node);
                if (traversedNodes.contains(node)) {
                    continue;
                }
                Map<E, V> incomming = edges.getIncomming();
                boolean hasIncomingBeenTraversed = true;
                for (V val : incomming.values()) {
                    if (!traversedNodes.contains(val)) {
                        hasIncomingBeenTraversed = false;
                        break;
                    }
                }
                if (!first && !hasIncomingBeenTraversed) {
                    continue;
                }
                handler.handle(node, incomming);
                traversedNodes.add(node);
                first = false;
                Map<E, V> outgoing = edges.getOutgoing();
                for (V next : outgoing.values()) {
                    traversalQueue.add(
                            new VertexHolder<V>(next, counter++, graph.getEdges(next).getIncomming().size()));
                }
            }

            Set<V> leftNodes = Sets.difference(graph.getVertices(), traversedNodes);
            if (leftNodes.isEmpty()) {
                done = true;
            } else {
                boolean added = false;
                for (V node : leftNodes) {
                    Collection<V> incomingNodes = graph.getEdges(node).getIncomming().values();
                    for (V incoming : incomingNodes) {
                        if (traversedNodes.contains(incoming)) {
                            traversalQueue.add(new VertexHolder<V>(node, counter++, 0));
                            added = true;
                            break;
                        }
                    }
                    if (added) {
                        break;
                    }
                }
                if (!added) {
                  break;
                }
            }
        } while (!done);
    }




}
