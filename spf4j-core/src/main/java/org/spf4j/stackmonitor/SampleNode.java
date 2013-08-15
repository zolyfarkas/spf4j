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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.ds.Traversals;
import org.spf4j.ds.Graph;
import org.spf4j.ds.HashMapGraph;
import org.spf4j.ds.VertexEdges;

/**
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class SampleNode {

    private int sampleCount;
    private Map<Method, SampleNode> subNodes;

    public SampleNode(final StackTraceElement[] stackTrace, final int from) {
        sampleCount = 1;
        if (from >= 0) {
            subNodes = new HashMap();
            subNodes.put(new Method(stackTrace[from]), new SampleNode(stackTrace, from - 1));
        }
    }

    public SampleNode(final int count, @Nullable final Map<Method, SampleNode> subNodes) {
        this.sampleCount = count;
        this.subNodes = subNodes;
    }

    public int addSample(final StackTraceElement[] stackTrace, final int from) {
        sampleCount++;
        if (from >= 0) {
            Method method = new Method(stackTrace[from]);
            SampleNode subNode = null;
            if (subNodes == null) {
                subNodes = new HashMap();
            } else {
                subNode = subNodes.get(method);
            }
            if (subNode == null) {
                subNodes.put(method, new SampleNode(stackTrace, from - 1));
                return from + 1;
            } else {
                return subNode.addSample(stackTrace, from - 1);
            }
        }
        return 0;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    @Nullable
    public Map<Method, SampleNode> getSubNodes() {
        return subNodes;
    }

    @Override
    public String toString() {
        return "SampleNode{" + "count=" + sampleCount + ", subNodes=" + subNodes + '}';
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

    @Nullable
    public SampleNode filteredBy(final Predicate<Method> predicate) {

        int newCount = this.sampleCount;

        Map<Method, SampleNode> sns = null;
        if (this.subNodes != null) {
            for (Map.Entry<Method, SampleNode> entry : this.subNodes.entrySet()) {
                Method method = entry.getKey();
                SampleNode sn = entry.getValue();
                if (predicate.apply(method)) {
                    newCount -= sn.getSampleCount();
                } else {
                    if (sns == null) {
                        sns = new HashMap<Method, SampleNode>();
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

        void handle(Method from, Method to, int count, Set<Method> ancestors);
    }

    public void forEach(final InvocationHandler handler, final Method from,
            final Method to, final Set<Method> ancestors) {


        handler.handle(from, to, sampleCount, ancestors);

        if (subNodes != null) {
            ancestors.add(to);
            for (Map.Entry<Method, SampleNode> subs : subNodes.entrySet()) {
                Method toKey = subs.getKey();
                while (ancestors.contains(toKey)) {
                    toKey = toKey.withNewId();
                }
                
                subs.getValue().forEach(handler, to, toKey, ancestors);
            }
            ancestors.remove(to);
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

    public static Graph<Method, InvocationCount> toGraph(final SampleNode rootNode) {
        final HashMapGraph<Method, InvocationCount> result = new HashMapGraph<Method, InvocationCount>();

        rootNode.forEach(new InvocationHandler() {
            @Override
            public void handle(final Method from, final Method to, final int count, final Set<Method> ancestors) {
                VertexEdges<Method, InvocationCount> edges = result.getEdges(from);
                if (edges != null) {
                    // If same invocation exists, count will be incremented.
                    Map<InvocationCount, Method> outgoing = edges.getOutgoing();
                    for (Map.Entry<InvocationCount, Method> entry : outgoing.entrySet()) {
                        if (entry.getValue().equals(to)) {
                            InvocationCount exic = entry.getKey();
                            exic.setValue(exic.getValue() + count);
                            return;
                        }
                    }
                }
                if (!Traversals.isPathTo(result, from, to, new HashSet<Method>())) {
                    // make sure we do not create cycles
                    result.add(new InvocationCount(count), from, to);
                } else {
                    result.add(new InvocationCount(count), from, to.withNewId());
                }
            }
        }, Method.ROOT, Method.ROOT, new HashSet<Method>());

        return result;

    }
}
