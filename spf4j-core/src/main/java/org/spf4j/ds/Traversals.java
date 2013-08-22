/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
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
 */
package org.spf4j.ds;

import com.google.common.collect.Sets;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
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
        Set<V> traversedNodes = new HashSet<V>();
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
    
    
    public static <V, E> void customTraverse(final Graph<V, E> graph, final V startNode,
            final TraversalCallback<V, E> handler) {
        Set<V> traversedNodes = new HashSet<V>();
        Queue<V> traversalQueue = new PriorityQueue<V>(16, new Comparator<V> () {
            @Override
            public int compare(final V o1, final V o2) {
                return graph.getEdges(o1).getIncomming().size()
                        - graph.getEdges(o2).getIncomming().size();
            }
        });
        traversalQueue.add(startNode);
        boolean done = false;
        do {
            boolean first = true;
            while (!traversalQueue.isEmpty()) {
                V node = traversalQueue.remove();
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

    
    
    
}
