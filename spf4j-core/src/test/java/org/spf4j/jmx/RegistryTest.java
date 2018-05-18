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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.JMException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.Runtime.Jmx;
import org.spf4j.base.Throwables;
import org.spf4j.tsdb2.avro.ColumnDef;
import org.spf4j.tsdb2.avro.TableDef;
import org.spf4j.tsdb2.avro.Type;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
public final class RegistryTest {

  private static final Logger LOG = LoggerFactory.getLogger(RegistryTest.class);


  public static final class JmxTest extends PropertySource {

    private volatile String stringVal;

    private volatile double doubleVal;

    private volatile boolean booleanFlag;

    private final String[][] matrix = {{"a", "b"}, {"c", "d"}};

    private volatile TestEnum enumVal = TestEnum.VAL2;

    private final TestBean bean = new TestBean(3, "bla");

    @JmxExport
    public String[][] getMatrix() {
      return matrix.clone();
    }

    private final String[] array = {"a", "b"};

    @JmxExport
    public String[] getArray() {
      return array.clone();
    }

    @JmxExport
    public String getStringVal() {
      return stringVal;
    }

    @JmxExport
    public double getDoubleVal() {
      return doubleVal;
    }

    @JmxExport
    public boolean isBooleanFlag() {
      return booleanFlag;
    }

    @JmxExport
    public Boolean isBooleanFlag2() {
      return booleanFlag;
    }

    @JmxExport
    public void setStringVal(final String stringVal) {
      this.stringVal = stringVal;
    }

    @JmxExport
    public void setBooleanFlag(final boolean booleanFlag) {
      this.booleanFlag = booleanFlag;
    }

    public void setDoubleVal(final double doubleVal) {
      this.doubleVal = doubleVal;
    }

    @JmxExport
    public TestEnum getEnumVal() {
      return enumVal;
    }

    @JmxExport
    public void setEnumVal(final TestEnum enumVal) {
      this.enumVal = enumVal;
    }

    @JmxExport
    public TestBean getBean() {
      return bean;
    }

    @JmxExport
    @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    public File getFile() {
      return new File("/tmp");
    }

    @JmxExport
    public String getProperty(final String name) {
      return "bla";
    }

    @JmxExport
    public void setProperty(final String name, final String value) {
      //do nothing
    }

    @JmxExport
    public ColumnDef getColumnDef() {
      return ColumnDef.newBuilder().setName("bla").setType(Type.LONG)
              .setDescription("bla").setUnitOfMeasurement("um").build();
    }

    @JmxExport
    public TableDef getTableDef() {
      return TableDef.newBuilder().setId(4).setDescription("bla").setName("name")
              .setSampleTime(10)
              .setColumns(Collections.singletonList(ColumnDef.newBuilder().setName("bla").setType(Type.LONG)
                      .setDescription("bla").setUnitOfMeasurement("um").build())).build();

    }

    @JmxExport
    public ColumnDef getColumnDef(final String id) {
      return ColumnDef.newBuilder().setName(id).setType(Type.LONG)
              .setDescription("bla").setUnitOfMeasurement("um").build();
    }

    @JmxExport
    public ColumnDef echo(final ColumnDef id) {
      return id;
    }

    @JmxExport
    public List<ColumnDef> echoX(final List<ColumnDef> id) {
      return id;
    }

    @JmxExport
    @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
    public List<ColumnDef> getListAttr() {
      return Arrays.asList(ColumnDef.newBuilder().setName("bla").setType(Type.LONG)
              .setDescription("bla").setUnitOfMeasurement("um").build(),
              ColumnDef.newBuilder().setName("bla2").setType(Type.LONG)
                      .setDescription("bla").setUnitOfMeasurement("um").build());
    }

    @JmxExport
    @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
    public Map<String, ColumnDef> getMapAttr() {
      return ImmutableMap.of("k1", ColumnDef.newBuilder().setName("bla").setType(Type.LONG)
              .setDescription("bla").setUnitOfMeasurement("um").build(),
              "K2",
              ColumnDef.newBuilder().setName("bla2").setType(Type.LONG)
                      .setDescription("bla").setUnitOfMeasurement("um").build());
    }

    @JmxExport
    public Map<String, ColumnDef> echoXY(final Map<String, ColumnDef> id) {
      return id;
    }

    @JmxExport(value = "echo2", description = "bla bla bla")
    public File echo(final File id) {
      return id;
    }

    @JmxExport
    public void print(final int nr, final boolean flag, final String bla, final ColumnDef id) {
      LOG.info("{}, {}, {}, {}", bla, nr, flag, id);
    }

  }

  public static final class JmxTest2 {

    private static volatile String testStr;

    private volatile String stringVal;

    @JmxExport("stringVal2")
    public String getStringVal() {
      return stringVal;
    }

    @JmxExport("stringVal2")
    public void setStringVal(final String stringVal) {
      this.stringVal = stringVal;
    }

    @JmxExport
    public static String getTestStr() {
      return testStr;
    }

    public static void setTestStr(final String testStr) {
      JmxTest2.testStr = testStr;
    }

