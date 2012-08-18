/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.stackmonitor;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 *
 * @author zoly
 */
public class StackVisualizer {
    
    private StackVisualizer () {}
    
    
    private static final String[] COLORS = {"#CCE01B",
        "#DDE01B", "#EEE01B", "#FFE01B", "#FFD01B",
        "#FFC01B", "#FFA01B", "#FF901B", "#FF801B",
        "#FF701B", "#FF601B", "#FF501B", "#FF401B"};

    
    public static void generateHtmlTable(Writer writer, Method m, SampleNode node, int tableWidth, int maxDepth) throws IOException {
        Map<Method, SampleNode> subNodes = node.getSubNodes();
        writer.append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"overflow:hidden;table-layout:fixed;width:").
                append(Integer.toString(tableWidth)).append("px\">\n");
        int totalSamples = node.getCount();
      
        if (subNodes != null && maxDepth > 0) {
            writer.append("<tr style=\"height:1em\">");
            for (Map.Entry<Method, SampleNode> entry : subNodes.entrySet()) {
                int width = entry.getValue().getCount() * tableWidth / totalSamples;
                writer.append("<td style=\"vertical-align:bottom; width:").append(Integer.toString(width)).append("px\">");
                generateHtmlTable(writer, entry.getKey(), entry.getValue(), width, maxDepth-1);
                writer.append("</td>");
            }
            writer.append("<td></td>");
            writer.append("</tr>\n");
        }
       writer.append( "<tr style=\"height:1em\" ><td ");
        if (subNodes != null) {
            writer.append("colspan=\""). append(Integer.toString( subNodes.size()+1)).append("\" ");
        }
        writer.append( " title=\"");
        m.toWriter(writer);
        writer.append(":");
        writer.append(Integer.toString( node.getCount()));        
        writer.append("\" style=\"overflow:hidden;width:100%;vertical-align:bottom ;background:").
                append(COLORS[(int) (Math.random() * COLORS.length)]).append("\">");
        m.toWriter(writer);
        writer.append(":");
        writer.append(Integer.toString( node.getCount()));
        writer.append("</td></tr>\n");
 
        writer.append("</table>\n");
    }

   
    public static int generateSvg(Writer writer, Method m, SampleNode node, int x, int y, int width, int maxDepth) throws IOException {
        Map<Method, SampleNode> subNodes = node.getSubNodes();
        writer.append("<svg width=\"" + width + "\" x= \"" + x + "\" y=\"" + y +"\" >");
        int totalSamples = node.getCount();
        int depth =0;
        if (subNodes != null && maxDepth > 0) {    
            int rx = 0;
            for (Map.Entry<Method, SampleNode> entry : subNodes.entrySet()) {
                int cwidth = entry.getValue().getCount() * width / totalSamples;                          
                int childDepth = generateSvg(writer, entry.getKey(), entry.getValue(), rx, 0 , cwidth, maxDepth-1);
                if (childDepth >depth) {
                    depth = childDepth;
                }
                rx += cwidth;         
            }
                 
        }
        writer.append("<rect x=\"0\" y=\""+ (depth * 15) +"\" width=\"" + width +"\" height=\"15\" fill=\"" + 
                        COLORS[(int) (Math.random() * COLORS.length)] +"\"  />" );
        
        writer.append("<text x=\"0\" y=\""+ (depth * 15) +"\" font-size=\"12\" font-family=\"Verdana\" fill=\"rgb(0,0,0)\" />" );
 
        writer.append("</svg>\n");
        return depth +1;
    }
    
    
    
    
    
}
