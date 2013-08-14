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
import java.util.List;
import java.util.Map;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;
import org.spf4j.base.Pair;
import org.spf4j.ds.RTree;
import org.spf4j.stackmonitor.Method;
import org.spf4j.stackmonitor.SampleNode;

/**
 *
 * @author zoly
 */
public final class StackPanel extends JPanel
        implements ActionListener, MouseListener {

    private SampleNode samples;
    private RTree<Pair<Method, Integer>> tooltipDetail = new RTree<Pair<Method, Integer>>();
    private JPopupMenu menu;
    private int xx;
    private int yy;

    public StackPanel(final SampleNode samples) {
        this.samples = samples;
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
            return tips.get(0).getFirst().toString() + "-" + tips.get(0).getSecond();
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
            int height = paintNode(Method.ROOT, samples, gr, 0, 0, width, rowHeight, 0);
            g2.drawImage(img, insets.left, insets.top, this);
            setPreferredSize(new Dimension((int) size.getWidth(), height + 10));
        } finally {
            g2.dispose();
        }
    }

    private int paintNode(final Method method, final SampleNode node,
            final Graphics2D g2, final int x, final int py, final int width, final int height, final int depth) {
        int y = py;
        int sampleCount = node.getSampleCount();
        String val = method.toString() + "-" + sampleCount;
        setElementColor(depth, g2);
        g2.setClip(x, y, width, height);
        g2.fillRect(x, y, width, height);
        tooltipDetail.insert(new float[]{x, y}, new float[]{width, height}, Pair.of(method, sampleCount));
        g2.setPaint(Color.BLACK);
        g2.drawString(val, x, y + height - 1);
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
    public void actionPerformed(final ActionEvent e) {
        if (e.getActionCommand().equals("FILTER")) {
            List<Pair<Method, Integer>> tips = tooltipDetail.search(new float[]{xx, yy}, new float[]{0, 0});
            if (tips.size() >= 1) {
                final Method value = tips.get(0).getFirst();
                samples = samples.filteredBy(new Predicate<Method>() {

                    @Override
                    public boolean apply(final Method t) {
                        return t.equals(value);
                    }
                });
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

    public static void setElementColor(final int depth, final Graphics2D g2) {
        if (depth % 2 == 0) {
            g2.setPaint(Color.YELLOW);
            g2.setBackground(Color.YELLOW);
        } else {
            g2.setPaint(Color.ORANGE);
            g2.setBackground(Color.ORANGE);
        }
    }
}
