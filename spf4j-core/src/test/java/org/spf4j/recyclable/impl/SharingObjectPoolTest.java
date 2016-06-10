
package org.spf4j.recyclable.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.recyclable.ObjectBorrowException;
import org.spf4j.recyclable.ObjectCreationException;
import org.spf4j.recyclable.RecyclingSupplier;

/**
 *
 * @author zoly
 */
public class SharingObjectPoolTest {


    @Test
    @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
    public void testSharingPool()
            throws ObjectCreationException, ObjectBorrowException, InterruptedException, TimeoutException {

        SharingObjectPool<String> pool = createTestPool();

        String obj = pool.get();
        Assert.assertEquals("O0", obj);
        String obj2 = pool.get();
        Assert.assertEquals("O1", obj2);
        pool.recycle(obj);
        String obj3 = pool.get();
        Assert.assertEquals("O0", obj3);

    }

    @Test
    @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
    public void testSharingPool2()
            throws ObjectCreationException, ObjectBorrowException, InterruptedException, TimeoutException {

        SharingObjectPool<String> pool = createTestPool();

        String obj = pool.get();
        Assert.assertEquals("O0", obj);
        pool.recycle(obj);
        String obj2 = pool.get();
        Assert.assertEquals("O0", obj2);
    }


    @SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
    public SharingObjectPool<String> createTestPool() throws ObjectCreationException {
        return new SharingObjectPool<>(new RecyclingSupplier.Factory<String>(){

            int i = 0;

            @Override
            public String create() {
                return "O" + (i++);
            }

            @Override
            public void dispose(String object) {
            }

            @Override
            public boolean validate(String object, Exception e) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }, 0, 4);
    }

}
