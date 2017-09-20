/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
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
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.perf.memory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sun.jna.NativeLibrary;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.Test;
import org.spf4j.unix.CLibrary;
import org.spf4j.unix.CLibrary.FILE;

/**
 *
 * @author zoly
 */
public final class VMHistogramsTest {


    private static final Cache<byte[], byte[]> CACHE = CacheBuilder.newBuilder().softValues().weakKeys().build();

    @Test
    public void testMemoryUsage() throws FileNotFoundException, IOException {
        //__stdoutp or __stderrp
        final FILE stdout = new FILE(NativeLibrary.getInstance("c").getGlobalVariableAddress("__stdoutp").getPointer(0));
        FILE fp = CLibrary.INSTANCE.freopen("/tmp/testErr.txt", "w", stdout);
        byte [] last = generateGarbage();
        System.out.println("last size =" + last.length);
        CLibrary.INSTANCE.fclose(fp);
    }

    public byte[] generateGarbage() {
        byte [] last = new byte [0];
        for (int i = 0; i < 1000; i++) {
            last = new byte[1024000];
            for (int j = 0; j < 100; j++) {
                CACHE.put(last, last);
                last[j] = (byte) j;
            }
        }
        return last;
    }


//    @Test
//    public void testMemoryUsage2() throws FileNotFoundException, IOException {
//
//        byte [] last = generateGarbage();
//        System.out.println("last size =" + last.length);
//
//        //FileInputStream fin = new FileInputStream(FileDescriptor.out);
//        FileDescriptor fd = new FileDescriptor();
//        sun.misc.SharedSecrets.getJavaIOFileDescriptorAccess().set(fd, 1);
//        FileInputStream fin = new FileInputStream(fd);
//        InputStreamReader reader = new InputStreamReader(new BufferedInputStream(fin));
//        System.out.println(">>>>>>>>>>>>>stderr");
//        int c;
//        while ((c = reader.read()) >= 0) {
//            System.err.print((char) c);
//        }
//    }

}
