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
import java.io.InputStream;
import java.io.OutputStream;
import org.spf4j.base.Arrays;

/**
 *
 * @author zoly
 */
public final class Streams {

    private Streams() { }

    /**
     * see copy(final InputStream is, final OutputStream os, final int buffSize)
     * for detail.
     *
     * @param is
     * @param os
     * @return
     * @throws IOException
     */
     public static long copy(final InputStream is, final OutputStream os) throws IOException {
         return copy(is, os, 8192);
     }

    /**
     * Equivalent to guava ByteStreams.copy, with one special behavior:
     * if is has no bytes immediately available for read, the os is flushed prior to the next
     * read that will probably block.
     *
     * I believe this behavior will yield better performance in most scenarios.
     * This method also makes use of: Arrays.getBytesTmp.
     * THis method should not be invoked from any context making use of Arrays.getBytesTmp.
     *
     * @param pis
     * @param pos
     * @param buffSize
     * @throws IOException
     */
    public static long copy(final InputStream is, final OutputStream os, final int buffSize) throws IOException {
        long total = 0;
        byte[] buffer = Arrays.getBytesTmp(buffSize);
        boolean done = false;
        while (!done) {
            // read+write as long as data is available.
            while (is.available() > 0) {
                int nrRead = is.read(buffer, 0, buffSize);
                if (nrRead < 0) {
                    done = true;
                    break;
                } else {
                    os.write(buffer, 0, nrRead);
                    total += nrRead;
                }
            }
            // there seems to be nothing available to read, lets flush to os.
            os.flush();
            if (done) {
                break;
            }
            // most likely a blocking read.
            int nrRead = is.read(buffer, 0, buffSize);
            if (nrRead < 0) {
                break;
            } else {
                os.write(buffer, 0, nrRead);
                total += nrRead;
            }
        }
        return total;
    }

}
