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
package org.spf4j.ui;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * @author Zoltan Farkas
 */
public final class Arrow2D extends Line2D.Float {

  private static final long serialVersionUID = 1L;

  public Arrow2D() {
  }

  public Arrow2D(final float x1, final float y1, final float x2, final float y2) {
    super(x1, y1, x2, y2);
  }

  public Arrow2D(final Point2D p1, final Point2D p2) {
    super(p1, p2);
  }


  public void draw(final Graphics2D g2d) {
    draw(g2d, (int) x1, (int) y1, (int) x2, (int) y2);
  }

  public static void draw(final Graphics2D g2d, final int x1, final int y1, final int x2, final int y2) {
    g2d.drawLine(x1, y1, x2, y2);
    double angle = Math.atan2(y2 - y1, x2 - x1);
    AffineTransform tx = new AffineTransform();
    tx.translate(x2, y2);
    tx.rotate((angle - Math.PI / 2d));
    Polygon arrowHead = new Polygon();
    arrowHead.addPoint(0, 5);
    arrowHead.addPoint(-5, -5);
    arrowHead.addPoint(5, -5);
    Graphics2D g = (Graphics2D) g2d.create();
    g.setTransform(tx);
    g.fill(arrowHead);
    g.dispose();
  }

}