    @JmxExport(description = "test operation")
    public String doStuff(@JmxExport(value = "what", description = "some param") final String what,
            final String where) {
      return "Doing " + what + " " + where;
    }

  }

  @Test
  public void testRegistry()
          throws InterruptedException, IOException, InstanceNotFoundException, MBeanException,
          AttributeNotFoundException, ReflectionException, InvalidAttributeValueException {
    JmxTest testObj = new JmxTest();
    Properties props = new Properties();
    props.setProperty("propKey", "propvalue");
    Registry.export("caca", "maca", props);
    Registry.export("test", "Test", props, testObj);
    Registry.registerMBean("test2", "TestClassic", new org.spf4j.jmx.Test());

    Map<String, Object> map = new HashMap<>();
    map.put("isCrap", Boolean.TRUE);
    map.put("a.crap", Boolean.FALSE);
    map.put("isNonsense", "bla");
    map.put("", "bla");
    Registry.export("testMap", "map", map, testObj);

//        Thread.sleep(300000);
    Client.setAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
            "test", "Test", "booleanFlag", Boolean.TRUE);

    Client.setAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
            "test", "Test", "propKey", "caca");

    Object ret = Client.getAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
            "test", "Test", "booleanFlag");
    Assert.assertEquals(Boolean.TRUE, ret);

    String prop = (String) Client.getAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
            "test", "Test", "propKey");
    Assert.assertEquals("caca", prop);
    Assert.assertEquals("caca", props.get("propKey"));

    CompositeData cd = (CompositeData) Client.getAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
            "test", "Test", "columnDef");
    LOG.debug("CD={}", cd);
    Assert.assertEquals("bla", cd.get("name"));

    CompositeData cd2 = (CompositeData) Client.callOperation("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
            "test", "Test", "getColumnDef", "bubu");
    LOG.debug("CD2={}", cd2);
    Assert.assertEquals("bubu", cd2.get("name"));

    CompositeData cd3 = (CompositeData) Client.callOperation("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
            "test", "Test", "echo", cd2);
    LOG.debug("CD3={}", cd3);
    Assert.assertEquals("bubu", cd3.get("name"));

    Client.callOperation("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
            "test", "Test", "print", 3, Boolean.TRUE, "caca", cd2);

    Client.setAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
            "test", "Test", "stringVal", "bla bla");

    Object ret2 = Client.getAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
            "test", "Test", "stringVal");
    Assert.assertEquals("bla bla", ret2);

    try {
      Client.setAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
              "test", "Test", "doubleVal", 0.0);
      Assert.fail();
    } catch (InvalidAttributeValueException e) {
      Throwables.writeTo(e, System.err, Throwables.PackageDetail.SHORT);
    }
//        Thread.sleep(1000000000);

  }

  @Test
  @SuppressFBWarnings("UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS") // callable via JMX :-)
  public void testRegistry2() throws IOException, JMException {
    JmxTest testObj = new JmxTest();
    JmxTest2 testObj2 = new JmxTest2();
    Registry.unregister("test", "Test");
    Registry.export("test", "Test", testObj, testObj2);
    Registry.export("test", "TestStatic", JmxTest2.class);

    Client.setAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
            "test", "Test", "booleanFlag", Boolean.TRUE);

    Object ret = Client.getAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
            "test", "Test", "booleanFlag");
    Assert.assertEquals(Boolean.TRUE, ret);

    new DynamicMBeanBuilder().withJmxExportObject(new Object() {
      @JmxExport("customName")
      @SuppressFBWarnings("UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS")
      public int getMyValue() {
        return 13;
      }
    }).extend("test", "Test");

    Object retCustom = Client.getAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
            "test", "Test", "customName");

    Assert.assertEquals(Integer.valueOf(13), retCustom);

    Client.setAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
            "test", "Test", "stringVal", "bla bla");

    Object ret2 = Client.getAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
            "test", "Test", "stringVal");
    Assert.assertEquals("bla bla", ret2);

    try {
      Client.setAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
              "test", "Test", "doubleVal", 0.0);
      Assert.fail();
    } catch (InvalidAttributeValueException e) {
      Throwables.writeTo(e, System.err, Throwables.PackageDetail.NONE);
    }

    testObj2.setStringVal("cucu");
    Object ret3 = Client.getAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
            "test", "Test", "stringVal2");
    Assert.assertEquals("cucu", ret3);

    JmxTest2.setTestStr("bubu");
    Object ret4 = Client.getAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
            "test", "Test", "testStr");
    Assert.assertEquals("bubu", ret4);

    Object ret5 = Client.callOperation("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
            "test", "Test", "doStuff", "a", "b");
    Assert.assertEquals("Doing a b", ret5);

  }

  @Test
  public void testClassLocator() throws IOException, InstanceNotFoundException, MBeanException, ReflectionException {
    Registry.export(Jmx.class);
    CompositeData info = (CompositeData) Client.callOperation(
            "service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
            Jmx.class.getPackage().getName(),
            Jmx.class.getSimpleName(), "getPackageInfo", Registry.class.getName());
    LOG.debug("Returned {}", info);
    Assert.assertNotNull(info);
  }

}
