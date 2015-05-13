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
package org.spf4j.stackmonitor;

import com.google.common.html.HtmlEscapers;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * utility class to generate svg and html out of stack samples.
 * @author zoly
 */
@edu.umd.cs.findbugs.annotations.SuppressWarnings("CBX_CUSTOM_BUILT_XML")
public final class StackVisualizer {

    private StackVisualizer() {
    }

    private static final String[] COLORS = {"#CCE01B",
        "#DDE01B", "#EEE01B", "#FFE01B", "#FFD01B",
        "#FFC01B", "#FFA01B", "#FF901B", "#FF801B",
        "#FF701B", "#FF601B", "#FF501B", "#FF401B"};

    public static void generateHtmlTable(final Writer writer, final Method m,
            final SampleNode node, final int tableWidth, final int maxDepth) throws IOException {
        Map<Method, SampleNode> subNodes = node.getSubNodes();
        writer.append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"overflow:hidden;table-layout:fixed;width:").
                append(Integer.toString(tableWidth)).append("px\">\n");
        int totalSamples = node.getSampleCount();

        if (subNodes != null && maxDepth > 0) {
            writer.append("<tr style=\"height:1em\">");
            for (Map.Entry<Method, SampleNode> entry : subNodes.entrySet()) {
                int width = entry.getValue().getSampleCount() * tableWidth / totalSamples;
                writer.append("<td style=\"vertical-align:bottom; width:").
                        append(Integer.toString(width)).append("px\">");
                generateHtmlTable(writer, entry.getKey(), entry.getValue(), width, maxDepth - 1);
                writer.append("</td>");
            }
            writer.append("<td></td>");
            writer.append("</tr>\n");
        }
        writer.append("<tr style=\"height:1em\" ><td ");
        if (subNodes != null) {
            writer.append("colspan=\"").append(Integer.toString(subNodes.size() + 1)).append("\" ");
        }
        writer.append(" title=\"");
        m.toHtmlWriter(writer);
        writer.append(":");
        writer.append(Integer.toString(node.getSampleCount()));
        writer.append("\" style=\"overflow:hidden;width:100%;vertical-align:bottom ;background:").
                append(COLORS[(int) (Math.random() * COLORS.length)]).append("\">");
        m.toHtmlWriter(writer);
        writer.append(":");
        writer.append(Integer.toString(totalSamples));
        writer.append("</td></tr>\n");

        writer.append("</table>\n");
    }

    public static void generateSvg(final Writer writer, final Method m,
            final SampleNode node, final int x, final int y,
            final int width, final int maxDepth, final String idPfx) throws IOException {
        if (width < 200) {
            throw new IllegalArgumentException("width must be greater than 200, it is " + width);
        }
        writer.append("<svg width=\"" + width + "\" height= \"" + (15 * node.height() + 15) + "\" onload=\""
                + idPfx + "init(evt)\" >\n");
        writer.append("<script type=\"text/ecmascript\">\n"
                + "<![CDATA[\n"
                + "var " + idPfx + "tooltip;\n"
                + "var " + idPfx + "tooltip_bg;\n"
                + "function " + idPfx + "init(evt)\n"
                + "  {\n"
                + "    if ( window.svgDocument == null )\n"
                + "    {\n"
                + "      svgDocument = evt.target.ownerDocument;\n"
                + "    }\n"
                + "    " + idPfx + "tooltip = svgDocument.getElementById('" + idPfx + "tooltip'); "
                + idPfx + "tooltip_bg = svgDocument.getElementById('" + idPfx + "tooltip_bg');"
                + "  }\n"
                + "function " + idPfx + "ss(evt, mouseovertext, xx, yy)\n"
                + "{\n"
                + "  " + idPfx + "tooltip.setAttributeNS(null,\"x\",xx );\n"
                + "  " + idPfx + "tooltip.setAttributeNS(null,\"y\",yy+13 );\n"
                + "  " + idPfx + "tooltip.firstChild.data = mouseovertext;\n"
                + "  " + idPfx + "tooltip.setAttributeNS(null,\"visibility\",\"visible\");\n"
                + "length = " + idPfx + "tooltip.getComputedTextLength();\n"
                + idPfx + "tooltip_bg.setAttributeNS(null,\"width\",length+8);\n"
                + idPfx + "tooltip_bg.setAttributeNS(null,\"x\",xx);\n"
                +  idPfx + "tooltip_bg.setAttributeNS(null,\"y\",yy);\n"
                +  idPfx + "tooltip_bg.setAttributeNS(null,\"visibility\",\"visibile\");"
                + "}\n"
                + "function " + idPfx + "hh()\n"
                + "{\n"
                + "  " + idPfx + "tooltip.setAttributeNS(null,\"visibility\",\"hidden\");\n"
                + idPfx + "tooltip_bg.setAttributeNS(null,\"visibility\",\"hidden\");\n"
                + "}]]>"
                + "</script>");

        generateSubSvg(writer, m, node, x, y + 15, width - 100, maxDepth, idPfx);

        writer.append("<rect fill=\"rgb(255,255,255)\" id=\"" + idPfx + "tooltip_bg\"\n"
                + "      x=\"0\" y=\"0\" rx=\"4\" ry=\"4\"\n"
                + "      width=\"55\" height=\"17\" visibility=\"hidden\"/>");
        writer.append("<text font-size=\"12\" font-family=\"Verdana\" fill=\"rgb(0,0,0)\"  id=\""
                + idPfx + "tooltip\" x=\"0\" y=\"0\" visibility=\"hidden\">Tooltip</text>");
        writer.append("</svg>\n");
    }

    @SuppressFBWarnings("ISB_TOSTRING_APPENDING")
    public static void generateSubSvg(final Writer writer, final Method m, final SampleNode node,
            final int x, final int y, final int width, final int maxDepth, final String idPfx) throws IOException {


        Map<Method, SampleNode> subNodes = node.getSubNodes();

        int totalSamples = node.getSampleCount();
        String id = idPfx + "ix" + x + 'y' + y;
        String content = HtmlEscapers.htmlEscaper().escape(m.toString() + ':' + totalSamples);
        writer.append("<g onmouseover=\"" + idPfx + "ss(evt,'" + content + "'," + x + ", "
                + y + " )\" onmouseout=\"" + idPfx + "hh()\">");
        writer.append("<rect id=\"" + id + "\" x=\"" + x + "\" y=\"" + y + "\" width=\"" + width
                + "\" height=\"15\" fill=\""
                + COLORS[(int) (Math.random() * COLORS.length)] + "\"  />");

        writer.append("<text x=\"" + x + "\" y=\"" + (y + 13)
                + "\" font-size=\"12\" font-family=\"Verdana\" fill=\"rgb(0,0,0)\" "
                + " >");
        writer.append(content.substring(0, Math.min(width / 9, content.length())));
        writer.append("</text>\n");
        writer.append("</g>");

        if (subNodes != null && maxDepth > 0) {
            int rx = 0;
            for (Map.Entry<Method, SampleNode> entry : subNodes.entrySet()) {
                int cwidth = (int) (((long) entry.getValue().getSampleCount()) * width / totalSamples);
                generateSubSvg(writer, entry.getKey(), entry.getValue(), rx + x, y + 15, cwidth, maxDepth - 1, idPfx);
                rx += cwidth;
            }

        }
    }
}
