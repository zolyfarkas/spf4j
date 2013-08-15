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

import com.google.common.base.Predicate;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;
import org.spf4j.base.Pair;
import org.spf4j.ds.Traversals;
import org.spf4j.ds.Graph;
import org.spf4j.ds.RTree;
import org.spf4j.stackmonitor.Method;
import org.spf4j.stackmonitor.SampleNode;

/**
 *
 * @author zoly
 */
public final class StackPanelPro extends JPanel
        implements ActionListener, MouseListener {

    private Graph<Method, SampleNode.InvocationCount> graph;
    private Graph<Method, SampleNode.InvocationCount> completeGraph;
    private Map<Method, Rectangle2D> methodLocations;
    private RTree<Pair<Method, Integer>> tooltipDetail = new RTree<Pair<Method, Integer>>();
    private JPopupMenu menu;
    private int xx;
    private int yy;
    private SampleNode samples;

    public StackPanelPro(final SampleNode samples) {
        this.samples = samples;
        completeGraph = SampleNode.toGraph(samples);
        graph = null;
        setPreferredSize(new Dimension(400, 20 * samples.height() + 10));
        ToolTipManager.sharedInstance().registerComponent(this);
        menu = buildPopupMenu();
        addMouseListener(this);
    }

    // disable finbugs since I don't care about internationalization for now.
    @edu.umd.cs.findbugs.annotations.SuppressWarnings
    private JPopupMenu buildPopupMenu() {
        JPopupMenu result = new JPopupMenu("Actions");
        JMenuItem filter = new JMenuItem("Filter");
        filter.setActionCommand("FILTER");
        filter.addActionListener(this);
        result.add(filter);
        return result;
    }

    @Override
    public String getToolTipText(final MouseEvent event) {
        Point location = event.getPoint();
        List<Pair<Method, Integer>> tips = tooltipDetail.search(new float[]{location.x, location.y}, new float[]{0, 0});
        if (tips.size() >= 1) {
            return tips.get(0).getFirst().toString() + "-" + tips.get(0).getSecond()
                    + "\n invoked from: " + graph.getEdges(tips.get(0).getFirst()).getIncomming();
        } else {
            return null;
        }
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        Dimension size = getSize();
        Insets insets = getInsets();
        Rectangle2D available = new Rectangle2D.Double(insets.left, insets.top,
                size.getWidth() - insets.left - insets.right,
                size.getHeight() - insets.top - insets.bottom);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            int rowHeight = (int) g2.getFont().getStringBounds("ROOT", g2.getFontRenderContext()).getHeight();

            GraphicsConfiguration gc = g2.getDeviceConfiguration();
            BufferedImage img = gc.createCompatibleImage(
                    (int) available.getWidth(), (int) available.getHeight(),
                    Transparency.TRANSLUCENT);

            int width = (int) available.getWidth();
            tooltipDetail.clear();
            Graphics2D gr = img.createGraphics();
            gr.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            paintGraph(Method.ROOT, gr, 0, 0, width, rowHeight, 0);
            g2.drawImage(img, insets.left, insets.top, this);
            setPreferredSize(new Dimension((int) size.getWidth(), totalHeight));
        } finally {
            g2.dispose();
        }
    }
    private int totalHeight = 0;

    private void paintGraph(final Method method,
            final Graphics2D g2, final int x, final int y, final int areaWidth, final int rowHeight, final int depth) {

        graph = completeGraph.copy();
        int rootSamples = graph.getEdges(Method.ROOT).getIncomming().keySet().iterator().next().getValue();
        final double pps = ((double) areaWidth) / rootSamples;
        List<Method> toRemove = new ArrayList<Method>();
        for (Method m : graph.getVertices()) {
            double width = 0;
            for (SampleNode.InvocationCount ic : graph.getEdges(m).getIncomming().keySet()) {
                width += ic.getValue() * pps;
            }
            if (width < 1) {
                toRemove.add(m);
            }
        }
        for (Method m : toRemove) {
            graph.remove(m);
        }

        methodLocations = new HashMap<Method, Rectangle2D>();
        final Traversals.TraversalCallback<Method, SampleNode.InvocationCount> traversalCallback =
                new Traversals.TraversalCallback<Method, SampleNode.InvocationCount>() {
            private int counter = 0;

            @Override
            public void handle(final Method vertex, final Map<SampleNode.InvocationCount, Method> edges) {
                if (edges.size() == 1) {
                    if (vertex.equals(Method.ROOT)) {
                        int nrSamples = edges.keySet().iterator().next().getValue();
                        drawMethod(vertex, nrSamples, x, y, areaWidth, rowHeight, edges);
                    } else {
                        Map.Entry<SampleNode.InvocationCount, Method> fromEntry =
                                edges.entrySet().iterator().next();
                        int nrSamples = fromEntry.getKey().getValue();
                        int width = (int) (nrSamples * pps);
                        Method fromMethod = fromEntry.getValue();
                        Rectangle2D fromRect = methodLocations.get(fromMethod);
                        List<Pair<Method, Integer>> methods = tooltipDetail.search(
                                new float[]{(float) fromRect.getX() + 0.1f, (float) fromRect.getY() + rowHeight + 0.1f},
                                new float[]{(float) fromRect.getWidth() - 0.2f, (float) rowHeight - 0.2f});
                        double newX = fromRect.getX();
                        int drawedSamples = 0;
                        boolean existingMethodsHaveSameParent = true;
                        for (Pair<Method, Integer> method : methods) {
                            drawedSamples += method.getSecond();
                            if (!graph.getEdges(method.getFirst()).getIncomming().containsValue(fromEntry.getValue())) {
                                existingMethodsHaveSameParent = false;
                                break;
                            }
                        }
                        if (!existingMethodsHaveSameParent) {
                            renderMethodLinked(edges, vertex);
                        } else {
                            newX += drawedSamples * pps;
                            drawMethod(vertex, nrSamples, (int) newX, (int) (fromRect.getY() + rowHeight),
                                    width, rowHeight, edges);
                        }

                    }
                } else if (edges.size() > 1) {
                    renderMethodLinked(edges, vertex);
                } else {
                    throw new IllegalStateException("Invalid state, there must be a way to get to node " + vertex);
                }
            }

            private void drawMethod(final Method vertex, final int nrSamples,
                    final int x, final int y, final int width, final int height,
                    final Map<SampleNode.InvocationCount, Method> edges) {
                drawMethod(vertex, nrSamples, x, y, width, height,
                        edges, new Point[]{});
            }

            private void drawMethod(final Method vertex, final int nrSamples,
                    final int x, final int y, final int width, final int height,
                    final Map<SampleNode.InvocationCount, Method> edges, final Point[] fromLinks) {
                Rectangle2D.Float location = new Rectangle2D.Float(x, y, width, height);
                methodLocations.put(vertex, location);
                tooltipDetail.insert(new float[]{x, y}, new float[]{width, height},
                        Pair.of(vertex, nrSamples));
                if (width <= 0) {
                    return;
                }
                int newHeight = y + height;
                if (totalHeight < newHeight) {
                    totalHeight = newHeight;
                }
                StackPanel.setElementColor(counter++, g2);
                g2.setClip(x, y, width, height);
                g2.fillRect(x, y, width, height);
                String val = vertex.toString() + "-" + nrSamples;

                g2.setPaint(Color.BLACK);
                g2.drawString(val, x, y + height - 1);
                g2.setClip(null);
                for (Point divLoc : fromLinks) {
                    g2.drawLine((int) divLoc.getX(), (int) divLoc.getY(),
                            x + width / 2, y);
                }
                g2.drawRect(x, y, width, height);
            }

            private void renderMethodLinked(final Map<SampleNode.InvocationCount, Method> edges, final Method vertex) {
                Point[] fromPoints = new Point[edges.size()];
                double newYBase = 0;
                double newXBase = Double.MAX_VALUE;
                double newWidth = 0;
                int i = 0;
                int nrSamples = 0;
                for (Map.Entry<SampleNode.InvocationCount, Method> fromEntry : edges.entrySet()) {
                    Rectangle2D fromRect = methodLocations.get(fromEntry.getValue());
                    double fromX = fromRect.getX();
                    if (fromX < newXBase) {
                        newXBase = fromX;
                    }
                    double newY = fromRect.getMaxY() + rowHeight;
                    if (newY > newYBase) {
                        newYBase = newY;
                    }

                    fromPoints[i] = new Point((int) fromRect.getCenterX(), (int) fromRect.getMaxY());
                    newWidth += fromEntry.getKey().getValue() * pps;
                    nrSamples += fromEntry.getKey().getValue();
                    i++;
                }
                // now find the new Y base
                List<Pair<Method, Integer>> methods = tooltipDetail.search(
                        new float[]{(float) newXBase, (float) newYBase},
                        new float[]{(float) newWidth, Float.MAX_VALUE / 10});
                while (!methods.isEmpty()) {
                    for (Pair<Method, Integer> intersected : methods) {
                        Rectangle2D fromRect = methodLocations.get(intersected.getFirst());
                        double ny = fromRect.getMaxY() + rowHeight;
                        if (newYBase < ny) {
                            newYBase = ny;
                        }
                    }
                    methods = tooltipDetail.search(
                            new float[]{(float) newXBase, (float) newYBase},
                            new float[]{(float) newWidth, Float.MAX_VALUE / 10});
                }
                drawMethod(vertex, nrSamples, (int) newXBase, (int) newYBase,
                        (int) newWidth, rowHeight, edges, fromPoints);
            }
        };

        Traversals.traverse(graph, Method.ROOT, traversalCallback);

    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getActionCommand().equals("FILTER")) {
            List<Pair<Method, Integer>> tips = tooltipDetail.search(new float[]{xx, yy}, new float[]{0, 0});
            if (tips.size() >= 1) {
                final Method value = tips.get(0).getFirst().withId(0);
                samples = samples.filteredBy(new Predicate<Method>() {

                    @Override
                    public boolean apply(final Method t) {
                        return t.equals(value);
                    }
                });
                this.completeGraph = SampleNode.toGraph(samples);
                repaint();
            }
        }
    }

    @Override
    public void mouseClicked(final MouseEvent e) {
    }

    @Override
    public void mousePressed(final MouseEvent e) {
        if (e.isPopupTrigger()) {
            xx = e.getX();
            yy = e.getY();
            menu.show(this, e.getX(), e.getY());
        }
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
        if (e.isPopupTrigger()) {
            xx = e.getX();
            yy = e.getY();
            menu.show(this, e.getX(), e.getY());
        }
    }

    @Override
    public void mouseEntered(final MouseEvent e) {
    }

    @Override
    public void mouseExited(final MouseEvent e) {
    }
}
