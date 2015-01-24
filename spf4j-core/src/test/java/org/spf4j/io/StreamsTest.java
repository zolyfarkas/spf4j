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

import com.google.common.base.Charsets;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class StreamsTest {

    @Test
    public void testCopy() throws IOException {
        byte [] testArray = PipedOutputStreamTest.generateTestStr(10001).toString().getBytes(Charsets.UTF_8);
        InputStream bis = new ByteArrayInputStream(testArray);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Streams.copy(bis, bos);
        Assert.assertArrayEquals(testArray, bos.toByteArray());
    }

}
