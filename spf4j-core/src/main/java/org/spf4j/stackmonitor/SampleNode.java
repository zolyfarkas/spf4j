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
package org.spf4j.stackmonitor;

import com.google.common.base.Predicate;
import gnu.trove.map.TMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.procedure.TObjectObjectProcedure;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.ds.Graph;
import org.spf4j.ds.HashMapGraph;

/**
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class SampleNode implements Serializable {

    private static final long serialVersionUID = 1L;

    private int sampleCount;
    private TMap<Method, SampleNode> subNodes;

    public SampleNode(final StackTraceElement[] stackTrace, final int from) {
        sampleCount = 1;
        if (from >= 0) {
            subNodes = new THashMap();
            subNodes.put(Method.getMethod(stackTrace[from]), new SampleNode(stackTrace, from - 1));
        }
    }

    public static SampleNode createSampleNode(final StackTraceElement... stackTrace) {
        SampleNode result = new SampleNode(1, null);
        SampleNode prevResult = result;
        for (int i = stackTrace.length - 1; i >= 0; i--) {
            StackTraceElement elem = stackTrace[i];
            if (prevResult.subNodes == null) {
                prevResult.subNodes = new THashMap<>();
            }
            SampleNode node = new SampleNode(1, null);
            prevResult.subNodes.put(Method.getMethod(elem), node);
            prevResult = node;
        }
        return result;
    }


    public static void addToSampleNode(final SampleNode node, final StackTraceElement... stackTrace) {
        SampleNode prevResult = node;
        prevResult.sampleCount++;
        for (int i = stackTrace.length - 1; i >= 0; i--) {
            StackTraceElement elem = stackTrace[i];
            final Method method = Method.getMethod(elem);
            SampleNode nNode;
            if (prevResult.subNodes == null) {
                prevResult.subNodes = new THashMap<>();
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
        final THashMap<Method, SampleNode> newSubNodes = new THashMap<>(node.subNodes.size());
        node.subNodes.forEachEntry(new TObjectObjectProcedure<Method, SampleNode>() {

            @Override
            public boolean execute(final Method a, final SampleNode b) {
                newSubNodes.put(a, SampleNode.clone(b));
                return true;
            }
        });
        return new SampleNode(node.sampleCount, newSubNodes);
    }

    public static SampleNode aggregate(final SampleNode node1, final SampleNode node2) {
        int newSampleCount = node1.sampleCount + node2.sampleCount;
        TMap<Method, SampleNode> newSubNodes;
        if (node1.subNodes == null && node2.subNodes == null) {
            newSubNodes = null;
        } else if (node1.subNodes == null) {
            newSubNodes = cloneSubNodes(node2);
        } else if (node2.subNodes == null) {
            newSubNodes = cloneSubNodes(node1);
        } else {
            final THashMap<Method, SampleNode> ns = new THashMap<>(node1.subNodes.size() + node2.subNodes.size());

            node1.subNodes.forEachEntry(new TObjectObjectProcedure<Method, SampleNode>() {

                @Override
                public boolean execute(final Method m, final SampleNode b) {
                    SampleNode other = node2.subNodes.get(m);
                    if (other == null) {
                        ns.put(m, SampleNode.clone(b));
                    } else {
                        ns.put(m, aggregate(b, other));
                    }
                    return true;
                }
            });
            node2.subNodes.forEachEntry(new TObjectObjectProcedure<Method, SampleNode>() {

                @Override
                public boolean execute(final Method m, final SampleNode b) {
                    if (!node1.subNodes.containsKey(m)) {
                        ns.put(m, SampleNode.clone(b));
                    }
                    return true;
                }
            });
            newSubNodes = ns;

        }
        return new SampleNode(newSampleCount, newSubNodes);
    }

    public static TMap<Method, SampleNode> cloneSubNodes(final SampleNode node) {
        final TMap<Method, SampleNode> ns = new THashMap<>(node.subNodes.size());
        putAllClones(node.subNodes, ns);
        return ns;
    }

    public static void putAllClones(final TMap<Method, SampleNode> source,
            final TMap<Method, SampleNode> destination) {
        source.forEachEntry(new TObjectObjectProcedure<Method, SampleNode>() {

            @Override
            public boolean execute(final Method a, final SampleNode b) {
                destination.put(a, SampleNode.clone(b));
                return true;
            }
        });
    }

    public SampleNode(final int count, @Nullable final TMap<Method, SampleNode> subNodes) {
        this.sampleCount = count;
        this.subNodes = subNodes;
    }

    public void addSample(final StackTraceElement[] stackTrace, final int from) {
        sampleCount++;
        if (from >= 0) {
            Method method = Method.getMethod(stackTrace[from]);
            SampleNode subNode = null;
            if (subNodes == null) {
                subNodes = new THashMap();
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

    public int getSampleCount() {
        return sampleCount;
    }

    @Nullable
    public TMap<Method, SampleNode> getSubNodes() {
        return subNodes;
    }

    @Override
    public String toString() {
        return "SampleNode{" + "count=" + sampleCount
                + ((subNodes == null || subNodes.isEmpty()) ? "" : ", subNodes=" + subNodes) + '}';
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

    @Nullable
    public SampleNode filteredBy(final Predicate<Method> predicate) {

        int newCount = this.sampleCount;

        THashMap<Method, SampleNode> sns = null;
        if (this.subNodes != null) {
            for (Map.Entry<Method, SampleNode> entry : this.subNodes.entrySet()) {
                Method method = entry.getKey();
                SampleNode sn = entry.getValue();
                if (predicate.apply(method)) {
                    newCount -= sn.getSampleCount();
                } else {
                    if (sns == null) {
                        sns = new THashMap<>();
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

    public interface InvocationHandler {

        void handle(Method from, Method to, int count, final Map<Method, Integer> ancestors);
    }

    public void forEach(final InvocationHandler handler, final Method from,
            final Method to, final Map<Method, Integer> ancestors) {

        handler.handle(from, to, sampleCount, ancestors);

        if (subNodes != null) {
            Integer val = ancestors.get(to);
            if (val != null) {
                val = val + 1;
            } else {
                val = 1;
            }
            ancestors.put(to, val);
            for (Map.Entry<Method, SampleNode> subs : subNodes.entrySet()) {
                Method toKey = subs.getKey();
                subs.getValue().forEach(handler, to, toKey, ancestors);
            }
            val = ancestors.get(to);
            if (val == 1) {
                ancestors.remove(to);
            } else {
                val = val - 1;
                ancestors.put(to, val);
            }
        }
    }

    public static final class InvocationCount {

        private int value;

        public InvocationCount(final int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public void setValue(final int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "InvocationCount{" + "value=" + value + '}';
        }
    }

    @Nonnull
    public static Graph<Method, InvocationCount> toGraph(final SampleNode rootNode) {
        final HashMapGraph<Method, InvocationCount> result = new HashMapGraph<>();

        rootNode.forEach(new InvocationHandler() {
            @Override
            public void handle(final Method pfrom, final Method pto, final int count,
                    final Map<Method, Integer> ancestors) {

                Method from = pfrom;
                Method to = pto;
                Integer val = ancestors.get(from);
                if (val != null) {
                    from = from.withId(val - 1);
                }
                val = ancestors.get(to);
                if (val != null) {
                    to = to.withId(val);
                }

                InvocationCount ic = result.getEdge(from, to);

                if (ic == null) {
                    result.add(new InvocationCount(count), from, to);
                } else {
                    ic.setValue(count + ic.getValue());
                }


            }
        }, Method.ROOT, Method.ROOT, new HashMap<Method, Integer>());

        return result;

    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + this.sampleCount;
        return 89 * hash + Objects.hashCode(this.subNodes);
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
