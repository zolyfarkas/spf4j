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
package org.spf4j.jmx;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Future;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.ComparablePair;
import org.spf4j.base.Pair;
import org.spf4j.jmx.mappers.Spf4jOpenTypeMapper;
import org.spf4j.tsdb2.avro.ColumnDef;
import org.spf4j.tsdb2.avro.TableDef;
import org.spf4j.tsdb2.avro.Type;

/**
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings({"SE_BAD_FIELD_INNER_CLASS", "SIC_INNER_SHOULD_BE_STATIC_ANON"})
public class OpenTypeConverterTest {

  private static final Logger LOG = LoggerFactory.getLogger(OpenTypeConverterTest.class);

  private final Spf4jOpenTypeMapper conv = new Spf4jOpenTypeMapper();

  @Test
  public void testConverter() throws NotSerializableException {
    JMXBeanMapping mxBeanMapping = conv.get(File.class);
    Assert.assertNull(mxBeanMapping);
  }

  @Test
  public void testConverter2() throws NotSerializableException {
    JMXBeanMapping mxBeanMapping2 = conv.get(ColumnDef[].class);
    Assert.assertNotNull(mxBeanMapping2);
  }

  @Test
  public void testConverterPrimArray() throws OpenDataException, InvalidObjectException, NotSerializableException {
    JMXBeanMapping mxBeanMapping2 = conv.get(int[].class);
    Assert.assertNotNull(mxBeanMapping2);
    Object obj = mxBeanMapping2.toOpenValue(new int[]{1, 2, 3});
    Object fromOpenValue = mxBeanMapping2.fromOpenValue(obj);
    Assert.assertArrayEquals(new int[]{1, 2, 3}, (int[]) fromOpenValue);
  }

  @Test
  public void testConverterAvroArray() throws OpenDataException, NotSerializableException {
    JMXBeanMapping mxBeanMapping2 = conv.get(TableDef[].class);
    Assert.assertNotNull(mxBeanMapping2);
    TableDef[] defs = new TableDef[]{
      TableDef.newBuilder().setId(4).setDescription("bla").setName("name")
              .setSampleTime(10)
              .setColumns(Collections.singletonList(ColumnDef.newBuilder().setName("bla").setType(Type.LONG)
                      .setDescription("bla").setUnitOfMeasurement("um").build())).build()
    };
    Object toOpenValue = mxBeanMapping2.toOpenValue(defs);
    LOG.debug("Open value {} from {}", toOpenValue, defs);
  }

  @Test
  public void testConverterSet() throws OpenDataException, InvalidObjectException, NotSerializableException {
    JMXBeanMapping mxBeanMapping2 = conv.get((new TypeToken<Set<TableDef>>() {
    }).getType());
    Assert.assertNotNull(mxBeanMapping2);
    Object toOpenValue = mxBeanMapping2.toOpenValue(ImmutableSet.of(
            TableDef.newBuilder().setId(4).setDescription("bla").setName("name")
                    .setSampleTime(10)
                    .setColumns(Collections.singletonList(ColumnDef.newBuilder().setName("bla").setType(Type.LONG)
                            .setDescription("bla").setUnitOfMeasurement("um").build())).build()
    ));
    LOG.debug("OpenValue = {}", toOpenValue);
    Object fromOpenValue = mxBeanMapping2.fromOpenValue(toOpenValue);
    LOG.debug("Back to object = {}", fromOpenValue);
    Assert.assertTrue("must be set, not " + fromOpenValue.getClass(), fromOpenValue instanceof Set);
  }

  @Test
  public void testConverterIterable() throws OpenDataException, InvalidObjectException, NotSerializableException {
    JMXBeanMapping mxBeanMapping2 = conv.get((new TypeToken<Iterable<TableDef>>() {
    }).getType());
    Assert.assertNotNull(mxBeanMapping2);
    Object toOpenValue = mxBeanMapping2.toOpenValue(ImmutableSet.of(
            TableDef.newBuilder().setId(4).setDescription("bla").setName("name")
                    .setSampleTime(10)
                    .setColumns(Collections.singletonList(ColumnDef.newBuilder().setName("bla").setType(Type.LONG)
                            .setDescription("bla").setUnitOfMeasurement("um").build())).build()
    ));
    LOG.debug("To open value: {}", toOpenValue);
    Object fromOpenValue = mxBeanMapping2.fromOpenValue(toOpenValue);
    LOG.debug("Back to object: {}", fromOpenValue);
    Assert.assertTrue("must be Iterable, not " + fromOpenValue.getClass(), fromOpenValue instanceof Iterable);
  }

  @Test
  public void testConverterList() throws OpenDataException, InvalidObjectException, NotSerializableException {
    JMXBeanMapping mxBeanMapping2 = conv.get((new TypeToken<List<ColumnDef>>() {
    }).getType());
    Assert.assertNotNull(mxBeanMapping2);
    Object ov = mxBeanMapping2.toOpenValue(Collections.singletonList(
            ColumnDef.newBuilder().setName("bla").setType(Type.LONG)
            .setDescription("bla").setUnitOfMeasurement("um").build()));
    LOG.debug("OpenValue = {}", ov);
    mxBeanMapping2.fromOpenValue(ov);
  }

  @Test
  public void testConverterFuture() throws OpenDataException, InvalidObjectException, NotSerializableException {
    JMXBeanMapping map = conv.get((new TypeToken<Future<Integer>>() {
    }).getType());
    Assert.assertNull(map);
  }

  @Test
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public void testConverterMap() throws OpenDataException, InvalidObjectException, NotSerializableException {
    JMXBeanMapping mxBeanMapping2 = conv.get((new TypeToken<Map<String, ColumnDef>>() {
    }).getType());
    Assert.assertNotNull(mxBeanMapping2);
    Object ov = mxBeanMapping2.toOpenValue(
            ImmutableMap.of("k1", ColumnDef.newBuilder().setName("bla").setType(Type.LONG)
            .setDescription("bla").setUnitOfMeasurement("um").build(),
            "K2",
            ColumnDef.newBuilder().setName("bla2").setType(Type.LONG)
                    .setDescription("bla").setUnitOfMeasurement("um").build()));
    LOG.debug("OpenValue = {}", ov);
    mxBeanMapping2.fromOpenValue(ov);
  }

  @Test
  @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
  public void testConverterProperties() throws OpenDataException, InvalidObjectException, NotSerializableException {
    JMXBeanMapping mxBeanMapping2 = conv.get(Properties.class);
    Assert.assertNotNull(mxBeanMapping2);
    Properties props = new Properties();
    props.setProperty("K", "V");
    Object ov = mxBeanMapping2.toOpenValue(props);
    LOG.debug("Open Value = {}", ov);
    Properties properties = (Properties) mxBeanMapping2.fromOpenValue(ov);
    Assert.assertEquals(props, properties);
  }

  @Test
  public void testConverterMapStrObject() throws OpenDataException, InvalidObjectException, NotSerializableException {
    JMXBeanMapping mxBeanMapping2 = conv.get((new TypeToken<Map<String, ? extends Serializable>>() {
    }).getType());
    Assert.assertNull(mxBeanMapping2);
  }

  @Test
  public void testConverterMapStrObject2() throws OpenDataException, InvalidObjectException, NotSerializableException {
    JMXBeanMapping get = conv.get((new TypeToken<Map<String, Object>>() {
    }).getType());
    Assert.assertNull(get);
  }

  @Test
  public void testConverterMapStrObject3() throws OpenDataException, InvalidObjectException, NotSerializableException {
    JMXBeanMapping mxBeanMapping2 = conv.get(Map.class);
    Assert.assertNull(mxBeanMapping2);
  }

  @Test
  public void testComparablePair() throws OpenDataException, InvalidObjectException, NotSerializableException {
    JMXBeanMapping mxBeanMapping2 = conv.get((new TypeToken<ComparablePair<Integer, TableDef>>() {
    }).getType());
    Assert.assertNotNull(mxBeanMapping2);
    Object ov = mxBeanMapping2.toOpenValue(
            Pair.of(3, TableDef.newBuilder().setId(4).setDescription("bla").setName("name")
            .setSampleTime(10)
            .setColumns(Collections.singletonList(ColumnDef.newBuilder().setName("bla").setType(Type.LONG)
                    .setDescription("bla").setUnitOfMeasurement("um").build())).build()));
    ComparablePair<Integer, TableDef> pair
            = (ComparablePair<Integer, TableDef>) mxBeanMapping2.fromOpenValue(ov);
    Assert.assertEquals(3L, pair.getFirst().longValue());
  }

  @Test
  public void testListSerializablePair() throws OpenDataException, InvalidObjectException, NotSerializableException {
    JMXBeanMapping mxBeanMapping2
            = conv.get((new TypeToken<List<ComparablePair<Integer, TableDef>>>() {
            }).getType());
    Assert.assertNotNull(mxBeanMapping2);
    Object ov = mxBeanMapping2.toOpenValue(
            Collections.singletonList(Pair.of(3, TableDef.newBuilder().setId(4).setDescription("bla").setName("name")
            .setSampleTime(10)
            .setColumns(Collections.singletonList(ColumnDef.newBuilder().setName("bla").setType(Type.LONG)
                    .setDescription("bla").setUnitOfMeasurement("um").build())).build())));
    List<ComparablePair<Integer, TableDef>> res
            = (List<ComparablePair<Integer, TableDef>>) mxBeanMapping2.fromOpenValue(ov);
    ComparablePair<Integer, TableDef> pair
            = (ComparablePair<Integer, TableDef>) res.get(0);
    Assert.assertEquals(3L, pair.getFirst().longValue());

  }

  @Test(expected = NotSerializableException.class)
  public void testConverterRecursiveData() throws OpenDataException, InvalidObjectException, NotSerializableException {
    JMXBeanMapping get = conv.get(RecursiveTestBean.class);
    LOG.debug("Mapping = {}", get);
  }

  @Test
  public void testConverterRecursiveAvro() throws OpenDataException, InvalidObjectException, NotSerializableException {
    JMXBeanMapping get = conv.get(org.spf4j.test.avro.SampleNode.class);
    Assert.assertNull(get);
  }

  @Test
  public void testConverterRecursiveAvro2() throws OpenDataException, InvalidObjectException, NotSerializableException {
    JMXBeanMapping get = conv.get(org.spf4j.test2.avro.SampleNode.class);
    Assert.assertNull(get);
  }

  @Test
  public void testConverterFile() throws OpenDataException, InvalidObjectException, NotSerializableException {
    JMXBeanMapping get = conv.get(File.class);
    Assert.assertNull(get);
  }


  @Test
  public void testJdkLocalDate() throws OpenDataException, InvalidObjectException, NotSerializableException {
    JMXBeanMapping get = conv.get(java.time.LocalDate.class);
    Assert.assertNotNull(get);
    java.time.LocalDate now = java.time.LocalDate.now();
    Object ov = get.toOpenValue(now);
    LOG.debug("Open Value = {}", ov);
    Assert.assertTrue(ov instanceof CompositeData);
  }

  @Test
  public void testCompositeData() throws OpenDataException, InvalidObjectException, NotSerializableException {
    JMXBeanMapping get = conv.get(CompositeData.class);
    Assert.assertNotNull(get);
  }

  @Test
  public void testCompositeDataSupport() throws OpenDataException, InvalidObjectException, NotSerializableException {
    JMXBeanMapping get = conv.get(CompositeDataSupport.class);
    Assert.assertNotNull(get);
  }

}
