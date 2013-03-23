/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.iomonitor;

import org.spf4j.perf.RecorderFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class NetworkMonitorAspectTest implements Runnable {

    public NetworkMonitorAspectTest() {
    }
    
    private final long startTime = System.currentTimeMillis();
     
    private volatile boolean terminated = false;

    public void runServer() throws IOException {
        ServerSocket server = new ServerSocket(4321);
        try {
            while (!terminated) {
                Socket client = server.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        client.getInputStream()));
                try {
                    PrintWriter out = new PrintWriter(client.getOutputStream(),
                            true);
                    try {
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
                    } finally {
                        out.close();
                    }
                } finally {
                    in.close();
                }
            }
        } finally {
            server.close();
        }
    }

    /**
     * Test of nioReadLong method, of class NetworkMonitorAspect.
     */
    @Test
    public void testNetworkUsageRecording() throws Exception {
        System.setProperty("perf.network.sampleTime", "1000");
        Thread t = new Thread(this, "server");
        t.start();

        for (int i = 0; i < 100; i++) {
            clientTest();
            Thread.sleep(100);
        }
        terminated = true;
        clientTest();
        t.join();
        
        System.out.println(RecorderFactory.TS_DATABASE.generateCharts(startTime, System.currentTimeMillis(), 1200, 600));
    }

    @Override
    public void run() {
        try {
            runServer();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void clientTest() throws IOException {
        Socket socket = new Socket("localhost", 4321);
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(),
                    true);
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                try {
                    out.println("hello world");
                    System.out.println(in.readLine());
                } finally {
                    in.close();
                }
            } finally {
                out.close();
            }
        } finally {
            socket.close();
        }
    }
}
