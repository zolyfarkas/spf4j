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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.spf4j.stackmonitor.Method;


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

    /**
     * traversal implementation where traversal will go through only if all incoming edges of a node can be traversed.
     *
     * @param <V>
     * @param <E>
     * @param graph
     * @param startNode
     * @param handler
     */
    public static <V, E> void traverse(final Graph<V, E> graph, final V startNode,
            final TraversalCallback<V, E> handler) {

        Map<V, VertexEdges> visitationRecords = new HashMap<V, VertexEdges>();
        traverse(graph, startNode, null, null, handler, visitationRecords);
    }

    private static <V, E> void traverse(final Graph<V, E> graph, final V node, final E from, final V fromNode,
            final TraversalCallback<V, E> handler, final Map<V, VertexEdges> visitationRecords) {
                if (((Method) node).getMethodName().equals("flushBuffer")) {
                    System.out.println();
                }
        VertexEdges visitInfo = visitationRecords.get(node);
        if (visitInfo == null) {
            visitInfo = new VertexEdges();
            visitationRecords.put(node, visitInfo);
        }
        VertexEdges<V, E> edges = graph.getEdges(node);
        if (from != null) { // starting
            visitInfo.getIncomming().put(from, fromNode);
            Map<E, V> incoming = edges.getIncomming();
            if (visitInfo.getIncomming().keySet().containsAll(incoming.keySet())) {
                handler.handle(node, incoming);
                Set<E> edgeDiff = new HashSet<E>(
                        Sets.difference(edges.getOutgoing().keySet(), visitInfo.getOutgoing().keySet()));
                for (E edge : edgeDiff) {
                    V toNode = graph.getVertices(edge).getSecond();
                    
                    visitInfo.getOutgoing().put(edge, toNode);
                    traverse(graph, toNode, edge, node, handler, visitationRecords);
                }
            }
        } else {
            if (visitInfo.getIncomming().keySet().containsAll(edges.getIncomming().keySet())) {
                for (E edge : edges.getOutgoing().keySet()) {
                    V toNode = graph.getVertices(edge).getSecond();
                    visitInfo.getOutgoing().put(edge, toNode);
                    traverse(graph, toNode, edge, node, handler, visitationRecords);
                }
            } else {
                Set<E> intersect = new HashSet<E>(
                        Sets.intersection(edges.getOutgoing().keySet(), edges.getIncomming().keySet()));
                for (E edge : intersect) {
                    V toNode = graph.getVertices(edge).getSecond();
                    visitInfo.getOutgoing().put(edge, toNode);
                    traverse(graph, toNode, edge, node, handler, visitationRecords);
                }
            }
        }

    }
    
    public static <V, E> boolean isPathTo(final Graph<V, E> graph, final V node, final V ancestor,
            final Set<V> visited) {
        if (!graph.contains(node)) {
            return false;
        }
        if (node.equals(ancestor)) {
            return true;
        }
        if (visited.contains(node)) {
            return false;
        }
        Collection<V> values = graph.getEdges(node).getIncomming().values();
        if (values.contains(ancestor)) {
            return true;
        } else {
            visited.add(node);
            for (V anc : values) {
                if (isPathTo(graph, anc, ancestor, visited)) {
                    return true;
                }
            }
            return false;
        }
        
    }
    
    
}
