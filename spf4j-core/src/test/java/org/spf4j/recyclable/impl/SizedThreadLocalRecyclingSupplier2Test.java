
package org.spf4j.recyclable.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.base.ReferenceType;
import org.spf4j.recyclable.SizedRecyclingSupplier;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
public class SizedThreadLocalRecyclingSupplier2Test {


    @Test
    @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
    public void testSupplier() {
        SizedRecyclingSupplier<byte[]> supplier = new Powerof2ThreadLocalRecyclingSupplier<>(
                new SizedRecyclingSupplier.Factory<byte[]>() {

            @Override
            @SuppressFBWarnings("SUA_SUSPICIOUS_UNINITIALIZED_ARRAY")
            public byte[] create(final int size) {
                return new byte[size];
            }

            @Override
            public int size(final byte[] object) {
                return object.length;
            }
        }, ReferenceType.SOFT);

        byte[] get = supplier.get(8000);
        supplier.recycle(get);
        byte [] get2 = supplier.get(8000);
        supplier.recycle(get2);
        Assert.assertTrue(get == get2);

        get = supplier.get(8000);
        get2 = supplier.get(8000);
        Assert.assertTrue(get != get2);
        supplier.recycle(get);
        supplier.recycle(get2);

        get = supplier.get(4000);
        Assert.assertEquals(4096, get.length);
        supplier.recycle(get);
        get2 = supplier.get(4000);
        Assert.assertEquals(4096, get2.length);
        supplier.recycle(get2);
        Assert.assertTrue(get == get2);

    }


}
