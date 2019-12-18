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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.spf4j.base.StackSamples;
import gnu.trove.map.TMap;
import gnu.trove.procedure.TObjectObjectProcedure;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.WillNotClose;
import org.spf4j.base.Json;
import org.spf4j.base.Methods;
import org.spf4j.base.MutableHolder;
import org.spf4j.base.Pair;
import org.spf4j.base.avro.Method;

/**
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class SampleNode implements Serializable, StackSamples {

  private static final long serialVersionUID = 1L;

  private int sampleCount;
  private TMap<Method, SampleNode> subNodes;

  SampleNode(final StackTraceElement[] stackTrace, final int from) {
    sampleCount = 1;
    if (from >= 0) {
      subNodes = new MethodMap<>();
      subNodes.put(Methods.getMethod(stackTrace[from]), new SampleNode(stackTrace, from - 1));
    }
  }

  public static SampleNode createSampleNode(final StackTraceElement... stackTrace) {
    SampleNode result = new SampleNode(1, null);
    SampleNode prevResult = result;
    for (int i = stackTrace.length - 1; i >= 0; i--) {
      StackTraceElement elem = stackTrace[i];
      if (prevResult.subNodes == null) {
        prevResult.subNodes = new MethodMap<>();
      }
      SampleNode node = new SampleNode(1, null);
      prevResult.subNodes.put(Methods.getMethod(elem), node);
      prevResult = node;
    }
    return result;
  }

  public static void addToSampleNode(final SampleNode node, final StackTraceElement... stackTrace) {
    SampleNode prevResult = node;
    prevResult.sampleCount++;
    for (int i = stackTrace.length - 1; i >= 0; i--) {
      StackTraceElement elem = stackTrace[i];
      final Method method = Methods.getMethod(elem);
      SampleNode nNode;
      if (prevResult.subNodes == null) {
        prevResult.subNodes = new MethodMap<>();
        nNode = new SampleNode(1, null);
        prevResult.subNodes.put(method, nNode);
      } else {
        nNode = prevResult.subNodes.get(method);
        if (nNode != null) {
          nNode.sampleCount++;
        } else {
          nNode = new SampleNode(1, null);
          prevResult.subNodes.put(method, nNode);
        }
      }
      prevResult = nNode;
    }
  }

  public static SampleNode clone(final SampleNode node) {
    if (node.subNodes == null) {
      return new SampleNode(node.sampleCount, null);
    }
    final TMap<Method, SampleNode> newSubNodes = new MethodMap<>(node.subNodes.size());
    node.subNodes.forEachEntry((final Method a, final SampleNode b) -> {
      newSubNodes.put(a, SampleNode.clone(b));
      return true;
    });
    return new SampleNode(node.sampleCount, newSubNodes);
  }

  @Nullable
  public static SampleNode aggregateNullable(@Nullable final SampleNode node1, @Nullable final SampleNode node2) {
    if (node1 == null) {
      if (node2 == null) {
        return null;
      } else {
        return node2;
      }
    } else if (node2 == null) {
      return node1;
    }  else {
      return aggregate(node1, node2);
    }
  }

  public static SampleNode aggregate(final SampleNode node1, final SampleNode node2) {
    int newSampleCount = node1.sampleCount + node2.sampleCount;
    TMap<Method, SampleNode> newSubNodes;
    if (node1.subNodes == null) {
      if (node2.subNodes == null) {
        newSubNodes = null;
      } else {
        newSubNodes = cloneSubNodes(node2);
      }
    } else if (node2.subNodes == null) {
      newSubNodes = cloneSubNodes(node1);
    } else {
      final TMap<Method, SampleNode> ns = new MethodMap<>(Math.max(node1.subNodes.size(), node2.subNodes.size()) + 1);
      node1.subNodes.forEachEntry((final Method m, final SampleNode b) -> {
        SampleNode other = node2.subNodes.get(m);
        if (other == null) {
          ns.put(m, SampleNode.clone(b));
        } else {
          ns.put(m, aggregate(b, other));
        }
        return true;
      });
      node2.subNodes.forEachEntry((final Method m, final SampleNode b) -> {
        if (!node1.subNodes.containsKey(m)) {
          ns.put(m, SampleNode.clone(b));
        }
        return true;
      });
      newSubNodes = ns;
    }
    return new SampleNode(newSampleCount, newSubNodes);
  }

  /**
   * Aggregation implementation where parts of node1 and node2 will be re-used.
   * @param node1
   * @param node2
   * @return
   */
  @Nullable
  public static SampleNode aggregateNullableUnsafe(@Nullable final SampleNode node1,
          @Nullable final SampleNode node2) {
    if (node1 == null) {
      if (node2 == null) {
        return null;
      } else {
        return node2;
      }
    } else if (node2 == null) {
      return node1;
    }  else {
      return aggregateUnsafe(node1, node2);
    }
  }


  /**
   * Aggregation implementation where parts of node1 and node2 will be re-used.
   * @param node1
   * @param node2
   * @return
   */
 public static SampleNode aggregateUnsafe(final SampleNode node1, final SampleNode node2) {
    int newSampleCount = node1.sampleCount + node2.sampleCount;
    TMap<Method, SampleNode> newSubNodes;
    if (node1.subNodes == null) {
      if (node2.subNodes == null) {
        newSubNodes = null;
      } else {
        newSubNodes = node2.subNodes;
      }
    } else if (node2.subNodes == null) {
      newSubNodes = node1.subNodes;
    } else {
      final TMap<Method, SampleNode> ns = new MethodMap<>(Math.max(node1.subNodes.size(), node2.subNodes.size()) + 1);
      node1.subNodes.forEachEntry((final Method m, final SampleNode b) -> {
        SampleNode other = node2.subNodes.get(m);
        if (other == null) {
          ns.put(m, b);
        } else {
          ns.put(m, aggregateUnsafe(b, other));
        }
        return true;
      });
      node2.subNodes.forEachEntry((final Method m, final SampleNode b) -> {
        if (!node1.subNodes.containsKey(m)) {
          ns.put(m, b);
        }
        return true;
      });
      newSubNodes = ns;
    }
    return new SampleNode(newSampleCount, newSubNodes);
  }


  public void add(final SampleNode node) {
    this.sampleCount += node.getSampleCount();
    TMap<Method, SampleNode> oSubNodes = (TMap<Method, SampleNode>) node.getSubNodes();
    if (this.subNodes == null) {
      this.subNodes = oSubNodes;
    } else if (oSubNodes != null) {
      oSubNodes.forEachEntry((final Method m, final SampleNode b) -> {
        SampleNode other = subNodes.get(m);
        if (other == null) {
          subNodes.put(m, b);
        } else {
          other.sampleCount += b.sampleCount;
        }
        return true;
      });
    }
  }



  public static TMap<Method, SampleNode> cloneSubNodes(final SampleNode node) {
    final TMap<Method, SampleNode> ns = new MethodMap<>(node.subNodes.size());
    putAllClones(node.subNodes, ns);
    return ns;
  }

  public static void putAllClones(final TMap<Method, SampleNode> source,
          final TMap<Method, SampleNode> destination) {
    source.forEachEntry((final Method a, final SampleNode b) -> {
      destination.put(a, SampleNode.clone(b));
      return true;
    });
  }

  public SampleNode(final int count, @Nullable final TMap<Method, SampleNode> subNodes) {
    this.sampleCount = count;
    this.subNodes = subNodes;
  }

  void addSample(final StackTraceElement[] stackTrace, final int from) {
    sampleCount++;
    if (from >= 0) {
      Method method = Methods.getMethod(stackTrace[from]);
      SampleNode subNode = null;
      if (subNodes == null) {
        subNodes = new MethodMap();
      } else {
        subNode = subNodes.get(method);
      }
      if (subNode == null) {
        subNodes.put(method, new SampleNode(stackTrace, from - 1));
      } else {
        subNode.addSample(stackTrace, from - 1);
      }
    }
  }

  @Override
  public int getSampleCount() {
    return sampleCount;
  }

  @Nullable
  @Override
  public TMap<Method, SampleNode> getSubNodes() {
    return subNodes;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(64);
    writeTo(sb);
    return sb.toString();
  }

  public int height() {
    if (subNodes == null) {
      return 1;
    } else {
      int subHeight = 0;
      for (SampleNode node : subNodes.values()) {
        int nHeight = node.height();
        if (nHeight > subHeight) {
          subHeight = nHeight;
        }
      }
      return subHeight + 1;
    }

  }

  /**
   * to do: have to remove recursion...
   *
   * @return the total number of nodes in this tree.
   */
  public int getNrNodes() {
    if (subNodes == null) {
      return 1;
    } else {
      int nrNodes = 0;
      for (SampleNode node : subNodes.values()) {
        nrNodes += node.getNrNodes();
      }
      return nrNodes + 1;
    }
  }

  /**
   * creates a copy filtered by predicate.
   * @param predicate
   * @return
   */
  @Nullable
  public SampleNode filteredBy(final Predicate<Method> predicate) {

    int newCount = this.sampleCount;

    TMap<Method, SampleNode> sns = null;
    if (this.subNodes != null) {
      for (Map.Entry<Method, SampleNode> entry : this.subNodes.entrySet()) {
        Method method = entry.getKey();
        SampleNode sn = entry.getValue();
        if (predicate.test(method)) {
          newCount -= sn.getSampleCount();
        } else {
          if (sns == null) {
            sns = new MethodMap<>();
          }
          SampleNode sn2 = sn.filteredBy(predicate);
          if (sn2 == null) {
            newCount -= sn.getSampleCount();
          } else {
            newCount -= sn.getSampleCount() - sn2.getSampleCount();
            sns.put(method, sn2);
          }

        }
      }
    }
    if (newCount == 0) {
      return null;
    } else if (newCount < 0) {
      throw new IllegalStateException("child sample counts must be <= parent sample count, detail: " + this);
    } else {
      return new SampleNode(newCount, sns);
    }
  }

  @Override
  public void writeJsonTo(final Appendable appendable) throws IOException {
    writeTo(Methods.ROOT, appendable);
  }

  public void writeTo(final Method m, final Appendable appendable) throws IOException {
    Deque<Object> dq = new ArrayDeque<>();
    dq.add(Pair.of(m, this));
    while (!dq.isEmpty()) {
      Object obj = dq.removeLast();
      if (obj instanceof CharSequence) {
        appendable.append((CharSequence) obj);
      } else {
        Map.Entry<Method, SampleNode> s = (Map.Entry<Method, SampleNode>) obj;
        appendable.append("{\"");
        Methods.writeTo(s.getKey(), appendable);
        appendable.append("\":");
        SampleNode sn = s.getValue();
        appendable.append(Integer.toString(sn.getSampleCount()));
        TMap<Method, SampleNode> cSn = sn.getSubNodes();
        if (cSn != null) {
          Iterator<Map.Entry<Method, SampleNode>> iterator = cSn.entrySet().iterator();
          if (iterator.hasNext()) {
            appendable.append(",\"c\":[");
            dq.addLast("]}");
            dq.addLast(iterator.next());
            while (iterator.hasNext()) {
              dq.addLast(",");
              dq.addLast(iterator.next());
            }
          } else {
            appendable.append('}');
          }
        } else {
          appendable.append('}');
        }
      }
    }
  }

  /**
   * A Json format compatible with: https://github.com/spiermar/d3-flame-graph
   * @param appendable
   * @throws IOException
   */
  public void writeD3JsonTo(final Appendable appendable) throws IOException {
    writeD3JsonFormatTo(Methods.ROOT, appendable);
  }

  /**
   * A Json format compatible with: https://github.com/spiermar/d3-flame-graph
   * @param m
   * @param appendable
   * @throws IOException
   */
  public void writeD3JsonFormatTo(final Method m, final Appendable appendable) throws IOException {
    Deque<Object> dq = new ArrayDeque<>();
    dq.add(Pair.of(m, this));
    while (!dq.isEmpty()) {
      Object obj = dq.removeLast();
      if (obj instanceof CharSequence) {
        appendable.append((CharSequence) obj);
      } else {
        Map.Entry<Method, SampleNode> s = (Map.Entry<Method, SampleNode>) obj;
        appendable.append("{\"name\":\"");
        Methods.writeTo(s.getKey(), appendable);
        appendable.append("\",\"value\":");
        SampleNode sn = s.getValue();
        appendable.append(Integer.toString(sn.getSampleCount()));
        TMap<Method, SampleNode> cSn = sn.getSubNodes();
        if (cSn != null) {
          Iterator<Map.Entry<Method, SampleNode>> iterator = cSn.entrySet().iterator();
          if (iterator.hasNext()) {
            appendable.append(",\"children\":[");
            dq.addLast("]}");
            dq.addLast(iterator.next());
            while (iterator.hasNext()) {
              dq.addLast(",");
              dq.addLast(iterator.next());
            }
          } else {
            appendable.append('}');
          }
        } else {
          appendable.append('}');
        }
      }
    }
  }


  private static final class TraversalData {
    private final Method m;
    private final SampleNode n;

    TraversalData(final Method m, final SampleNode n) {
      this.m = m;
      this.n = n;
    }

  }

  public interface Invocation {
    @CheckReturnValue
    boolean invocation(Method from, Method to, int nrSamples);
  }

  public static void traverse(final Method m, final SampleNode node, final Invocation handler) {
    traverse(m, node, handler, true);
  }

  public static void traverse(final Method m, final SampleNode node, final Invocation handler,
          final boolean breadthFirst) {
    traverse(m, node, handler, breadthFirst ? Deque<TraversalData>::pollFirst : Deque<TraversalData>::pollLast);
  }

  public static void traverse(final Method m, final SampleNode node, final Invocation handler,
          final Function<Deque, TraversalData> func) {
    Deque<TraversalData> dq = new ArrayDeque<>();
    dq.add(new TraversalData(m, node));
    TraversalData t;
    while ((t = func.apply(dq)) != null) {
      if (t.n.subNodes != null) {
        Method from = t.m;
        boolean conti = t.n.subNodes.forEachEntry(new TObjectObjectProcedure<Method, SampleNode>() {
          @Override
          public boolean execute(final Method a, final SampleNode b) {
            boolean result = handler.invocation(from, a, b.sampleCount);
            if (result) {
              dq.addLast(new TraversalData(a, b));
            }
            return result;
          }
        });
        if (!conti) {
          return;
        }
      }
    }
  }


  public static Pair<Method, SampleNode> parse(@WillNotClose final Reader r) throws IOException {
    JsonParser jsonP = Json.FACTORY.createParser(r);
    consume(jsonP, JsonToken.START_OBJECT);
    MutableHolder<Method> method  = MutableHolder.of(null);
    MutableHolder<SampleNode> samples = MutableHolder.of((SampleNode) null);
    parse(jsonP, (m, s) -> {
      method.setValue(m); samples.setValue(s);
    });
    return Pair.of(method.get(), samples.get());
  }

  private static void parse(final JsonParser jsonP, final BiConsumer<Method, SampleNode> consumer) throws IOException {
    consume(jsonP, JsonToken.FIELD_NAME);
    String name = jsonP.getCurrentName();
    consume(jsonP, JsonToken.VALUE_NUMBER_INT);
    int sc = jsonP.getIntValue();
    JsonToken nextToken = jsonP.nextToken();
    if (nextToken == JsonToken.END_OBJECT) {
      consumer.accept(Methods.from(name), new SampleNode(sc, null));
    } else if (nextToken == JsonToken.FIELD_NAME) {
      consume(jsonP, JsonToken.START_ARRAY);
      TMap<Method, SampleNode> nodes = new MethodMap<>();
      while (jsonP.nextToken() != JsonToken.END_ARRAY) {
        parse(jsonP, nodes::put);
      }
      consume(jsonP, JsonToken.END_OBJECT);
      consumer.accept(Methods.from(name), new SampleNode(sc, nodes));
    } else {
      throw new JsonParseException(jsonP, "Expected field name or end Object, not: " + nextToken);
    }
  }

  public static Pair<Method, SampleNode> parseD3Json(@WillNotClose final Reader r) throws IOException {
    JsonParser jsonP = Json.FACTORY.createParser(r);
    consume(jsonP, JsonToken.START_OBJECT);
    MutableHolder<Method> method  = MutableHolder.of(null);
    MutableHolder<SampleNode> samples = MutableHolder.of((SampleNode) null);
    parseD3Json(jsonP, (m, s) -> {
      method.setValue(m); samples.setValue(s);
    });
    return Pair.of(method.get(), samples.get());
  }

  @SuppressFBWarnings("WEM_WEAK_EXCEPTION_MESSAGING") // not that weak here...
  private static void parseD3Json(final JsonParser jsonP, final BiConsumer<Method, SampleNode> consumer)
          throws IOException {
    String methodName = null;
    int nrSamples = -1;
    TMap<Method, SampleNode> nodes = null;
    while (true) {
    JsonToken nextToken = jsonP.nextToken();
      if (nextToken == JsonToken.FIELD_NAME) {
        String fieldName = jsonP.getCurrentName();
        switch (fieldName) {
          case "name":
            consume(jsonP, JsonToken.VALUE_STRING);
            methodName = jsonP.getText();
            break;
          case "value":
            consume(jsonP, JsonToken.VALUE_NUMBER_INT);
            nrSamples = jsonP.getIntValue();
            break;
          case "children":
            consume(jsonP, JsonToken.START_ARRAY);
            nodes = new MethodMap<>();
            while (jsonP.nextToken() != JsonToken.END_ARRAY) {
              parseD3Json(jsonP, nodes::put);
            }
            break;
          default:
            throw new JsonParseException(jsonP, "Unexpected field name : " + fieldName);
        }
      } else if (nextToken == JsonToken.END_OBJECT) {
        if (methodName == null) {
          throw new JsonParseException(jsonP, "name field not found");
        }
        if (nrSamples < 0) {
          throw new JsonParseException(jsonP, "value field not found");
        }
        consumer.accept(Methods.from(methodName), new SampleNode(nrSamples, nodes));
        return;
      } else {
        throw new JsonParseException(jsonP, "Unexpected " + nextToken);
      }
    }

  }

  private static void consume(final JsonParser jsonP, final JsonToken token)
          throws IOException {
    JsonToken nextToken = jsonP.nextToken();
    if (nextToken != token) {
      throw new JsonParseException(jsonP, "Expected start object, not " + nextToken);
    }
  }

  @Override
  public int hashCode() {
    return 89 * this.sampleCount + Objects.hashCode(this.subNodes);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final SampleNode other = (SampleNode) obj;
    if (this.sampleCount != other.sampleCount) {
      return false;
    }
    if (this.subNodes == other.subNodes) {
      return true;
    }
    if (this.subNodes != null && other.subNodes == null && this.subNodes.isEmpty()) {
      return true;
    }
    if (this.subNodes == null && other.subNodes != null && other.subNodes.isEmpty()) {
      return true;
    }
    return Objects.equals(this.subNodes, other.subNodes);
  }

}
