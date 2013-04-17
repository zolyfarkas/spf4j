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
import java.io.*;
import javax.management.*;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spf4j.stackmonitor.proto.Converter;
import org.spf4j.stackmonitor.proto.gen.ProtoSampleNodes;

public class ProtoTest {

    @BeforeClass
    public static void init() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                StringWriter strw = new StringWriter();
                e.printStackTrace(new PrintWriter(strw));
                Assert.fail("Got Exception: " + strw.toString());
            }
        });
    }
    

    
    @Test
    public void testProto() throws InterruptedException, MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, IOException {
        
        Sampler sampler = new Sampler(1);
        sampler.registerJmx();
        sampler.start();
        MonitorTest.main(new String [] {});
        String serializedFile = File.createTempFile("stackSample", ".samp").getPath();
        final FileOutputStream os = new FileOutputStream(serializedFile);
        try  {
            sampler.getStackCollector().applyOnSamples(new Function<SampleNode, SampleNode> () {

                @Override
                public SampleNode apply(SampleNode f) {
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
            is.setRecursionLimit(1024);
            anotherOne.getStackCollector().applyOnSamples(new Function<SampleNode, SampleNode>() {

                @Override
                public SampleNode apply(SampleNode f) {
                    try {
                        return Converter.fromProtoToSampleNode( ProtoSampleNodes.SampleNode.parseFrom(is) );
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }

            });     
        } finally {
           fis.close(); 
        }
        String report = File.createTempFile("stackSample", ".html").getPath();
        anotherOne.generateHtmlMonitorReport(report, 1000, 25);
        System.out.println(report);    
    }
    
    
    
    
}
