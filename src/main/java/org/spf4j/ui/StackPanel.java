/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Transparency;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import javax.swing.JPanel;
import org.spf4j.stackmonitor.Method;
import org.spf4j.stackmonitor.SampleNode;

/**
 *
 * @author zoly
 */
public class StackPanel extends JPanel {

    private SampleNode samples;
    
    public StackPanel(SampleNode samples) {
        this.samples =  samples;
        setPreferredSize(new Dimension(400,14*samples.height()));
    }
    
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);   
        Dimension size = getSize();
        Insets insets = getInsets();
        Rectangle2D available = new Rectangle2D.Double(insets.left, insets.top,
                size.getWidth() - insets.left - insets.right,
                size.getHeight() - insets.top - insets.bottom);        
        Graphics2D g2 = (Graphics2D) g.create();
        int rowHeight = (int) g2.getFont().getStringBounds("ROOT", g2.getFontRenderContext()).getHeight();

        GraphicsConfiguration gc = g2.getDeviceConfiguration();
        BufferedImage img = gc.createCompatibleImage(
                        (int)available.getWidth(), (int)available.getHeight(),
                        Transparency.TRANSLUCENT);
        
        int width = (int) available.getWidth();
        paintNode(Method.ROOT, samples, img.createGraphics(), 0, 0, width, rowHeight , 0);    
        g2.drawImage(img, insets.left, insets.top, this);
        g2.dispose();
    }
    
    private int paintNode (Method method, SampleNode node, Graphics2D g2, int x, int y, int width, int height, int depth) {
        int sampleCount = node.getSampleCount();
        String val = method.getMethodName() + "-" + sampleCount;

        if (depth %2 ==0) {
            g2.setPaint(Color.YELLOW);
            g2.setBackground(Color.YELLOW);
        } else {
           g2.setPaint(Color.ORANGE);
           g2.setBackground(Color.ORANGE); 
        }
        g2.setClip(x, y, width, height);
        g2.fillRect(x, y,width, height);
        g2.setPaint(Color.BLACK);
        g2.drawString(val, x, y +height -1);  
        Map<Method, SampleNode> children = node.getSubNodes();
        int result = height;
        if (children != null) {
            y+= height;
            int relX = x;
            double scale = (double)width/sampleCount; 
            int maxY = 0;
            for (Map.Entry<Method,SampleNode> entry : children.entrySet()) {
                SampleNode cnode = entry.getValue();
                // sampleCount -> width
                // childSampleCount -> childWidth
                int childWidth = (int) (scale * cnode.getSampleCount());
                maxY = Math.max(maxY, paintNode(entry.getKey(), cnode, g2, relX, y, childWidth, height, depth +1));  
                relX += childWidth;
            }
            result += maxY;
        }
        return result;
    }
    

}
