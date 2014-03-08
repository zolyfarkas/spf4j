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

package org.spf4j.base;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

/**
 *
 * @author zoly
 */
public final class Csv {
    
    private Csv() { }
    
        public static void writeCsvRow(final Writer writer, final Object ... elems) throws IOException {
        if (elems.length > 0) {
            int i = 0;
            writeCsvElement(elems[i++].toString(), writer);
            while (i < elems.length) {
                writer.write(',');
                writeCsvElement(elems[i++].toString(), writer);
            }
        }
        writer.write('\n');
    }
    
    public static void writeCsvRow(final Writer writer, final Iterable<?> elems) throws IOException {
        Iterator<?> it = elems.iterator();
        if (it.hasNext()) {
            writeCsvElement(it.next().toString(), writer);
            while (it.hasNext()) {
                writer.write(',');
                writeCsvElement(it.next().toString(), writer);
            }
        }
        writer.write('\n');
    }
    
    public interface CsvHandler<T> {
        void startRow();
        void element(String elem);
        void endRow();
        T eof();
    }
    
    public static <T> T read(final BufferedReader reader, final CsvHandler<T> handler) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            int i = 0;
            int l = line.length();
            handler.startRow();
            do {
                StringBuilder strB = new StringBuilder();
                i = readCsvElement(line, i, l, strB) + 1;
                handler.element(strB.toString());
            } while (i < l);
            handler.endRow();
        }
        return handler.eof();
    }

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

    /**
     * read a CSV element.
     * @param fromStr - string to parse
     * @param fromIdx - start index
     * @param maxIdx - max index
     * @param addElemTo append elem to;
     * @return the start index of next elem (index of next comma)
     */
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
            i++;
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
