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
package org.spf4j.perf.impl;

import org.spf4j.perf.impl.ms.graphite.GraphiteUdpStore;
import com.google.common.base.Charsets;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.Future;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.perf.MeasurementRecorder;
import org.spf4j.recyclable.ObjectCreationException;

/**
 *
 * @author zoly
 */
public final class GraphiteUdpStoreTest {

    static {
        File tsdb;
        File tsdbtxt;
        try {
            tsdb = File.createTempFile("ttt", "tsdb");
            tsdbtxt = File.createTempFile("ttt", "tsdbtxt");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        System.setProperty("perf.ms.config",
                "TSDB@" + tsdb.getAbsolutePath() + "," + "TSDB_TXT@" + tsdbtxt.getAbsolutePath()
                        + ",GRAPHITE_UDP@127.0.0.1:1976");
    }
    
    @Test
    public void testGraphiteUdpStore() throws IOException, ObjectCreationException {

        final GraphiteUdpStore store = new GraphiteUdpStore("127.0.0.1", 1976);
        store.saveMeasurements(new EntityMeasurementsInfoImpl("bla", "ms",
                new String[]{"val1", "val2", "val3"},
                new String[]{"ms", "ms", "ms"}),
                System.currentTimeMillis(), 0, 1L, 2L, 3L);
        System.out.println("measurements sent");
    }
    
    
        @Test
    public void testStore() throws InterruptedException {
        
        MeasurementRecorder recorder = RecorderFactory.createScalableQuantizedRecorder("test measurement",
                "ms", 1000, 10, 0, 6, 10);
        
        for (int i = 0; i < 100; i++) {
            recorder.record(i);
            Thread.sleep(100);
        }

    }
    

    private static volatile boolean terminated = false;
    private static volatile Future<?> server;
    
    @BeforeClass
    public static void runUdpServer() throws IOException {
        
        server = org.spf4j.concurrent.DefaultExecutor.INSTANCE.submit(new AbstractRunnable(true) {

            @Override
            public void doRun() throws Exception {
                DatagramChannel channel = DatagramChannel.open();
                final InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 1976);
                channel.socket().bind(inetSocketAddress);
                ByteBuffer bb = ByteBuffer.allocate(512);
                while (!terminated) {
                    bb.rewind();
                    channel.receive(bb);
                    byte[] rba = new byte[bb.position()];
                    bb.rewind();
                    bb.get(rba);
                    System.out.println("Received = " + new String(rba, Charsets.UTF_8));
                }
            }
        });

    }
    
    @AfterClass
    public static void stopUdpServer() {
        terminated = true;
        server.cancel(true);
    }
    

}
