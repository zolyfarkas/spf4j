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
import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gnu.trove.map.TMap;
import org.spf4j.base.StackSamples;
import gnu.trove.procedure.TObjectObjectProcedure;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
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
public final class SampleNode extends MethodMap<SampleNode> implements Serializable, StackSamples {

  private static final long serialVersionUID = 1L;

  private int sampleCount;

  public SampleNode(final int count, final int capacity) {
    super(capacity);
    this.sampleCount = count;
  }

  public SampleNode(final int count) {
    super(0);
    this.sampleCount = count;
  }

  public SampleNode() {
    super(0);
    this.sampleCount = 0;
  }

  @VisibleForTesting
  void addToCount(final int nr) {
    sampleCount += nr;
  }

  public static SampleNode createSampleNode(final StackTraceElement... stackTrace) {
    SampleNode result = new SampleNode(1);
    SampleNode prevResult = result;
    for (int i = stackTrace.length - 1; i >= 0; i--) {
      StackTraceElement elem = stackTrace[i];
      SampleNode node = new SampleNode(1);
      prevResult.put(Methods.getMethod(elem), node);
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
      SampleNode nNode = prevResult.get(method);
      if (nNode != null) {
        nNode.sampleCount++;
      } else {
        nNode = new SampleNode(1);
        prevResult.put(method, nNode);
      }
      prevResult = nNode;
    }
  }

  @Override
  public TMap<Method, SampleNode> getSubNodes() {
    return this;
  }

  private static class Traverse {
    private final SampleNode parent;
    private final Method method;
    private final SampleNode child;

    Traverse(final SampleNode parent, final Method method, final SampleNode child) {
      this.parent = parent;
      this.method = method;
      this.child = child;
    }
  }

  public static SampleNode clone(final SampleNode node) {
    if (node.isEmpty()) {
      return new SampleNode(node.sampleCount);
    }
    SampleNode result = new SampleNode(node.sampleCount, node.size());
    ArrayDeque<Traverse> traverse = new ArrayDeque<>();
    node.forEachEntry((final Method a, final SampleNode b) -> {
      traverse.add(new Traverse(result, a, b));
      return true;
    });
    Traverse t;
    while ((t = traverse.poll()) != null) {
      if (t.child.isEmpty()) {
        t.parent.put(t.method, new SampleNode(t.child.sampleCount));
      } else {
        SampleNode nc = new SampleNode(t.child.sampleCount, t.child.size());
        t.parent.put(t.method, nc);
        t.child.forEachEntry((final Method a, final SampleNode b) -> {
          traverse.add(new Traverse(nc, a, b));
          return true;
        });
      }
    }
    return result;
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
    SampleNode result = new SampleNode(node1.sampleCount + node2.sampleCount,
            Math.max(node1.size(), node2.size()));
      node1.forEachEntry((final Method m, final SampleNode b) -> {
        SampleNode other = node2.get(m);
        if (other == null) {
          result.put(m, SampleNode.clone(b));
        } else {
          result.put(m, aggregate(b, other));
        }
        return true;
      });
      node2.forEachEntry((final Method m, final SampleNode b) -> {
        if (!node1.containsKey(m)) {
          result.put(m, SampleNode.clone(b));
        }
        return true;
      });
    return result;
  }

  /**
   * Aggregation implementation where parts of node1 and node2 will be re-used.
   * @param node1
   * @param node2
   * @return
   */
  @Nullable
  @SuppressFBWarnings("CFS_CONFUSING_FUNCTION_SEMANTICS") // this is why this is called unsafe.
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
      node1.add(node2);
      return node1;
    }
  }


  /**
   * add other samples to this one.
   * other will not be usable after this operation since it will be linked directly where possible.
   * @param other
   */
  public void add(final SampleNode other) {
    this.sampleCount += other.sampleCount;
      other.forEachEntry((final Method m, final SampleNode b) -> {
        SampleNode xChild = get(m);
        if (xChild == null) {
          put(m, b);
        } else {
          xChild.add(b);
        }
        return true;
      });
  }

  @Override
  public int getSampleCount() {
    return sampleCount;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(64);
    writeTo(sb);
    return sb.toString();
  }

  public int height() {
    if (isEmpty()) {
      return 1;
    } else {
      int subHeight = 0;
      for (SampleNode node : values()) {
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
    if (isEmpty()) {
      return 1;
    } else {
      int nrNodes = 0;
      for (SampleNode node : values()) {
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
    SampleNode result = new SampleNode(0);
      for (Map.Entry<Method, SampleNode> entry : this.entrySet()) {
        Method method = entry.getKey();
        SampleNode sn = entry.getValue();
        if (predicate.test(method)) {
          newCount -= sn.getSampleCount();
        } else {
          SampleNode sn2 = sn.filteredBy(predicate);
          if (sn2 == null) {
            newCount -= sn.getSampleCount();
          } else {
            newCount -= sn.getSampleCount() - sn2.getSampleCount();
            result.put(method, sn2);
          }
        }
    }
    if (newCount == 0) {
      return null;
    } else if (newCount < 0) {
      throw new IllegalStateException("child sample counts must be <= parent sample count, detail: " + this);
    } else {
      result.sampleCount = newCount;
      return result;
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
          Iterator<Map.Entry<Method, SampleNode>> iterator = sn.entrySet().iterator();
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
          Iterator<Map.Entry<Method, SampleNode>> iterator = sn.entrySet().iterator();
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
      if (!t.n.isEmpty()) {
        Method from = t.m;
        boolean conti = t.n.forEachEntry(new TObjectObjectProcedure<Method, SampleNode>() {
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
      consumer.accept(Methods.from(name), new SampleNode(sc));
    } else if (nextToken == JsonToken.FIELD_NAME) {
      consume(jsonP, JsonToken.START_ARRAY);
      SampleNode sn = new SampleNode(sc);
      while (jsonP.nextToken() != JsonToken.END_ARRAY) {
        parse(jsonP, sn::put);
      }
      consume(jsonP, JsonToken.END_OBJECT);
      consumer.accept(Methods.from(name), sn);
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
    SampleNode sn = new SampleNode(-1);
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
            sn.sampleCount = jsonP.getIntValue();
            break;
          case "children":
            consume(jsonP, JsonToken.START_ARRAY);
            while (jsonP.nextToken() != JsonToken.END_ARRAY) {
              parseD3Json(jsonP, sn::put);
            }
            break;
          default:
            throw new JsonParseException(jsonP, "Unexpected field name : " + fieldName);
        }
      } else if (nextToken == JsonToken.END_OBJECT) {
        if (methodName == null) {
          throw new JsonParseException(jsonP, "name field not found");
        }
        if (sn.sampleCount < 0) {
          throw new JsonParseException(jsonP, "value field not found");
        }
        consumer.accept(Methods.from(methodName), sn);
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
    return 89 * this.sampleCount + super.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    if (this.sampleCount != ((SampleNode) obj).sampleCount) {
      return false;
    }
    return super.equals(obj);
  }

  public void writeExternal(final ObjectOutput out) throws IOException {
        // NOTE: Super was not written in version 0
        super.writeExternal(out);

        // NUMBER OF SAMPLES
        out.writeInt(this.sampleCount);
    }

    public void readExternal(final ObjectInput in)
            throws IOException, ClassNotFoundException {

        super.readExternal(in);

        // NUMBER OF SAMPLES
        this.sampleCount = in.readInt();
    }


}
