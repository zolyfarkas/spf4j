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
package org.spf4j.ui;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.spf4j.base.EqualsPredicate;
import org.spf4j.base.Method;
import org.spf4j.base.Pair;
import org.spf4j.stackmonitor.SampleNode;

/**
 * An inverted implementation of Brendan Gregg's flame charts.
 *
 * @author zoly
 */
public final class FlameStackPanel extends StackPanelBase<Pair<Method, SampleNode>> {

  private static final long serialVersionUID = 1L;

  public FlameStackPanel(final Method method,
          final SampleNode samples, final LinkedList<Pair<Method, SampleNode>> history) {
    super(method, samples, history);
  }

  @Override
  public int paint(final Graphics2D gr, final double width, final double rowHeight) {
    return paintNode(getMethod(), getSamples(), gr,
            0, 0, (int) width, (int) rowHeight, 0);
  }

  @SuppressFBWarnings("ISB_TOSTRING_APPENDING")
  private int paintNode(final Method method, final SampleNode node,
          final Graphics2D g2, final int x, final int py, final int width, final int height, final int depth) {
    int y = py;
    int sampleCount = node.getSampleCount();
    String val = method.toString() + '-' + sampleCount;
    setElementColor(depth, g2);
    g2.setClip(x, y, width, height);
    g2.fillRect(x, y, width, height);
    insert(x, y, width, height, Pair.of(method, node));
    g2.setPaint(Color.BLACK);
    g2.drawString(val, x, y + height - 1);
    g2.setClip(null);
    g2.setPaint(LINK_COLOR);
    g2.drawRect(x, y, width, height);
    Map<Method, SampleNode> children = node.getSubNodes();
    int result = height;
    if (children != null) {
      y += height;
      int relX = x;
      double scale = (double) width / sampleCount;
      int maxY = 0;
      for (Map.Entry<Method, SampleNode> entry : children.entrySet()) {
        SampleNode cnode = entry.getValue();
        // sampleCount -> width
        // childSampleCount -> childWidth
        int childWidth = (int) (scale * cnode.getSampleCount());
        if (childWidth > 0) {
          maxY = Math.max(maxY, paintNode(entry.getKey(), cnode, g2, relX, y, childWidth, height, depth + 1));
          relX += childWidth;
        }
      }
      result += maxY;
    }
    return result;
  }

  @Override
  @SuppressFBWarnings("ISB_TOSTRING_APPENDING")
  @Nullable
  public String getDetail(final Point location) {
    List<Pair<Method, SampleNode>> tips = search(location.x, location.y, 0, 0);
    if (tips.size() >= 1) {
      final Pair<Method, SampleNode> m = tips.get(0);
      return m.getFirst().toString() + '-' + m.getSecond().getSampleCount();
    } else {
      return null;
    }
  }

  @Override
  public void filter() {
    List<Pair<Method, SampleNode>> tips = search(xx, yy, 0, 0);
    if (tips.size() >= 1) {
      final Method value = tips.get(0).getFirst();
      updateSamples(getMethod(), getSamples().filteredBy(new EqualsPredicate<Method>(value)));
      repaint();
    }
  }

  @Override
  public void drill() {
    List<Pair<Method, SampleNode>> tips = search(xx, yy, 0, 0);
    if (tips.size() >= 1) {
      Pair<Method, SampleNode> sample = tips.get(0);
      updateSamples(sample.getFirst(), sample.getSecond());
      repaint();
    }
  }

}
