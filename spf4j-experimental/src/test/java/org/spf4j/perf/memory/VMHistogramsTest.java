
package org.spf4j.perf.memory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sun.jna.NativeLibrary;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.Test;
import org.spf4j.c.CLibrary;
import org.spf4j.c.CLibrary.FILE;

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
        FILE fp = CLibrary.LIBC.freopen("/tmp/testErr.txt", "w", stdout);
        byte [] last = generateGarbage();
        System.out.println("last size =" + last.length);
        CLibrary.LIBC.fclose(fp);
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
