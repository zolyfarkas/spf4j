
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

package org.spf4j.jmx;

import java.io.IOException;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public final class RegistryTest {

    public static final class JmxTest {
        
        private volatile String stringVal;
        
        private volatile double doubleVal;
        
        private volatile boolean booleanFlag;
        
        private final String [][] matrix = {{"a", "b"}, {"c", "d"}};
        
        private volatile TestEnum enumVal = TestEnum.VAL2;
        
        private final TestBean bean = new TestBean(3, "bla");
        
        @JmxExport
        public String [][] getMatrix() {
            return matrix;
        }
        
        private final String [] array = {"a", "b"};
        
        @JmxExport
        public String [] getArray() {
            return array;
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
        
        
        
    }
    
    public static final class JmxTest2 {
            
        private volatile String stringVal;

        @JmxExport
        public String getStringVal() {
            return stringVal;
        }

        @JmxExport
        public void setStringVal(final String stringVal) {
            this.stringVal = stringVal;
        }
        
        private static volatile String testStr;

        @JmxExport
        public static String getTestStr() {
            return testStr;
        }

        public static void setTestStr(final String testStr) {
            JmxTest2.testStr = testStr;
        }
        
        @JmxExport(description = "test operation")
        public String doStuff(@JmxExport(name = "what", description = "some param") final String what,
                final String where) {
            return "Doing " + what + " " + where;
        }
        
        
    }
    
    @Test
    public void testRegistry()
            throws InterruptedException, IOException, InstanceNotFoundException, MBeanException,
            AttributeNotFoundException, ReflectionException, InvalidAttributeValueException {
        JmxTest testObj = new JmxTest();
        Registry.export("test", "Test", testObj);
        Registry.register("test2", "TestClassic", new org.spf4j.jmx.Test());
        
//        Thread.sleep(300000);
        
        Client.setAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
                "test", "Test", "booleanFlag", true);
       
        Object ret = Client.getAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
                "test", "Test", "booleanFlag");
        Assert.assertEquals(Boolean.TRUE, ret);
        
        Client.setAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
                "test", "Test", "stringVal", "bla bla");
       
        Object ret2 = Client.getAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
                "test", "Test", "stringVal");
        Assert.assertEquals("bla bla", ret2);
        
        try {
            Client.setAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
                "test", "Test", "doubleVal", 0.0);
        } catch (InvalidAttributeValueException e) {
            e.printStackTrace();
        }
        
    }

    
    @Test
    public void testRegistry2()
            throws InterruptedException, IOException, InstanceNotFoundException, MBeanException,
            AttributeNotFoundException, ReflectionException, InvalidAttributeValueException {
        JmxTest testObj = new JmxTest();
        JmxTest2 testObj2 = new JmxTest2();
        Registry.export("test", "Test", testObj, testObj2);
        Registry.export("test", "TestStatic", JmxTest2.class);
   
        Client.setAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
                "test", "Test", "JmxTest.booleanFlag", true);
       
        Object ret = Client.getAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
                "test", "Test", "JmxTest.booleanFlag");
        Assert.assertEquals(Boolean.TRUE, ret);
        
        Client.setAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
                "test", "Test", "JmxTest.stringVal", "bla bla");
       
        Object ret2 = Client.getAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
                "test", "Test", "JmxTest.stringVal");
        Assert.assertEquals("bla bla", ret2);
        
        try {
            Client.setAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
                "test", "Test", "JmxTest.doubleVal", 0.0);
        } catch (InvalidAttributeValueException e) {
            e.printStackTrace();
        }
        
        testObj2.setStringVal("cucu");
        Object ret3 = Client.getAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
                "test", "Test", "JmxTest2.stringVal");
        Assert.assertEquals("cucu", ret3);
        
        JmxTest2.setTestStr("bubu");
        Object ret4 = Client.getAttribute("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
                "test", "Test", "JmxTest2.testStr");
        Assert.assertEquals("bubu", ret4);
        
        Object ret5 = Client.callOperation("service:jmx:rmi:///jndi/rmi://:9999/jmxrmi",
                "test", "Test", "JmxTest2.doStuff", "a", "b");
        Assert.assertEquals("Doing a b", ret5);
        
    }

    
    
}
