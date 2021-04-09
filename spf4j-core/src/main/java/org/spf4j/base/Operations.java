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
package org.spf4j.base;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import org.apache.avro.io.Decoder;
import org.apache.avro.specific.SpecificDatumReader;
import org.spf4j.avro.AvroCompatUtils;
import org.spf4j.base.avro.Operation;

/**
 * @author Zoltan Farkas
 */
public final class Operations {

  private static final MutableGraph<String> OP_GRAPH;

  static {
    try {
      OP_GRAPH = getRegisteredOperations();
    } catch (IOException ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  private Operations() {
  }

  private static MutableGraph<String> getRegisteredOperations() throws IOException {
    MutableGraph<String> graph = GraphBuilder.directed().build();
    for (Enumeration<URL> e = ClassLoader.getSystemResources("operations.json"); e.hasMoreElements();) {
      URL url = e.nextElement();
      try (InputStream is = url.openStream()) {
        SpecificDatumReader<org.spf4j.base.avro.Operations> reader
                = new SpecificDatumReader<>(org.spf4j.base.avro.Operations.class);
        Decoder dec = AvroCompatUtils.getJsonDecoder(org.spf4j.base.avro.Operations.getClassSchema(), is);
        org.spf4j.base.avro.Operations ops = reader.read(null, dec);
        for (Operation op : ops.getOperations()) {
          addOperation(op, graph);
        }
      }
    }
    return graph;
  }

  private static void addOperation(final Operation op, final MutableGraph<String> graph) {
    String opName = op.getName();
    graph.addNode(opName);
    for (String pop : op.getPropagates()) {
      graph.addNode(pop);
      graph.putEdge(pop, opName);
    }
  }

  /**
   * Method that will return the provided operation and all its sub-operations recursively.
   * @param operationName
   * @return operation and all its sub-operations recursively
   */
  public static Set<String> getOperations(final String operationName) {
    Set<String> children = OP_GRAPH.predecessors(operationName);
    if (children.isEmpty()) {
      return Collections.singleton(operationName);
    } else {
      Set<String> response = new HashSet<>(8);
      response.add(operationName);
      response.addAll(children);
      ArrayDeque<String> queue = new ArrayDeque<>(4);
      queue.addAll(children);
      do {
        String op = queue.removeFirst();
        children = OP_GRAPH.predecessors(op);
        if (!children.isEmpty() && response.addAll(children)) {
            queue.addAll(children);
        }
      } while (!queue.isEmpty());
      return Collections.unmodifiableSet(response);
    }
  }

  @VisibleForTesting
  static void addOperation(final Operation op) {
    addOperation(op, OP_GRAPH);
  }

}
