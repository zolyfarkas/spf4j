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
import java.io.Writer;

public final class UnicodeUnescaper extends CharSequenceTranslator {

    /**
     * {@inheritDoc}
     */
    @Override
    public int translate(final CharSequence input, final int index, final Writer out) throws IOException {
        final int length = input.length();
        if (index + 1 < length &&  input.charAt(index) == '\\' && input.charAt(index + 1) == 'u') {
            // consume optional additional 'u' chars
            int i = 2;
            while (index + i < length && input.charAt(index + i) == 'u') {
                i++;
            }

            if (index + i < length && input.charAt(index + i) == '+') {
                i++;
            }

            if (index + i + 4 <= length) {
                // Get 4 hex digits
                final CharSequence unicode = input.subSequence(index + i, index + i + 4);

                try {
                    final int value = Integer.parseInt(unicode.toString(), 16);
                    out.write((char) value);
                } catch (final NumberFormatException nfe) {
                    throw new IllegalArgumentException("Unable to parse unicode value: " + unicode, nfe);
                }
                return i + 4;
            } else {
                throw new IllegalArgumentException("Less than 4 hex digits in unicode value: '"
                        + input.subSequence(index, length)
                        + "' due to end of CharSequence");
            }
        }
        return 0;
    }
}
