/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.recyclable.impl;

import junit.framework.Assert;
import org.junit.Test;
import org.spf4j.recyclable.SizedRecyclingSupplier;

/**
 *
 * @author zoly
 */
public class SizedThreadLocalRecyclingSupplierTest {


    @Test
    public void testSupplier() {
        SizedThreadLocalRecyclingSupplier<byte[]> supplier = ArraySuppliers.Bytes.SUPPLIER;

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
        Assert.assertEquals(8000, get.length);
        supplier.recycle(get);

        get = supplier.get(10000);
        Assert.assertEquals(10000, get.length);
        supplier.recycle(get);

        get = supplier.get(8000);
        get2 = supplier.get(8000);
        Assert.assertTrue(get.length == 10000 || get2.length == 10000);

    }

}
