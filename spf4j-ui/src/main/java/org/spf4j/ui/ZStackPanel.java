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
package org.spf4j.ui;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.spf4j.base.EqualsPredicate;
import org.spf4j.base.Pair;
import org.spf4j.ds.Traversals;
import org.spf4j.ds.Graph;
import org.spf4j.ds.HashMapGraph;
import org.spf4j.stackmonitor.Method;
import org.spf4j.stackmonitor.SampleNode;
import static org.spf4j.ui.StackPanelBase.LINK_COLOR;

/**
 * Stack panel implementation that visualizes the profile data via a "flame real graph".
 *
 * @author zoly
 */
@SuppressFBWarnings("SE_BAD_FIELD")
public final class ZStackPanel extends StackPanelBase {

  private static final long serialVersionUID = 1L;

  private Graph<Method, SampleNode.InvocationCount> graph;
  private Graph<Method, SampleNode.InvocationCount> completeGraph;
  private Map<Method, Rectangle2D> methodLocations;
  private double totalHeight = 0;

  public ZStackPanel(final SampleNode samples) {
    super(samples);
    completeGraph = SampleNode.toGraph(samples);
    graph = null;
  }

  @Override
  public int paint(final Graphics2D gr, final double width, final double rowHeight) {
    paintGraph(gr, 0, 0, (int) width, rowHeight);
    return (int) totalHeight;
  }

  private void paintGraph(
          final Graphics2D g2, final int x, final int y, final double areaWidth, final double rowHeight) {

    graph = completeGraph.copy();
    int rootSamples = graph.getEdges(Method.ROOT).getIncomming().keySet().iterator().next().getValue();
    final double pps = areaWidth / rootSamples;
    methodLocations = new HashMap<Method, Rectangle2D>();
    final Traversals.TraversalCallback<Method, SampleNode.InvocationCount> traversalCallback
            = new Traversals.TraversalCallback<Method, SampleNode.InvocationCount>() {
      private int counter = 0;

      @Override
      public void handle(final Method vertex, final Map<SampleNode.InvocationCount, Method> edges) {
        if (edges.size() == 1) {
          if (vertex.equals(Method.ROOT)) {
            int nrSamples = edges.keySet().iterator().next().getValue();
            drawMethod(vertex, nrSamples, (double) x, (double) y, (double) areaWidth,
                    (double) rowHeight);
          } else {
            Map.Entry<SampleNode.InvocationCount, Method> fromEntry = edges.entrySet().iterator().next();
            Method fromMethod = fromEntry.getValue();
            Rectangle2D fromMethodLocation = methodLocations.get(fromMethod);
            int relativeSamples = 0;
            for (Map.Entry<SampleNode.InvocationCount, Method> ens
                    : graph.getEdges(fromMethod).getOutgoing().entrySet()) {
              Method slm = ens.getValue();
              if (methodLocations.containsKey(slm) && graph.getEdges(slm).getIncomming().size() == 1
                      && !fromMethod.equals(slm)) {
                relativeSamples += ens.getKey().getValue();
              }
            }

            int nrSamples = fromEntry.getKey().getValue();
            double width = nrSamples * pps;
            double newX = fromMethodLocation.getX() + relativeSamples * pps;
            drawMethod(vertex, nrSamples, newX, (fromMethodLocation.getY() + rowHeight),
                    width, rowHeight);
          }
        } else if (edges.size() > 1) {
          renderMethodLinked(edges, vertex);
        } else {
          throw new IllegalStateException("Invalid state, there must be a way to get to node " + vertex);
        }
      }

      @SuppressFBWarnings("ISB_TOSTRING_APPENDING")
      private void drawMethod(final Method vertex, final int nrSamples,
              final double x, final double y, final double width, final double height,
              final Point... fromLinks) {
        Rectangle2D.Double location = new Rectangle2D.Double(x, y, width, height);
        methodLocations.put(vertex, location);
        tooltipDetail.insert(new float[]{(float) x, (float) y}, new float[]{(float) width, (float) height},
                Pair.of(vertex, nrSamples));
        double newHeight = y + height;
        if (totalHeight < newHeight) {
          totalHeight = newHeight;
        }
        FlameStackPanel.setElementColor(counter++, g2);
        g2.setClip((int) x, (int) y, (int) width, (int) height);
        g2.fillRect((int) x, (int) y, (int) width, (int) height);
        String val = vertex.toString() + '-' + nrSamples;

        g2.setPaint(Color.BLACK);
        g2.drawString(val, (int) x, (int) (y + height - 1));
        g2.setClip(null);
        g2.setPaint(LINK_COLOR);
        for (Point divLoc : fromLinks) {
          g2.drawLine((int) divLoc.getX(), (int) divLoc.getY(),
                  (int) (x + width / 2), (int) y);
        }
        g2.drawRect((int) x, (int) y, (int) width, (int) height);
      }

      private void renderMethodLinked(final Map<SampleNode.InvocationCount, Method> edges, final Method vertex) {
        List<Point> fromPoints = new ArrayList<>(edges.size());
        double newYBase = 0;
        double newXBase = Double.MAX_VALUE;
        double newWidth = 0;
        double maxX = Double.MIN_VALUE;
        int nrSamples = 0;
        for (Map.Entry<SampleNode.InvocationCount, Method> fromEntry : edges.entrySet()) {
          Rectangle2D fromRect = methodLocations.get(fromEntry.getValue());
          newWidth += fromEntry.getKey().getValue() * pps;
          nrSamples += fromEntry.getKey().getValue();
          if (fromRect == null) {
            continue;
          }
          double fromX = fromRect.getX();
          if (fromX < newXBase) {
            newXBase = fromX;
          }
          double mx = fromRect.getMaxX();
          if (mx > maxX) {
            maxX = mx;
          }
          double newY = fromRect.getMaxY() + rowHeight;
          if (newY > newYBase) {
            newYBase = newY;
          }
          fromPoints.add(new Point((int) fromRect.getCenterX(), (int) fromRect.getMaxY()));
        }

        Pair<List<Pair<Method, Integer>>, Double> result = findEmptySpace(newXBase, newYBase, newWidth, maxX);
        while (!result.getFirst().isEmpty()) {
          // TODO: optimize this to increment with a better value
          newYBase += rowHeight;
          result = findEmptySpace(newXBase, newYBase, newWidth, maxX);
        }
        drawMethod(vertex, nrSamples, result.getSecond(), newYBase,
                newWidth, rowHeight, fromPoints.toArray(new Point[fromPoints.size()]));
      }

      @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
      private Pair<List<Pair<Method, Integer>>, Double> findEmptySpace(
              final double newXBase, final double newYBase,
              final double newWidth, final double maxX) {
        double tryx = newXBase + (maxX - newXBase) / 2 - newWidth / 2;
        tryx = fitToViewableArea(tryx, newWidth);
        final float[] oc = new float[]{(float) newWidth, Float.MAX_VALUE};
        List<Pair<Method, Integer>> methods = tooltipDetail.search(new float[]{(float) tryx, (float) newYBase}, oc);
        if (!methods.isEmpty()) {
          tryx = newXBase;
          tryx = fitToViewableArea(tryx, newWidth);
          methods = tooltipDetail.search(new float[]{(float) tryx, (float) newYBase}, oc);
          if (!methods.isEmpty()) {
            tryx = maxX - newWidth;
            tryx = fitToViewableArea(tryx, newWidth);
            methods = tooltipDetail.search(new float[]{(float) tryx, (float) newYBase}, oc);
          }
        }
        return Pair.of(methods, tryx);
      }

      private double fitToViewableArea(final double ptryx, final double newWidth) {
        double tryx = ptryx;
        if (tryx < 0) {
          tryx = 0;
        } else if (tryx > areaWidth - newWidth) {
          tryx = areaWidth - newWidth;
        }
        return tryx;
      }
    };

    Traversals.customTraverse(graph, Method.ROOT, traversalCallback);

  }

