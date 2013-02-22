/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.perf.tsdb;

import com.zoltran.base.Pair;
import java.io.File;
import java.util.Arrays;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class TimeSeriesDatabaseTest {
    
    public TimeSeriesDatabaseTest() {
    }

    private static final String fileName = System.getProperty("java.io.tmpdir")+ "/testdb.tsdb";
    
    /**
     * Test of close method, of class TimeSeriesDatabase.
     */
    @Test
    public void testWriteTSDB() throws Exception {
        System.out.println("testWriteTSDB");
        new File(fileName).delete();
        TimeSeriesDatabase instance = new TimeSeriesDatabase(fileName, new byte[] {});
        instance.addColumnGroup("gr1", 5, new String[]{ "a","b"}, new byte [][] {});
        instance.write(System.currentTimeMillis(), "gr1", new long[] {0, 1});
        Thread.sleep(5);
        instance.write(System.currentTimeMillis(), "gr1", new long[] {1, 2});
        Thread.sleep(5);
        instance.write(System.currentTimeMillis(), "gr1", new long[] {3, 4});
        Thread.sleep(5);
        instance.addColumnGroup("gr2", 5, new String[] {"a","b"}, new byte [][] {});
        instance.write(System.currentTimeMillis(), "gr2",  new long[] { 7, 8});
        instance.flush();
    
        System.out.println(instance.getColumnsInfo());
        Pair<long[], long[][]> readAll = instance.readAll("gr1");
        printValues(readAll);
        readAll = instance.readAll("gr2");
        printValues(readAll);
        
        instance.close();
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testWriteBadTSDB() throws Exception {
        System.out.println("testWriteTSDB");
        TimeSeriesDatabase instance = new TimeSeriesDatabase(System.getProperty("java.io.tmpdir")+ "/testdb.tsdb", new byte[] {});
        instance.addColumnGroup("gr1", 5, new String[]{ "a","b"}, new byte [][] {});
        instance.close();
    }

    private void printValues(Pair<long[], long[][]> readAll) {
        long[] ts = readAll.getFirst();
        long[][] values = readAll.getSecond();
        for (int i = 0; i< ts.length; i++ ) {
            System.out.println(ts[i] + ":" + Arrays.toString(values[i]));
        }
    }
    

}
