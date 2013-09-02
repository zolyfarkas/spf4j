/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.base;

import java.io.IOException;
import java.io.Writer;

/**
 *
 * @author zoly
 */
public final class Strings {
    
    private Strings() { }
    
    
    public static void writeCsvElement(final String elem, final Writer writer) throws IOException {
        if (elem.contains(",")) {
            int length = elem.length();
            writer.write('"');
            for (int i = 0; i < length; i++) {
                char c = elem.charAt(i);
                if (c == '"') {
                    writer.write("\"\"");
                } else {
                    writer.write(c);
                }
            }
            writer.write('"');
        } else {
            writer.write(elem);
        }
    }
    
    
    public static String toCsvElement(final String elem) {
        if (elem.contains(",")) {
            int length = elem.length();
            StringBuilder builder = new StringBuilder(length + 2);
            builder.append('"');
            for (int i = 0; i < length; i++) {
                char c = elem.charAt(i);
                if (c == '"') {
                    builder.append("\"\"");
                } else {
                    builder.append(c);
                }
            }
            builder.append('"');
            return builder.toString();
            
        } else {
            return elem;
        }
    }
    
    public static int readCsvElement(final String fromStr, final int fromIdx,
            final StringBuilder addElemTo) {
        return readCsvElement(fromStr, fromIdx, fromStr.length(), addElemTo);
    }
    
    public static int readCsvElement(final String fromStr, final int fromIdx,
            final int maxIdx, final StringBuilder addElemTo) {
        int i = fromIdx;
        char c = fromStr.charAt(i);
        if (c == '"') {
            i++;
            while (i < maxIdx) {
                c = fromStr.charAt(i);
                if (c == '"') {
                    int nxtIdx = i + 1;
                    if (nxtIdx < maxIdx) {
                        if (fromStr.charAt(nxtIdx) == '"') {
                            addElemTo.append(c);
                            i = nxtIdx;
                        } else {
                            break;
                        }
                    } else {
                        break;
                    }
                } else {
                    addElemTo.append(c);
                }
                i++;
            }
        } else {
            while (c != ',') {
                addElemTo.append(c);
                i++;
                if (i >= maxIdx) {
                    break;
                }
                c = fromStr.charAt(i);
            }
        }
        return i;
    }
    
    
}
