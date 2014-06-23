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
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
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
public abstract class StackPanelBase extends JPanel
        implements ActionListener, MouseListener {
    //CHECKSTYLE:OFF
    protected SampleNode samples;
    protected RTree<Pair<Method, Integer>> tooltipDetail = new RTree<Pair<Method, Integer>>();
    protected int xx;
    protected int yy;    
    //CHECKSTYLE:ON
    private JPopupMenu menu;

    public static final Color LINK_COLOR = new Color(128, 128, 128, 128);
    
    public StackPanelBase(final SampleNode samples) {
        this.samples = samples;
        setPreferredSize(new Dimension(400, 20 * samples.height() + 10));
        final ToolTipManager sharedInstance = ToolTipManager.sharedInstance();
        sharedInstance.registerComponent(this);
        sharedInstance.setDismissDelay(30000);
        menu = buildPopupMenu(this);
        addMouseListener(this);
    }
    
    
        // disable finbugs since I don't care about internationalization for now.
    @edu.umd.cs.findbugs.annotations.SuppressWarnings
    private static JPopupMenu buildPopupMenu(final ActionListener listener) {
        JPopupMenu result = new JPopupMenu("Actions");
        JMenuItem filter = new JMenuItem("Filter");
        filter.setActionCommand("FILTER");
        filter.addActionListener(listener);
        result.add(filter);
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
            double rowHeight =  g2.getFont().getStringBounds("ROOT", g2.getFontRenderContext()).getHeight();

            GraphicsConfiguration gc = g2.getDeviceConfiguration();
            BufferedImage img = gc.createCompatibleImage(
                    (int) available.getWidth(), (int) available.getHeight(),
                    Transparency.TRANSLUCENT);

            double width = available.getWidth();
            tooltipDetail.clear();
            Graphics2D gr = img.createGraphics();
            gr.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int height = paint(gr, width, rowHeight);
            g2.drawImage(img, insets.left, insets.top, this);
            final Dimension dimension = new Dimension((int) size.getWidth(), height + 10);
            setPreferredSize(dimension);
            setSize(dimension);
            
        } finally {
            g2.dispose();
        }
    }


    public abstract int paint(Graphics2D gr, double width, double rowHeight);
    
    @Override
    public final void actionPerformed(final ActionEvent e) {
        final String actionCommand = e.getActionCommand();
        if ("FILTER".equals(actionCommand)) {
            filter();
        } else if ("COPY".equals(actionCommand)) {
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
                public Object getTransferData(final DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                    return detail;
                }
            }, new ClipboardOwner() {
                @Override
                public void lostOwnership(final Clipboard clipboard, final Transferable contents) {
                }
            });
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
    @edu.umd.cs.findbugs.annotations.SuppressWarnings
    public  void mouseClicked(final MouseEvent e) {
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings
    public void mouseEntered(final MouseEvent e) {
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings
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

    public abstract String getDetail(Point location);

    public abstract void filter();
}
