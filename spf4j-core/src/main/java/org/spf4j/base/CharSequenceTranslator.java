
package org.spf4j.base;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;


public abstract class CharSequenceTranslator {


    public abstract int translate(CharSequence input, int index, Writer out) throws IOException;


    public final String translate(final CharSequence input) {
        if (input == null) {
            return null;
        }
        try {
            final StringWriter writer = new StringWriter(input.length() * 2);
            translate(input, writer);
            return writer.toString();
        } catch (final IOException ioe) {
            // this should never ever happen while writing to a StringWriter
            throw new RuntimeException(ioe);
        }
    }


    public final void translate(final CharSequence input, final Writer out) throws IOException {
        if (out == null) {
            throw new IllegalArgumentException("The Writer must not be null when translating " + input);
        }
        if (input == null) {
            return;
        }
        int pos = 0;
        final int len = input.length();
        while (pos < len) {
            final int consumed = translate(input, pos, out);
            if (consumed == 0) {
                final char[] c = Character.toChars(Character.codePointAt(input, pos));
                out.write(c);
                pos += c.length;
                continue;
            }
            for (int pt = 0; pt < consumed; pt++) {
                pos += Character.charCount(Character.codePointAt(input, pt));
            }
        }
    }

    /**
     * <p>Returns an upper case hexadecimal <code>String</code> for the given
     * character.</p>
     *
     * @param codepoint The codepoint to convert.
     * @return An upper case hexadecimal <code>String</code>
     */
    public static String hex(final int codepoint) {
        return Integer.toHexString(codepoint).toUpperCase(Locale.ENGLISH);
    }

}

