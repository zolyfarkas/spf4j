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

import com.google.common.annotations.Beta;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import gnu.trove.map.TMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.spf4j.base.Method;

/**
 *
 * @author Zoltan Farkas
 */
@Beta
public final class SampleGraph {

  public static final class SampleKey {
    private final Method method;
    private int idxInHierarchy;
    public SampleKey(final Method method, final int idxInHierarchy) {
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
      final SampleKey other = (SampleKey) obj;
      if (this.idxInHierarchy != other.idxInHierarchy) {
        return false;
      }
      return Objects.equals(this.method, other.method);
    }

    public Method getMethod() {
      return method;
    }

    @Override
    public String toString() {
      return "SampleKey{" + "method=" + method + ", idxInHierarchy=" + idxInHierarchy + '}';
    }

  }

  @SuppressWarnings("checkstyle:VisibilityModifier")
  public static class Sample {

    private final SampleKey key;
    protected int nrSamples;
    protected int level;

    public Sample(final SampleKey key, final int nrSamples, final int level) {
      this.key = key;
      this.nrSamples = nrSamples;
      this.level = level;
    }

    public final SampleKey getKey() {
      return key;
    }

    public final int getNrSamples() {
      return nrSamples;
    }

    public final int getLevel() {
      return level;
    }

    /**
     * string for debug..
     */
    @Override
    public String toString() {
      return "Sample{" + "key=" + key + ", nrSamples=" + nrSamples + ", level=" + level + '}';
    }



  }

  public static final class AggSample extends Sample {

    public AggSample(final Sample sample) {
      super(sample.key, sample.nrSamples, sample.level);
    }

    public void add(final Sample sample) {
     this.nrSamples += sample.nrSamples;
     this.level = Math.max(sample.level, level);
    }

    @Override
    public String toString() {
      return "AggSample{" + "key=" + getKey() + ", nrSamples=" + nrSamples + ", level=" + level + '}';
    }

  }

  private static final class TraversalData {

    private final Sample parent;
    private final Method method;
    private final SampleNode node;

    TraversalData(final Sample parent, final Method method, final SampleNode node) {
      this.parent = parent;
      this.method = method;
      this.node = node;
    }

  }

  /**
   * index of Method -> Sample information.
   */
  private final SetMultimap<SampleKey, Sample> vertexMap;

  /**
   * index of Method -> Agregated sample information.
   */
  private final Map<SampleKey, AggSample> aggregates;

  /**
   * A graph representation of the stack trace tree.
   */
  private final MutableGraph<Sample> sg;

  /**
   * An aggreagated representation of the stack trace tree;
   */
  private final MutableGraph<AggSample> aggGraph;

  /**
   * The root vertex.
   */
  private final Sample rootVertex;


  public SampleGraph(final Method m, final SampleNode node) {
    int nrNodes = node.getNrNodes();
    vertexMap = MultimapBuilder.hashKeys(nrNodes).hashSetValues(1).build();
    aggregates = new THashMap<>(nrNodes);
    sg = GraphBuilder.directed()
            .nodeOrder(ElementOrder.unordered())
            .expectedNodeCount(nrNodes)
            .build();
    aggGraph = GraphBuilder.directed()
            .nodeOrder(ElementOrder.unordered())
            .expectedNodeCount(nrNodes)
            .build();
    rootVertex = tree2Graph(m, node);
  }




  /**
   * Compute a duplication occurrence from root index for a method.
   * (number of occurrences of this method on a stack path.
   * @param from
   * @param m
   * @return
   */
  private int computeMethodIdx(final Sample from, final Method m) {
    if (from.key.method.equals(m)) {
      return from.key.idxInHierarchy + 1;
    } else {
      Set<Sample> predecessors = sg.predecessors(from);
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


  private Sample tree2Graph(final Method m, final SampleNode node) {
    Sample parentVertex = new Sample(new SampleKey(m, 0), node.getSampleCount(), 0);
    if (!sg.addNode(parentVertex)) {
      throw new IllegalStateException();
    }
    if (!vertexMap.put(parentVertex.key, parentVertex)) {
      throw new IllegalStateException();
    }
    AggSample aggSampleVertex = new AggSample(parentVertex);
    aggGraph.addNode(aggSampleVertex);
    aggregates.put(parentVertex.key, aggSampleVertex);
    Deque<TraversalData> dq = new ArrayDeque<>();
    TMap<Method, SampleNode> subNodes = node.getSubNodes();
    if (subNodes != null) {
      subNodes.forEachEntry((k, v) -> {
        dq.add(new TraversalData(parentVertex, k, v));
        return true;
      });
    }
    TraversalData t;
    while ((t = dq.pollLast()) != null) {
      SampleKey vtxk = new SampleKey(t.method, computeMethodIdx(t.parent, t.method));
      Sample vtx = new Sample(vtxk, t.node.getSampleCount(), t.parent.level + 1);
      if (!sg.addNode(vtx)) {
        throw new IllegalStateException();
      }
      if (!vertexMap.put(vtx.key, vtx)) {
        throw new IllegalStateException();
      }
      AggSample aggParent = aggregates.get(t.parent.key);
      AggSample current = aggregates.get(vtxk);
      if (current == null) {
        current = new AggSample(vtx);
        aggGraph.addNode(current);
        aggregates.put(vtxk, current);
      } else {
        current.add(vtx);
      }
      aggGraph.putEdge(aggParent, current);
      if (!sg.putEdge(t.parent, vtx)) {
        throw new IllegalStateException();
      }
      TMap<Method, SampleNode> subNodes2 = t.node.getSubNodes();
      if (subNodes2 != null) {
        subNodes2.forEachEntry((k, v) -> {
          dq.add(new TraversalData(vtx, k, v));
          return true;
        });
      }
    }
    return parentVertex;
  }

  public Sample getRootVertex() {
    return rootVertex;
  }

  public AggSample getAggRootVertex() {
    return aggregates.get(rootVertex.key);
  }

  public int getAggNodesNr() {
    return this.aggregates.size();
  }

  public boolean haveCommonChild(final AggSample a, final AggSample b) {
    if (a.getKey().equals(b.getKey())) {
      return true;
    }
    Set<SampleKey> traversed = new THashSet<>();
    ArrayDeque<AggSample> trq = new ArrayDeque<>();
    trq.add(a);
    trq.add(b);
    AggSample curr;
    while ((curr = trq.pollFirst()) != null) {
      if (!traversed.add(curr.getKey())) {
        return true;
      }
      aggGraph.successors(curr).forEach((n) -> traversed.add(n.getKey()));
    }
    return false;
  }

  public Set<AggSample> getParents(final AggSample node) {
    return aggGraph.predecessors(node);
  }

  public Set<AggSample> getChildren(final AggSample node) {
    return aggGraph.successors(node);
  }

  public AggSample getAggNode(final SampleKey key) {
    return this.aggregates.get(key);
  }


  @Override
  public String toString() {
    return "SampleGraph{" + "vertexMap=" + vertexMap + ", sg=" + sg + ", rootVertex=" + rootVertex + '}';
  }



}
