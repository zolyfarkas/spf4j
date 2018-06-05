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
package org.spf4j.perf.impl;

import java.io.IOException;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.jmx.Client;
import org.spf4j.perf.CloseableMeasurementRecorder;
import org.spf4j.perf.CloseableMeasurementRecorderSource;
import org.spf4j.perf.MeasurementRecorder;

/**
 *
 * @author zoly
 */
public class RecorderFactoryTest {

  @Test
  public void testRecorderFactory() throws InterruptedException, IOException,
          InstanceNotFoundException, MBeanException, AttributeNotFoundException, ReflectionException {
    CloseableMeasurementRecorder rec = RecorderFactory.createScalableQuantizedRecorder2(RecorderFactoryTest.class,
            "ms", 100000000, 10, 0, 6, 10);
    rec.record(1);
    int sum = 1;
    for (int i = 0; i < 10; i++) {
      rec.record(i);
      sum += i;
    }
    String ret3 = (String) Client.getAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
            "org.spf4j.perf.recorders", "class_" + RecorderFactoryTest.class.getName(), "measurementsAsString");
    Assert.assertThat(ret3, Matchers.containsString(sum + "," + 11));
    rec.close();
  }

  @Test
  public void testRecorderFactory2() throws InterruptedException, IOException,
          InstanceNotFoundException, MBeanException, AttributeNotFoundException, ReflectionException {
    CloseableMeasurementRecorder rec = RecorderFactory.createScalableQuantizedRecorder2(RecorderFactoryTest.class,
            "ms", 100000000, 10, 0, 6, 10);
    CompositeData ret3 = (CompositeData) Client.getAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
            "org.spf4j.perf.recorders", "class_" + RecorderFactoryTest.class.getName(), "measurements");
    Assert.assertNull(ret3);
    rec.close();
  }

  private static final class RsTest {

  }

  @Test
  public void testRecorderFactoryDyna() throws InterruptedException, IOException,
          InstanceNotFoundException, MBeanException, AttributeNotFoundException, ReflectionException {
    CloseableMeasurementRecorderSource rec = RecorderFactory.createScalableQuantizedRecorderSource2(RsTest.class,
            "ms", 100000000, 10, 0, 6, 10);
    MeasurementRecorder recorder = rec.getRecorder("test");
    recorder.record(1);
    int sum = 1;
    for (int i = 0; i < 10; i++) {
      recorder.record(i);
      sum += i;
    }
    String ret3 = (String) Client.getAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
            "org.spf4j.perf.recorders", "class_" + RecorderFactoryTest.class.getName() + "_RsTest",
            "measurementsAsString");
    Assert.assertThat(ret3, Matchers.containsString("test," + sum + "," + 11));
    rec.close();
  }

}
