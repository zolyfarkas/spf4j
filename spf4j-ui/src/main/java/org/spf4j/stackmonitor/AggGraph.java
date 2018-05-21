/*
 * Copyright 2018 SPF4J.
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

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.spf4j.base.Method;
import org.spf4j.ds.Graph;
import org.spf4j.ds.HashMapGraph;
import org.spf4j.stackmonitor.SampleGraph.SampleKey;

/**
 *
 * @author Zoltan Farkas
 */
public final class AggGraph {

  private AggGraph() { }

    @Nonnull
  public static Graph<SampleKey, SampleNode.InvocationCount> toGraph(final SampleNode rootNode) {
    final HashMapGraph<SampleKey, SampleNode.InvocationCount> result = new HashMapGraph<>();

    rootNode.forEach((final Method pfrom, final Method pto,
            final int count, final Map<Method, Integer> ancestors) -> {
      SampleKey from;
      SampleKey to;
      Integer val = ancestors.get(pfrom);
      if (val != null) {
        from = new SampleKey(pfrom, val - 1);
      } else {
        from = new SampleKey(pfrom, 0);
      }
      val = ancestors.get(pto);
      if (val != null) {
        to = new SampleKey(pto, val);
      } else {
        to = new SampleKey(pto, 0);
      }

      SampleNode.InvocationCount ic = result.getEdge(from, to);

      if (ic == null) {
        result.add(new SampleNode.InvocationCount(count), from, to);
      } else {
        ic.setValue(count + ic.getValue());
      }
    }, Method.ROOT, Method.ROOT, new HashMap<Method, Integer>());

    return result;

  }

}
