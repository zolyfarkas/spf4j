
package org.spf4j.perf.memory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class VMHistogramsTest {


    private static final Cache<byte[], byte[]> CACHE = CacheBuilder.newBuilder().softValues().weakKeys().build();

    @Test
    public void testMemoryUsage() throws FileNotFoundException, IOException {

        byte [] last = new byte [0];
        for (int i = 0; i < 1000; i++) {
            last = new byte[1024000];
            for (int j = 0; j < 100; j++) {
                CACHE.put(last, last);
                last[j] = (byte) j;
            }
        }
        System.out.println("last size =" + last.length);
        System.out.println("histogram  =" + VMHistograms.getHeapInstanceCountsHistogram());

    }

}
