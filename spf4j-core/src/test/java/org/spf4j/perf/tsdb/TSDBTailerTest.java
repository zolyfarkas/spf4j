/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.perf.tsdb;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import junit.framework.Assert;
import org.junit.Test;
import org.spf4j.base.MutableHolder;
import org.spf4j.concurrent.DefaultExecutor;
import org.spf4j.concurrent.FileBasedLockTest;

/**
 *
 * @author zoly
 */
public final class TSDBTailerTest {

    private static final String FILE_NAME = System.getProperty("java.io.tmpdir") + "/testdb.tsdb";

    private volatile boolean finish = false;

    /**
     * Test of close method, of class TimeSeriesDatabase.
     */
    @Test
    public void testWriteTSDB() throws Exception {
        System.out.println("testWriteTSDB");
        if (new File(FILE_NAME).delete()) {
            System.out.println("existing tsdb file deleted");
        }
        final TimeSeriesDatabase instance = new TimeSeriesDatabase(FILE_NAME, new byte[]{});
        Future<Integer> tailFut = DefaultExecutor.INSTANCE.submit(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                return doTail(instance);
            }

        });

        instance.addTSTable("gr1", new byte[]{}, 5, new String[]{"a", "b"}, new byte[][]{});
        instance.write(System.currentTimeMillis(), "gr1", new long[]{0, 1});
        Thread.sleep(5);
        instance.write(System.currentTimeMillis(), "gr1", new long[]{1, 2});
        Thread.sleep(5);
        instance.write(System.currentTimeMillis(), "gr1", new long[]{3, 4});
        Thread.sleep(5);
        instance.addTSTable("gr2", new byte[]{}, 5, new String[]{"a", "b"}, new byte[][]{});
        instance.write(System.currentTimeMillis(), "gr2", new long[]{7, 8});
        instance.flush();

        instance.addTSTable("gr3", new byte[]{}, 5, new String[]{"a", "b"}, new byte[][]{});
        instance.write(System.currentTimeMillis(), "gr3", new long[]{7, 8});
        Thread.sleep(5);
        instance.write(System.currentTimeMillis(), "gr3", new long[]{9, 10});
        instance.flush();
        Thread.sleep(1000);
        finish = true;
        int result = tailFut.get();
        Assert.assertEquals(6, result);
    }

    @Test
    public void externalTailerTest() throws InterruptedException, ExecutionException {
        final String classPath = ManagementFactory.getRuntimeMXBean().getClassPath();
        final String jvmPath
                = System.getProperties().getProperty("java.home")
                + File.separatorChar + "bin" + File.separatorChar + "java";

        Future<Integer> result = DefaultExecutor.INSTANCE.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws IOException, InterruptedException {
                String[] command = new String[]{jvmPath, "-cp", classPath, TSDBTailerTest.class.getName(),
                    FILE_NAME};
                System.out.println("Running " + Arrays.toString(command));
                Process proc = Runtime.getRuntime().exec(command);
                return proc.waitFor();
            }
        });
        Assert.assertEquals(6, (int) result.get());

    }

    private Integer doTail(final TimeSeriesDatabase instance) throws IOException {
        final MutableHolder<Integer> counter = new MutableHolder<>(0);
        instance.tail(1, 0, new TSDataHandler() {

            @Override
            public void newTable(final String tableName, final String[] columnNames) {
                System.out.println("New Table: " + tableName
                        + " columns: " + Arrays.toString(columnNames));
            }

            @Override
            public void newData(final String tableName, final TimeSeries data) {
                System.out.println("Table " + tableName + " - " + data);
                counter.setValue(counter.getValue() + data.getTimeStamps().length);
            }

            @Override
            public boolean finish() {
                return finish;
            }
        });
        return counter.getValue();
    }

    public static void main(final String[] parameters) throws IOException {
        final MutableHolder<Integer> counter = new MutableHolder<>(0);
        TimeSeriesDatabase tsdb = new TimeSeriesDatabase(parameters[0]);
        tsdb.tail(1, 0, new TSDataHandler() {
            
            private int count = 0;

            @Override
            public void newTable(final String tableName, final String[] columnNames) {
                System.out.println("New Table: " + tableName
                        + " columns: " + Arrays.toString(columnNames));
            }

            @Override
            public void newData(final String tableName, final TimeSeries data) {
                System.out.println("Table " + tableName + " - " + data);
                counter.setValue(counter.getValue() + data.getTimeStamps().length);
            }

            @Override
            public boolean finish() {
                return count++ > 2000;
            }
        });
        System.exit(counter.getValue());
    }

}
