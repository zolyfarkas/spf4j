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
import java.util.HashMap;

/**
 * Translates a value using a lookup table.
 *
 * @since 3.0
 * @version $Id$
 */
@edu.umd.cs.findbugs.annotations.SuppressWarnings("CLI_CONSTANT_LIST_INDEX")
public final class LookupTranslator extends CharSequenceTranslator {

    private final HashMap<String, CharSequence> lookupMap;
    private final int shortest;
    private final int longest;


    public LookupTranslator(final CharSequence[]... lookup) {
        lookupMap = new HashMap<String, CharSequence>();
        int ashortest = Integer.MAX_VALUE;
        int alongest = 0;
        if (lookup != null) {
            for (final CharSequence[] seq : lookup) {
                this.lookupMap.put(seq[0].toString(), seq[1]);
                final int sz = seq[0].length();
                if (sz < ashortest) {
                    ashortest = sz;
                }
                if (sz > alongest) {
                    alongest = sz;
                }
            }
        }
        shortest = ashortest;
        longest = alongest;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int translate(final CharSequence input, final int index, final Writer out) throws IOException {
        int max = longest;
        final int l = input.length();
        if (index + longest > l) {
            max = l - index;
        }
        // descend so as to get a greedy algorithm
        for (int i = max; i >= shortest; i--) {
            final CharSequence subSeq = input.subSequence(index, index + i);
            final CharSequence result = lookupMap.get(subSeq.toString());
            if (result != null) {
                out.write(result.toString());
                return i;
            }
        }
        return 0;
    }
}

