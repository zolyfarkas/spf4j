

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
        
        
    }
    
    @Test
    public void testRegistry()
            throws InterruptedException, IOException, InstanceNotFoundException, MBeanException,
            AttributeNotFoundException, ReflectionException, InvalidAttributeValueException {
        JmxTest testObj = new JmxTest();
        Registry.export("test", "Test", testObj);
        
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
        
    }

    
    
}
