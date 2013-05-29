/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.perf.tsdb;

import org.spf4j.base.Pair;
import java.io.File;
import java.util.Arrays;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class TimeSeriesDatabaseTest {
    
    public TimeSeriesDatabaseTest() {
    }

    private static final String FILE_NAME = System.getProperty("java.io.tmpdir") + "/testdb.tsdb";
    
    /**
     * Test of close method, of class TimeSeriesDatabase.
     */
    @Test
    public void testWriteTSDB() throws Exception {
        System.out.println("testWriteTSDB");
        new File(FILE_NAME).delete();
        TimeSeriesDatabase instance = new TimeSeriesDatabase(FILE_NAME, new byte[] {});
        instance.addColumnGroup("gr1", new byte []{}, 5, new String[]{"a", "b"}, new byte [][] {});
        instance.write(System.currentTimeMillis(), "gr1", new long[] {0, 1});
        Thread.sleep(5);
        instance.write(System.currentTimeMillis(), "gr1", new long[] {1, 2});
        Thread.sleep(5);
        instance.write(System.currentTimeMillis(), "gr1", new long[] {3, 4});
        Thread.sleep(5);
        instance.addColumnGroup("gr2", new byte []{}, 5, new String[] {"a", "b"}, new byte [][] {});
        instance.write(System.currentTimeMillis(), "gr2",  new long[] {7, 8});
        instance.flush();
        
        instance.addColumnGroup("gr3", new byte []{}, 5, new String[] {"a", "b"}, new byte [][] {});
        instance.write(System.currentTimeMillis(), "gr3",  new long[] {7, 8});
        Thread.sleep(5);
        instance.write(System.currentTimeMillis(), "gr3",  new long[] {9, 10});
 
        System.out.println(instance.getColumnsInfo());
        Pair<long[], long[][]> readAll = instance.readAll("gr1");
        printValues(readAll);
        readAll = instance.readAll("gr2");
        printValues(readAll);
        
        instance.close();
        
        TimeSeriesDatabase instanceRead = new TimeSeriesDatabase(FILE_NAME, null);
        
        System.out.println(instanceRead.getColumnsInfo());
        Assert.assertEquals(3, instanceRead.getColumnsInfo().size());
        
        
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testWriteBadTSDB() throws Exception {
        System.out.println("testWriteBadTSDB");
        TimeSeriesDatabase instance = new TimeSeriesDatabase(System.getProperty("java.io.tmpdir")
                + "/testdb.tsdb", new byte[] {});
        instance.addColumnGroup("gr1", new byte []{}, 5, new String[]{"a", "b"}, new byte [][] {});
        instance.close();
    }

    private void printValues(final Pair<long[], long[][]> readAll) {
        long[] ts = readAll.getFirst();
        long[][] values = readAll.getSecond();
        for (int i = 0; i < ts.length; i++) {
            System.out.println(ts[i] + ":" + Arrays.toString(values[i]));
        }
    }
    

}
