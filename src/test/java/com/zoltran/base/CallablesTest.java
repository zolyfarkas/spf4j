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


package com.zoltran.base;

import java.util.concurrent.Callable;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author zoly
 */
public class CallablesTest
{
    
    public CallablesTest()
    {
    }
    

    /**
     * Test of executeWithRetry method, of class Callables.
     */
    @Test
    public void testExecuteWithRetry_4args_1() throws Exception
    {
        System.out.println("executeWithRetry");
        Integer result = Callables.executeWithRetry(new Callable<Integer> () {

            @Override
            public Integer call() throws Exception
            {
                return 1;
            }
        }, 3, 10, 10);
       Assert.assertEquals(1L, result.longValue());
    }
    /**
     * Test of executeWithRetry method, of class Callables.
     */
    @Test
    public void testExecuteWithRetry_4args_2() throws Exception
    {
        System.out.println("executeWithRetry");
        long startTime = System.currentTimeMillis();
        Integer result = Callables.executeWithRetry(new Callable<Integer> () {

            private int count;
            @Override
            public Integer call() throws Exception
            {
                count++;
                if (count <5) {
                    throw new RuntimeException("Aaaaaaaaaaa" + count);
                }
                    
                return 1;
            }
        }, 3, 10, 10);
        long elapsedTime = System.currentTimeMillis() - startTime;
       Assert.assertEquals(1L, result.longValue());
       Assert.assertTrue("Operation has to take at least 10 ms", elapsedTime > 10L);
    }
    
    /**
     * Test of executeWithRetry method, of class Callables.
     */
    @Test(expected=RuntimeException.class)
    public void testExecuteWithRetry_4args_3() throws Exception
    {
        System.out.println("executeWithRetry");
        Integer result = Callables.executeWithRetry(new Callable<Integer> () {

            private int count;
            @Override
            public Integer call() throws Exception
            {
                count++;
                throw new RuntimeException("Aaaaaaaaaaa" + count);
            }
        }, 3, 10, 10);
    }
    
}
