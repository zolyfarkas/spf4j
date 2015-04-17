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

import com.google.common.base.Function;
import com.google.protobuf.CodedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spf4j.ds.Traversals;
import org.spf4j.ds.Graph;
import org.spf4j.stackmonitor.proto.Converter;
import org.spf4j.stackmonitor.proto.gen.ProtoSampleNodes;

public final class ProtoTest {

    @BeforeClass
    public static void init() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(final Thread t, final Throwable e) {
                StringWriter strw = new StringWriter();
                e.printStackTrace(new PrintWriter(strw));
                Assert.fail("Got Exception: " + strw.toString());
            }
        });
    }

    @Test
    public void testProto() throws InterruptedException, MalformedObjectNameException,
            InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, IOException, ExecutionException, TimeoutException {

        Sampler sampler = new Sampler(1);
        sampler.registerJmx();
        sampler.start();
        MonitorTest.main(new String[]{});
        String serializedFile = File.createTempFile("stackSample", ".samp").getPath();
        final FileOutputStream os = new FileOutputStream(serializedFile);
        try {
            sampler.getStackCollector().applyOnSamples(new Function<SampleNode, SampleNode>() {
                @Override
                public SampleNode apply(final SampleNode f) {
                    try {
                        Converter.fromSampleNodeToProto(f).writeTo(os);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    return f;
                }
            });
        } finally {
            os.close();
        }
        sampler.stop();
        Sampler anotherOne = new Sampler(100);
        FileInputStream fis = new FileInputStream(serializedFile);
        try {
            final CodedInputStream is = CodedInputStream.newInstance(fis);
            is.setRecursionLimit(Short.MAX_VALUE);
            final SampleNode samples = Converter.fromProtoToSampleNode(ProtoSampleNodes.SampleNode.parseFrom(is));
            anotherOne.getStackCollector().applyOnSamples(new Function<SampleNode, SampleNode>() {
                @Override
                public SampleNode apply(final SampleNode f) {
                    return samples;
                }
            });
            Graph<Method, SampleNode.InvocationCount> graph = SampleNode.toGraph(samples);
            Traversals.traverse(graph, Method.ROOT,
                    new Traversals.TraversalCallback<Method, SampleNode.InvocationCount>() {

                @Override
                public void handle(final Method vertex, final Map<SampleNode.InvocationCount, Method> edges) {
                    System.out.println("Method: " + vertex + " from " + edges);
                }

            }, true);
        } finally {
            fis.close();
        }
        String report = anotherOne.dumpToFile();
        System.out.println(report);
    }
}
