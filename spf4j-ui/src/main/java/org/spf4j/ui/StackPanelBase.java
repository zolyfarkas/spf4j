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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;
import org.spf4j.base.Method;
import org.spf4j.base.Pair;
import org.spf4j.ds.RTree;
import org.spf4j.stackmonitor.SampleNode;

/**
 * @author zoly
 */
public abstract class StackPanelBase<T> extends JPanel
        implements ActionListener, MouseListener {

  private static final long serialVersionUID = 1L;
  //CHECKSTYLE:OFF
  private SampleNode samples;
  private Method method;
  private RTree<Sampled<T>> samplesRTree = new RTree<>();
  protected int xx;
  protected int yy;
  //CHECKSTYLE:ON
  private final JPopupMenu menu;
  private final LinkedList<Pair<Method, SampleNode>> history;

  public static final Color LINK_COLOR = new Color(128, 128, 128, 128);

  public StackPanelBase(final SampleNode samples) {
    this.samples = samples;
    this.method = Method.ROOT;
    history = new LinkedList<>();
    setPreferredSize(new Dimension(400, 20 * samples.height() + 10));
    final ToolTipManager sharedInstance = ToolTipManager.sharedInstance();
    sharedInstance.registerComponent(this);
    sharedInstance.setDismissDelay(30000);
    menu = buildPopupMenu(this);
    addMouseListener(this);
  }

  public final List<Sampled<T>> search(final int x, final int y, final int w, final int h) {
    return samplesRTree.search(new float[]{x, y}, new float[]{w, h});
  }

  public final List<Sampled<T>> search(final double x, final double y, final double w, final double h) {
    return samplesRTree.search(new float[]{(float) x, (float) y}, new float[]{(float) w, (float) h});
  }

  public final void insert(final int x, final int y, final int w, final int h, final Sampled sampled) {
    samplesRTree.insert(new float[]{x, y}, new float[]{w, h}, sampled);
  }

  public final void insert(final double x, final double y, final double w, final double h, final Sampled sampled) {
    samplesRTree.insert(new float[]{(float) x, (float) y}, new float[]{(float) w, (float) h}, sampled);
  }

  // disable finbugs since I don't care about internationalization for now.
  @SuppressFBWarnings("S508C_NON_TRANSLATABLE_STRING")
  private static JPopupMenu buildPopupMenu(final ActionListener listener) {
    JPopupMenu result = new JPopupMenu("Actions");
    JMenuItem filter = new JMenuItem("Filter");
    filter.setActionCommand("FILTER");
    filter.addActionListener(listener);
    result.add(filter);
    JMenuItem drill = new JMenuItem("Drill");
    drill.setActionCommand("DRILL");
    drill.addActionListener(listener);
    result.add(drill);
    JMenuItem back = new JMenuItem("Back");
    back.setActionCommand("BACK");
    back.addActionListener(listener);
    result.add(back);
    JMenuItem copy = new JMenuItem("Copy");
    copy.setActionCommand("COPY");
    copy.addActionListener(listener);
    result.add(copy);
    return result;
  }

  @Override
  public final String getToolTipText(final MouseEvent event) {
    Point location = event.getPoint();
    return getDetail(location);
  }

  @Override
  public final void paintComponent(final Graphics g) {
    super.paintComponent(g);
    Dimension size = getSize();
    Insets insets = getInsets();
    Rectangle2D available = new Rectangle2D.Double(insets.left, insets.top,
            size.getWidth() - insets.left - insets.right,
            size.getHeight() - insets.top - insets.bottom);
    Graphics2D g2 = (Graphics2D) g.create();
    try {
      double rowHeight = g2.getFont().getStringBounds("ROOT", g2.getFontRenderContext()).getHeight();

      GraphicsConfiguration gc = g2.getDeviceConfiguration();
      BufferedImage img = gc.createCompatibleImage(
              (int) available.getWidth(), (int) available.getHeight(),
              Transparency.TRANSLUCENT);

      double width = available.getWidth();
      samplesRTree.clear();
      Graphics2D gr = img.createGraphics();
      gr.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      int height = paint(gr, width, rowHeight);
      g2.drawImage(img, insets.left, insets.top, this);
      final Dimension dimension = new Dimension((int) size.getWidth(), height + 10);
      setPreferredSize(dimension);
    } finally {
      g2.dispose();
    }
  }

  public abstract int paint(Graphics2D gr, double width, double rowHeight);

  @Override
  public final void actionPerformed(final ActionEvent e) {
    final String actionCommand = e.getActionCommand();
    switch (actionCommand) {
      case "FILTER":
        history.addLast(Pair.of(method, samples));
        filter();
        break;
      case "DRILL":
        history.addLast(Pair.of(method, samples));
        drill();
        break;
      case "BACK":
        Pair<Method, SampleNode> prev = history.pollLast();
        if (prev != null) {
          updateSamples(prev.getFirst(), prev.getSecond());
          repaint();
        }
        break;
      case "COPY":
        final String detail = getDetail(new Point(xx, yy));
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new Transferable() {
          @Override
          public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.stringFlavor};
          }

          @Override
          public boolean isDataFlavorSupported(final DataFlavor flavor) {
            return flavor.equals(DataFlavor.stringFlavor);
          }

          @Override
          public Object getTransferData(final DataFlavor flavor) {
            return detail;
          }
        }, new ClipboardOwner() {
          @Override
          public void lostOwnership(final Clipboard clipboard, final Transferable contents) {
          }
        });
        break;
      default:
        break;
    }
  }

  @Override
  public final void mousePressed(final MouseEvent e) {
    if (e.isPopupTrigger()) {
      xx = e.getX();
      yy = e.getY();
      menu.show(this, e.getX(), e.getY());
    }
  }

  @Override
  public final void mouseReleased(final MouseEvent e) {
    if (e.isPopupTrigger()) {
      xx = e.getX();
      yy = e.getY();
      menu.show(this, e.getX(), e.getY());
    }
  }

  @Override
  @SuppressFBWarnings
  public void mouseClicked(final MouseEvent e) {
    // default do nothing
  }

  @Override
  @SuppressFBWarnings
  public void mouseEntered(final MouseEvent e) {
    // default do nothing
  }

  @Override
  @SuppressFBWarnings
  public void mouseExited(final MouseEvent e) {
    // default do nothing
  }

  public static void setElementColor(final int depth, final Graphics2D g2) {
    if (depth % 2 == 0) {
      g2.setPaint(Color.YELLOW);
      g2.setBackground(Color.YELLOW);
    } else {
      g2.setPaint(Color.ORANGE);
      g2.setBackground(Color.ORANGE);
    }
  }

  //CHECKSTYLE:OFF
  public void updateSamples(final Method m, final SampleNode n) {
    //CHECKSTYLE:ON
    this.samples = n;
    this.method = m;
  }

  public final SampleNode getSamples() {
    return samples;
  }

  public final Method getMethod() {
    return method;
  }

  @Nullable
  public abstract String getDetail(Point location);

  public abstract void filter();

  public abstract void drill();

}
