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
package org.spf4j.io;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gnu.trove.map.hash.THashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckReturnValue;
import javax.annotation.ParametersAreNonnullByDefault;
import org.spf4j.base.Strings;

/**
 * Supports CSV format as described at: https://en.wikipedia.org/wiki/Comma-separated_values.
 * either of \n \r or \r\n are valid end of line delimiters
 *
 * why another implementation?
 * because I need one that is as fast as possible, and as flexible as possible.
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
@SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE") // FB gets it wrong here
public final class Csv {

    private Csv() {
    }

    public interface CsvHandler<T> {

        void startRow();

        void element(CharSequence elem);

        void endRow();

        T eof();
    }

    public interface CsvRowHandler<T> {

        void element(CharSequence elem);

        T eof();
    }


    public interface CsvMapHandler<T> {

        void row(Map<String, CharSequence> row);

        T eof();
    }


    public static void writeCsvRow(final Writer writer, final Object... elems) throws IOException {
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


    public static <T> T read(final File file, final Charset charset,
            final CsvMapHandler<T> handler) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset))) {
            return read(br, handler);
        }
    }

    public static <T> T read(final File file, final Charset charset,
            final CsvHandler<T> handler) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset))) {
            return read(br, handler);
        }
    }

    public static <T> T read(final Reader preader,
            final CsvMapHandler<T> handler) throws IOException {
        return read(preader, new CsvHandler<T>() {

            private boolean first = true;

            private final List<String> header = new ArrayList<>();

            private int elemIdx;

            private Map<String, CharSequence> row = null;

            @Override
            public void startRow() {
                elemIdx = 0;
                if (!first) {
                    row = new THashMap<>(header.size());
                }
            }

            @Override
            public void element(final CharSequence elem) {
                if (first) {
                    header.add(elem.toString());
                } else {
                    row.put(header.get(elemIdx), elem);
                }
                elemIdx++;
            }

            @Override
            public void endRow() {
                if (first) {
                  first = false;
                } else {
                    handler.row(row);
                }
            }

            @Override
            public T eof() {
                return handler.eof();
            }
        });
    }


    public static <T> T readRow(final Reader reader, final CsvRowHandler<T> handler) throws IOException {
        return read(reader, new CsvHandler<T>() {

            @Override
            public void startRow() {
            }

            @Override
            public void element(final CharSequence elem) {
                handler.element(elem);
            }

            @Override
            public void endRow() {
            }

            @Override
            public T eof() {
                return handler.eof();
            }
        });
    }

    public static <T> T read(final Reader preader,
            final CsvHandler<T> handler) throws IOException {
        PushbackReader reader = new PushbackReader(preader);
        int firstChar = reader.read();
        if (firstChar != UTF_BOM) {
            reader.unread(firstChar);
        }
        return readNoBom(reader, handler);
    }

    /**
     * http://unicode.org/faq/utf_bom.html#BOM
     */
    public static final int UTF_BOM = '\uFEFF';

    /**
     * reads CSV format until EOF of reader.
     *
     * @param <T>
     * @param preader
     * @param handler
     * @return
     * @throws IOException
     */
    public static <T> T readNoBom(final PushbackReader reader, final CsvHandler<T> handler) throws IOException {
        boolean start = true;
        do {
            if (start) {
                handler.startRow();
                start = false;
            }
            StringBuilder strB = new StringBuilder();
            int c = readCsvElement(reader, strB);
            handler.element(strB);
            if (c == '\r') {
                handler.endRow();
                start = true;
                int c2 = reader.read();
                if (c2 != '\n') {
                    reader.unread(c2);
                }
            } else if (c == '\n') {
                handler.endRow();
                start = true;
            } else if (c < 0) {
                break;
            }
        } while (true);
        return handler.eof();
    }

    private static final char[] TO_ESCAPE = new char[]{',', '\n', '\r', '"'};

    public static void writeCsvElement(final CharSequence elem, final Writer writer) throws IOException {
        if (Strings.contains(elem, TO_ESCAPE)) {
            writeEscaped(elem, writer);
        } else {
            writer.append(elem);
        }
    }

    private static void writeEscaped(final CharSequence elem, final Writer writer) throws IOException {
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
    }

    public static CharSequence toCsvElement(final CharSequence elem) {
        if (Strings.contains(elem, TO_ESCAPE)) {
            StringWriter sw = new StringWriter(elem.length() - 1);
            try {
                writeEscaped(elem, sw);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            return sw.toString();
        } else {
            return elem;
        }
    }

    /**
     * returns next character.
     *
     * @param reader
     * @param addElemTo
     * @return
     * @throws IOException
     */
    @CheckReturnValue
    public static int readCsvElement(final Reader reader, final StringBuilder addElemTo) throws IOException {
        int c = reader.read();
        if (c < 0) {
            return c;
        }
        if (c == '"') {
            c = reader.read();
            while (c >= 0) {
                if (c == '"') {
                    int c2 = reader.read();
                    if (c2 >= 0) {
                        if (c2 == '"') {
                            addElemTo.append((char) c);
                        } else {
                            return c2;
                        }
                    } else {
                        return c2;
                    }
                } else {
                    addElemTo.append((char) c);
                }
                c = reader.read();
            }
        } else {
            while (c != ',' && c != '\n' && c != '\r' && c >= 0) {
                addElemTo.append((char) c);
                c = reader.read();
            }
        }
        return c;
    }

}
