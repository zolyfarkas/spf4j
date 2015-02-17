
package org.spf4j.recyclable.impl;

import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.recyclable.ObjectBorrowException;
import org.spf4j.recyclable.ObjectCreationException;
import org.spf4j.recyclable.ObjectDisposeException;
import org.spf4j.recyclable.RecyclingSupplier;

/**
 *
 * @author zoly
 */
public class SharingObjectPoolTest {


    @Test
    public void testSharingPool() throws ObjectCreationException, ObjectBorrowException, InterruptedException, TimeoutException {

        SharingObjectPool<String> pool = new SharingObjectPool<>(new RecyclingSupplier.Factory<String>(){

            int i = 0;

            @Override
            public String create() throws ObjectCreationException {
                return "O" + (i++);
            }

            @Override
            public void dispose(String object) throws ObjectDisposeException {
            }

            @Override
            public boolean validate(String object, Exception e) throws Exception {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        }, 0, 2);

        String obj = pool.get();
        Assert.assertEquals("O0", obj);
        String obj2 = pool.get();
        Assert.assertEquals("O1", obj2);
        pool.recycle(obj);
        String obj3 = pool.get();
        Assert.assertEquals("O0", obj3);

    }

}
