/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.perf.tsdb;

import com.zoltran.base.Pair;
import gnu.trove.list.array.TLongArrayList;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

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
        TimeSeriesDatabase instance = new TimeSeriesDatabase(fileName, 5, new byte[] {});
        instance.addColumns("gr1", new String[]{ "a","b"});
        instance.write(System.currentTimeMillis(), new double[] {0.1, 0.2});
        Thread.sleep(5);
        instance.write(System.currentTimeMillis(), new double[] {1, 2});
        Thread.sleep(5);
        instance.write(System.currentTimeMillis(), new double[] {3, 4});
        Thread.sleep(5);
        instance.addColumns("gr2", new String[] {"a","b"});
        instance.write(System.currentTimeMillis(), new double[] {5, 6, 7, 8});
        instance.flush();
    
        System.out.println(instance.getColumns());
        Pair<TLongArrayList, List<double[]>> readAll = instance.readAll();
        TLongArrayList ts = readAll.getFirst();
        List<double[]> values = readAll.getSecond();
        for (int i = 0; i< ts.size(); i++ ) {
            System.out.println(ts.get(i) + ":" + Arrays.toString(values.get(i)));
        }
        
        instance.close();
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testWriteBadTSDB() throws Exception {
        System.out.println("testWriteTSDB");
        TimeSeriesDatabase instance = new TimeSeriesDatabase(System.getProperty("java.io.tmpdir")+ "/testdb.tsdb", 5, new byte[] {});
        instance.addColumns("gr1", new String[]{ "a","b"});
        instance.close();
    }
    

}
