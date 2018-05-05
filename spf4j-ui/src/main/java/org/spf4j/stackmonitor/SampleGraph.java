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
package org.spf4j.stackmonitor;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import gnu.trove.map.TMap;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Set;
import org.spf4j.base.Method;

/**
 *
 * @author Zoltan Farkas
 */
public class SampleGraph {

  public static final class SampleVertexKey {
    private final Method method;
    private int idxInHierarchy;
    public SampleVertexKey(Method method, int idxInHierarchy) {
      this.method = method;
      this.idxInHierarchy = idxInHierarchy;
    }

    @Override
    public int hashCode() {
      int hash = 59 + Objects.hashCode(this.method);
      return 59 * hash + this.idxInHierarchy;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final SampleVertexKey other = (SampleVertexKey) obj;
      if (this.idxInHierarchy != other.idxInHierarchy) {
        return false;
      }
      return Objects.equals(this.method, other.method);
    }

  }

  public static final class SampleVertex {

    private final SampleVertexKey key;
    private int nrSamples;

    public SampleVertex(SampleVertexKey key, int nrSamples) {
      this.key = key;
      this.nrSamples = nrSamples;
    }

    public SampleVertexKey getKey() {
      return key;
    }

    public int getNrSamples() {
      return nrSamples;
    }

    

  }


  private static final class Traversal {

    private final SampleVertex parent;
    private final Method method;
    private final SampleNode node;

    public Traversal(SampleVertex parent, Method method, SampleNode node) {
      this.parent = parent;
      this.method = method;
      this.node = node;
    }

  }

  private final SetMultimap<SampleVertexKey, SampleVertex> vertexMap;

  private final MutableGraph<SampleVertex> sg;

  private final SampleVertex rootVertex;


  public SampleGraph(final Method m, final SampleNode node) {
    vertexMap = MultimapBuilder.hashKeys().hashSetValues(1).build();
    int nrNodes = node.getNrNodes();
    sg = GraphBuilder.directed()
            .nodeOrder(ElementOrder.unordered())
            .expectedNodeCount(nrNodes)
            .build();
    rootVertex = tree2Graph(m, node);
  }

  public SampleGraph(SetMultimap<SampleVertexKey, SampleVertex> vertexMap,
          MutableGraph<SampleVertex> sg, SampleVertex rootVertex) {
    this.vertexMap = vertexMap;
    this.sg = sg;
    this.rootVertex = rootVertex;
  }



  private int computeMethodIdx(SampleVertex from,  Method m) {
    if (from.key.method.equals(m)) {
      return from.key.idxInHierarchy + 1;
    } else {
      Set<SampleVertex> predecessors = sg.predecessors(from);
      int size = predecessors.size();
      if (size == 1) {
        return computeMethodIdx(predecessors.iterator().next(), m);
      } else if (size > 1) {
        throw new IllegalStateException("Cannot have multiple predecesors for " + from + ", pred = " + predecessors);
      } else {
        return 0;
      }
    }
  }


  private SampleVertex tree2Graph(final Method m, final SampleNode node) {
    SampleVertex parentVertex = new SampleVertex(new SampleVertexKey(m, 0), node.getSampleCount());
    sg.addNode(parentVertex);
    vertexMap.put(parentVertex.key, parentVertex);
    Deque<Traversal> dq = new ArrayDeque<>();
    TMap<Method, SampleNode> subNodes = node.getSubNodes();
    if (subNodes != null) {
      subNodes.forEachEntry((k, v) -> {
        dq.add(new Traversal(parentVertex, k, v));
        return true;
      });
    }
    Traversal t;
    while ((t = dq.pollLast()) != null) {
      SampleVertex vtx = new SampleVertex(new SampleVertexKey(t.method, computeMethodIdx(t.parent, t.method)),
              t.node.getSampleCount());
      sg.addNode(vtx);
      sg.putEdge(t.parent, vtx);
      vertexMap.put(vtx.key, vtx);
      TMap<Method, SampleNode> subNodes2 = t.node.getSubNodes();
      if (subNodes2 != null) {
        subNodes2.forEachEntry((k, v) -> {
          dq.add(new Traversal(vtx, k, v));
          return true;
        });
      }
    }
    return parentVertex;
  }

  public SetMultimap<SampleVertexKey, SampleVertex> getVertexMap() {
    return vertexMap;
  }

  public MutableGraph<SampleVertex> getSg() {
    return sg;
  }

  public SampleVertex getRootVertex() {
    return rootVertex;
  }


}
