/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
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
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.io.proxy;

import org.spf4j.io.tcp.proxy.ProxyClientHandler;
import org.spf4j.io.tcp.TcpServer;
import com.google.common.net.HostAndPort;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.ds.UpdateablePriorityQueue;
import org.spf4j.io.Streams;
import org.spf4j.io.tcp.ClientHandler;
import org.spf4j.io.tcp.DeadlineAction;
import org.spf4j.io.tcp.proxy.Sniffer;
import org.spf4j.io.tcp.proxy.SnifferFactory;

/**
 * https://unix.stackexchange.com/questions/17218/
 * how-long-is-a-tcp-local-socket-address-that-has-been-bound-unavailable-after-clo
 * https://stackoverflow.com/questions/13838256/java-closing-a-serversocket-and-opening-up-the-port
 *
 * @author zoly
 */
@SuppressFBWarnings({"SIC_INNER_SHOULD_BE_STATIC_ANON", "MDM_THREAD_YIELD"})
public class TcpServerTest {

  private static final Logger LOG = LoggerFactory.getLogger(TcpServerTest.class);

  private static final Logger SNIFFER_LOG = LoggerFactory.getLogger(TcpServerTest.class + ".SNIFFER");

  private static final String TEST_SITE = "localhost"; //"charizard.homeunix.net";

  private static final int TEST_PORT = 8080;

  private static HttpServer server;


