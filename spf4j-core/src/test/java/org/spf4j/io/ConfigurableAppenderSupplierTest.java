
package org.spf4j.io;

import com.google.common.net.HostAndPort;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class ConfigurableAppenderSupplierTest {


  @Test
  @SuppressWarnings("unchecked")
  public void testBehavior() {
    ConfigurableAppenderSupplier supplier = new ConfigurableAppenderSupplier();
    final ObjectAppender<Object> objectAppender = (Object object, Appendable appendTo) -> {
      appendTo.append("its an object!");
    };
    final ObjectAppender<ConfigurableAppenderSupplierTest> testObjAppender =
            (ConfigurableAppenderSupplierTest object, Appendable appendTo) -> {
              appendTo.append("its my unit test!");
    };
    final ObjectAppender<CharSequence> testObjAppender2 =
            (CharSequence object, Appendable appendTo) -> {
              appendTo.append(object);
    };

    supplier.register(Object.class, objectAppender);
    supplier.register(ConfigurableAppenderSupplierTest.class, testObjAppender);
    supplier.register(CharSequence.class, testObjAppender2);

    Assert.assertEquals(testObjAppender, supplier.get(ConfigurableAppenderSupplierTest.class));
    Assert.assertEquals(objectAppender, supplier.get(Object.class));
    Assert.assertEquals(testObjAppender2, supplier.get(String.class));
    Assert.assertEquals(objectAppender, supplier.get(HostAndPort.class));


  }

}
