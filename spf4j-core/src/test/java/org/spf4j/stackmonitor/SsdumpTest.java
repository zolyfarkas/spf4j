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
package org.spf4j.stackmonitor;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spf4j.ds.Traversals;
import org.spf4j.ds.Graph;
import org.spf4j.ssdump2.Converter;

public final class SsdumpTest {

    @BeforeClass
    public static void init() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(final Thread t, final Throwable e) {
                StringWriter strw = new StringWriter();
                e.printStackTrace(new PrintWriter(strw));
                Assert.fail("Got Exception: " + strw);
            }
        });
    }

    @Test
    public void testProto() throws InterruptedException, IOException, ExecutionException, TimeoutException  {

        Sampler sampler = new Sampler(1);
        sampler.registerJmx();
        sampler.start();
        MonitorTest.main(new String[]{});
        final File serializedFile = File.createTempFile("stackSample", ".samp");
        sampler.getStackCollector().applyOnSamples((final SampleNode f) -> {
          if (f != null) {
            try {
              Converter.save(serializedFile, f);
            } catch (IOException ex) {
              throw new RuntimeException(ex);
            }

          }
          return f;
        });
        sampler.stop();
        final SampleNode samples = Converter.load(serializedFile);
        Graph<Method, SampleNode.InvocationCount> graph = SampleNode.toGraph(samples);
        Traversals.traverse(graph, Method.ROOT,
                (final Method vertex, final Map<SampleNode.InvocationCount, Method> edges) -> {
          System.out.println("Method: " + vertex + " from " + edges);
        }, true);
        Assert.assertNotNull(graph);

    }
}
