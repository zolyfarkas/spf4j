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
package org.spf4j.reflect;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.reflect.TypeToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.spf4j.ds.Graphs;

/**
 * @author Zoltan Farkas
 */
public final class GraphTypeMap<H> implements TypeMap<H> {

  private final MutableGraph<TypeToken> typeGraph;

  private final Map<TypeToken, H> handlers;

  public GraphTypeMap() {
    this(16);
  }

  public GraphTypeMap(final int expectedSize) {
    typeGraph = GraphBuilder.directed().allowsSelfLoops(false)
            .expectedNodeCount(expectedSize).build();
    handlers = new HashMap<>(expectedSize);
  }

  @Override
  public Set<H> getAll(final Type t) {
    Set<H> result = new HashSet<>(1);
    TypeToken tt = TypeToken.of(t);
    MutableGraph<TypeToken> traverseGraph = Graphs.clone(typeGraph);
    Set<TypeToken> nodes = traverseGraph.nodes();
    List<TypeToken> nodesToRemove = new ArrayList<>();
    do {
      for (TypeToken token : nodes) {
        if (traverseGraph.inDegree(token) == 0) {
          nodesToRemove.add(token);
          if (tt.isSubtypeOf(token)) {
            result.add(handlers.get(token));
          }
        }
      }
      for (TypeToken token : nodesToRemove) {
        if (!traverseGraph.removeNode(token)) {
          throw new IllegalStateException("Cannot remove " + token + " from " + traverseGraph);
        }
      }
      nodesToRemove.clear();
      nodes = traverseGraph.nodes();
    } while (result.isEmpty() && !nodes.isEmpty());
    return result;
  }


  @Override
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
  public boolean putIfNotPresent(final Type type, final H appender) {
    TypeToken<?> nType = TypeToken.of(type);
    if (typeGraph.addNode(nType)) {
      handlers.put(nType, appender);
      for (TypeToken t : typeGraph.nodes()) {
        if (!nType.equals(t)) {
          if (nType.isSubtypeOf(t)) {
            typeGraph.putEdge(nType, t);
          } else if (t.isSubtypeOf(nType)) {
            typeGraph.putEdge(t, nType);
          }
        }
      }
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean remove(final Type type) {
    TypeToken<?> tt = TypeToken.of(type);
    if (typeGraph.removeNode(tt)) {
      handlers.remove(tt);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return "GraphTypeMap{" + "typeGraph=" + typeGraph + ", handlers=" + handlers + '}';
  }

  @Override
  public H getExact(final Type t) {
    return handlers.get(TypeToken.of(t));
  }

}