  @Override
  public String getDetail(final Point location) {
    List<Pair<Method, Integer>> tips = tooltipDetail.search(new float[]{location.x, location.y}, new float[]{0, 0});
    if (tips.size() >= 1) {
      final Pair<Method, Integer> node = tips.get(0);
      final Method method = node.getFirst();
      final Map<SampleNode.InvocationCount, Method> incomming = graph.getEdges(method).getIncomming();
      StringBuilder sb = new StringBuilder();
      sb.append(method).append('-').append(node.getSecond())
              .append("\n invoked from: ");
      appendEdgeInfo(incomming, sb);
      sb.append("\n invoking: ");
      final Map<SampleNode.InvocationCount, Method> outgoing = graph.getEdges(method).getOutgoing();
      appendEdgeInfo(outgoing, sb);
      return sb.toString();
    } else {
      return null;
    }
  }

  @Override
  public void filter() {
    List<Pair<Method, Integer>> tips = tooltipDetail.search(new float[]{xx, yy}, new float[]{0, 0});
    if (tips.size() >= 1) {
      final Method value = tips.get(0).getFirst().withId(0);
      samples = samples.filteredBy(new EqualsPredicate<Method>(value));
      if (samples != null) {
        this.completeGraph = SampleNode.toGraph(samples);
      } else {
        this.completeGraph = new HashMapGraph<>();
      }
      repaint();
    }
  }

  private static void appendEdgeInfo(final Map<SampleNode.InvocationCount, Method> incomming,
          final StringBuilder sb) {
    for (Map.Entry<SampleNode.InvocationCount, Method> entry : incomming.entrySet()) {
      int ic = entry.getKey().getValue();
      Method method = entry.getValue();
      sb.append(method).append('-').append(ic).append("; ");
    }
  }
}
