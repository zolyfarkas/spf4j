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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.spf4j.base.Pair;

/**
 *
 * @author zoly
 */
public final class HashMapGraph<V, E> implements Graph<V, E> {

    private Map<E, Pair<V, V>> edgeNodes;
    private Map<V, VertexEdges> vertices;

    public HashMapGraph() {
        edgeNodes = new HashMap<E, Pair<V, V>>();
        vertices = new HashMap<V, VertexEdges>();
    }

    private HashMapGraph(final Map<E, Pair<V, V>> edgeNodes,
            final Map<V, VertexEdges> vertices) {
        this.edgeNodes = edgeNodes;
        this.vertices = vertices;
    }

    public void add(final V vertex) {
        if (!vertices.containsKey(vertex)) {
            vertices.put(vertex, null);
        }
    }

    public void add(final E edge, final V fromVertex, final V toVertex) {
        edgeNodes.put(edge, new Pair<V, V>(fromVertex, toVertex));
        VertexEdges fromV = vertices.get(fromVertex);
        if (fromV == null) {
            fromV = new VertexEdges();
            vertices.put(fromVertex, fromV);
        }
        fromV.getOutgoing().put(edge, toVertex);

        VertexEdges toV = vertices.get(toVertex);
        if (toV == null) {
            toV = new VertexEdges();
            vertices.put(toVertex, toV);
        }
        toV.getIncomming().put(edge, fromVertex);
    }

    @Override
    public Pair<V, V> getVertices(final E edge) {
        return edgeNodes.get(edge);
    }

    @Override
    public VertexEdges<V, E> getEdges(final V vertice) {
        return vertices.get(vertice);
    }

    @Override
    public Set<V> getVertices() {
        return vertices.keySet();
    }

    @Override
    public void remove(final V vertice) {
        VertexEdges<V, E> remove = vertices.remove(vertice);
        if (remove == null) {
            return;
        }
        for (E edge : remove.getIncomming().keySet()) {
            V fromVertex = edgeNodes.remove(edge).getFirst();
            vertices.get(fromVertex).getOutgoing().remove(edge);

        }
        for (E edge : remove.getOutgoing().keySet()) {
            V toVertex = edgeNodes.remove(edge).getSecond();
            vertices.get(toVertex).getIncomming().remove(edge);
        }
    }

    @Override
    public String toString() {
        return "HashMapGraph{" + "edgeNodes=" + edgeNodes + ", vertices=" + vertices + '}';
    }

    @Override
    public Graph<V, E> copy() {
        HashMap<V, VertexEdges> hashMap = new HashMap<V, VertexEdges>(vertices);
        for (Map.Entry<V, VertexEdges> entry : hashMap.entrySet()) {
            V v = entry.getKey();
            VertexEdges vertexEdges = entry.getValue();
            hashMap.put(v, vertexEdges.copy());
        }
        return new HashMapGraph<V, E>(new HashMap<E, Pair<V, V>>(edgeNodes),
                hashMap);
    }

    @Override
    public boolean contains(final V vertice) {
        return vertices.containsKey(vertice);
    }

    @Override
    @Nullable
    public E getEdge(final V from, final V to) {
        VertexEdges<V, E> edges = vertices.get(from);
        if (edges != null) {
            Map<E, V> outgoing = edges.getOutgoing();
            for (Map.Entry<E, V> entry : outgoing.entrySet()) {
                if (entry.getValue().equals(to)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }
}
