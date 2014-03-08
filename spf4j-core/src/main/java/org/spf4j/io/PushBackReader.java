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

import java.io.IOException;
import java.io.Reader;

/**
 *
 * @author zoly
 */
public final class PushBackReader extends Reader {
    
    private final Reader reader;
    
    private char pchar;
    
    private boolean pushback;

    public PushBackReader(final Reader reader) {
        this.reader = reader;
        this.pushback = false;
    }
    
    @Override
    public int read(final char[] cbuf, final int off, final int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        if (pushback) {
            if (pchar < 0) {
                return -1;
            } else {
                cbuf[off] = pchar;
                pushback = false;
                if (len > 1) {
                    return reader.read(cbuf, off + 1, len - 1) + 1;
                } else {
                    return 1;
                }
            }
        } else {
            return reader.read(cbuf, off, len);
        }
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
    
    public void pushBack(final char c) {
        this.pchar = c;
        pushback = true;
    }
    
}
