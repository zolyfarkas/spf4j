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
package org.spf4j.perf.aspects;

import org.spf4j.perf.impl.RecorderFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.perf.impl.ms.tsdb.TSDBMeasurementStore;

/**
 *
 * @author zoly
 */
public final class NetworkMonitorAspectTest {

    
    private final long startTime = System.currentTimeMillis();
     
    private volatile boolean terminated = false;

    private final CountDownLatch latch = new CountDownLatch(1);
    
    public void runServer() throws IOException {
        try (ServerSocket server = new ServerSocket(4321)) {
            while (!terminated) {
                Socket client = server.accept();
                latch.countDown();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(
                        client.getInputStream())); PrintWriter out = new PrintWriter(client.getOutputStream(),
                                true)) {
                    try {
                        while (true) {
                            String line = in.readLine();
                            if (line == null) {
                                break;
                            }
                            out.println(line);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Test of nioReadLong method, of class NetworkMonitorAspect.
     */
    @Test
    public void testNetworkUsageRecording() throws Exception {
        System.setProperty("perf.network.sampleTimeMillis", "1000");
        Thread t = new Thread(new AbstractRunnable() {

            @Override
            public void doRun() throws Exception {
                runServer();
            }
        }, "server");
        t.start();
        latch.await();
        for (int i = 0; i < 100; i++) {
            clientTest();
            Thread.sleep(100);
        }
        terminated = true;
        clientTest();
        t.join();
        
        System.out.println(((TSDBMeasurementStore) RecorderFactory.MEASUREMENT_STORE).generateCharts(startTime,
                System.currentTimeMillis(), 1200, 600));
    }


    private void clientTest() throws IOException {
        try (Socket socket = new Socket("localhost", 4321);
                PrintWriter out = new PrintWriter(socket.getOutputStream(),
                true); BufferedReader in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()))) {
            out.println("hello world");
            System.out.println(in.readLine());
        }
    }
}