  @BeforeClass
  public static void createHttpServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress(TEST_PORT), 0);
    server.createContext("/", new HttpHandler() {
      @Override
      public void handle(final HttpExchange he) throws IOException {
        Headers respHeaders = he.getResponseHeaders();
        respHeaders.add("testheader", "testValue");
        he.sendResponseHeaders(200, 0);
        OutputStream responseBody = he.getResponseBody();
        responseBody.write("Some Body".getBytes(StandardCharsets.UTF_8));
        responseBody.close();
      }
    });
    server.start();
  }

  public static void stopHttpServer() {
    server.stop(3);
  }



  private SnifferFactory printSnifferFactory = new SnifferFactory() {
    @Override
    public Sniffer get(final SocketChannel channel) {
      return new Sniffer() {


        private final CharsetDecoder asciiDecoder = StandardCharsets.US_ASCII.newDecoder();

        @Override
        public int received(final ByteBuffer data, final int nrBytes) {
          // Naive printout using ASCII
          if (nrBytes < 0) {
            SNIFFER_LOG.debug("EOF");
            return nrBytes;
          }
          ByteBuffer duplicate = data.duplicate();
          duplicate.position(data.position() - nrBytes);
          duplicate = duplicate.slice();
          duplicate.position(nrBytes);
          duplicate.flip();
          CharBuffer cb = CharBuffer.allocate((int) (asciiDecoder.maxCharsPerByte() * duplicate.limit()));
          asciiDecoder.decode(duplicate, cb, true);
          cb.flip();
          SNIFFER_LOG.debug(cb.toString());
          return nrBytes;
        }

      };
    }
  };

  @Test(timeout = 100000)
  public void testProxy() throws IOException, InterruptedException {
    ForkJoinPool pool = new ForkJoinPool(1024);
    try (TcpServer server = new TcpServer(pool,
            new ProxyClientHandler(HostAndPort.fromParts(TEST_SITE, TEST_PORT), printSnifferFactory,
                    printSnifferFactory, 10000, 5000),
            1976, 10)) {
      server.startAsync().awaitRunning();
      long start = System.currentTimeMillis();
      byte[] originalContent = readfromSite("http://" + TEST_SITE + ':' + TEST_PORT);
      long time1 = System.currentTimeMillis();
      byte[] proxiedContent = readfromSite("http://localhost:1976");
      long time2 = System.currentTimeMillis();
      LOG.debug("Direct = {}  ms, proxied = {}", (time1 - start), (time2 - time1));
      Assert.assertArrayEquals(originalContent, proxiedContent);
    }
  }

  @Test(timeout = 100000)
  public void testProxySimple() throws IOException, InterruptedException {
    ForkJoinPool pool = new ForkJoinPool(1024);

    try (TcpServer server = new TcpServer(pool,
            new ProxyClientHandler(
                    HostAndPort.fromParts(TEST_SITE, TEST_PORT), printSnifferFactory, printSnifferFactory, 10000, 5000),
            1977, 10)) {
      server.startAsync().awaitRunning();

      byte[] proxiedContent = readfromSite("http://localhost:1977");
      Assert.assertNotNull(proxiedContent);
    }
  }

  @Test(expected = java.net.SocketException.class, timeout = 60000)
  public void testTimeout() throws IOException, InterruptedException {
    String testSite = "10.10.10.10";
    ForkJoinPool pool = new ForkJoinPool(1024);
    try (TcpServer server = new TcpServer(pool,
            new ProxyClientHandler(HostAndPort.fromParts(testSite, 80), null, null, 10000, 5000),
            1978, 10)) {
      server.startAsync().awaitRunning();
      readfromSite("http://localhost:1978"); // results in a socket exception after 5 seconds
      Assert.fail("Should timeout");
    }
  }

  @Test(timeout = 100000)
  public void testRestart() throws IOException, InterruptedException, TimeoutException {
    ForkJoinPool pool = new ForkJoinPool(1024);
    try (TcpServer server = new TcpServer(pool,
            new ProxyClientHandler(HostAndPort.fromParts("bla", 80), null, null, 10000, 5000),
            1979, 10)) {
      server.startAsync().awaitRunning(10, TimeUnit.SECONDS);
      server.stopAsync().awaitTerminated(10, TimeUnit.SECONDS);
      server.startAsync().awaitRunning(60, TimeUnit.SECONDS);
      Assert.assertTrue(server.isRunning());
    }
  }

  @Test(expected = IOException.class, timeout = 10000)
  public void testRejectingServer() throws IOException, InterruptedException {
    String testSite = "localhost";
    ForkJoinPool pool = new ForkJoinPool(1024);
    try (TcpServer rejServer = new TcpServer(pool,
            new ClientHandler() {
      @Override
      public void handle(final Selector serverSelector, final SocketChannel clientChannel,
              final ExecutorService exec, final BlockingQueue<Runnable> tasksToRunBySelector,
              final UpdateablePriorityQueue<DeadlineAction> deadlineActions) throws IOException {
        clientChannel.configureBlocking(true);
        ByteBuffer allocate = ByteBuffer.allocate(1024);
        clientChannel.read(allocate); // read something
        try {
          Thread.sleep(100);
        } catch (InterruptedException ex) {
          throw new RuntimeException(ex);
        }
        allocate.flip();
        clientChannel.write(allocate);
        clientChannel.close();
      }
    }, 1980, 10)) {
      rejServer.startAsync().awaitRunning();

      try (TcpServer server = new TcpServer(pool,
              new ProxyClientHandler(HostAndPort.fromParts(testSite, 1980), null, null, 10000, 5000),
              1981, 10)) {
        server.startAsync().awaitRunning();
        byte[] readfromSite = readfromSite("http://localhost:1981");
        LOG.debug("Response: {}", new String(readfromSite, StandardCharsets.UTF_8)); //probably wrong charset assumtion
      }
    }
  }

  @Test(expected = java.net.SocketException.class, timeout = 10000)
  public void testKill() throws IOException, InterruptedException {
    String testSite = "10.10.10.10";
    ForkJoinPool pool = new ForkJoinPool(1024);
    try (TcpServer server = new TcpServer(pool,
            new ProxyClientHandler(HostAndPort.fromParts(testSite, 80), null, null, 10000, 10000),
            1982, 10)) {
      server.startAsync().awaitRunning();
      DefaultScheduler.INSTANCE.schedule(new AbstractRunnable(true) {
        @Override
        public void doRun() throws IOException {
          server.close();
        }
      }, 2, TimeUnit.SECONDS);
      readfromSite("http://localhost:1982"); // results in a socket exception after 5 seconds
      Assert.fail("Should timeout");
    }
  }

  private static byte[] readfromSite(final String siteUrl) throws IOException {
    URL url = new URL(siteUrl);
    URLConnection conn = url.openConnection();
    conn.setConnectTimeout(10000);
    conn.setReadTimeout(30000);
    InputStream stream = conn.getInputStream();
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    Streams.copy(stream, bos);
    return bos.toByteArray();
  }

}
