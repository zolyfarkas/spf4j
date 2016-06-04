
package org.spf4j.base.asm;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.List;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class ScannerTest2 {

    public static final String DEFAULT_SS_DUMP_FILE_NAME_PREFIX =
            System.getProperty("spf4j.perf.ms.defaultSsdumpFilePrefix", ManagementFactory.getRuntimeMXBean().getName());

  @Test
  public void testScan() throws NoSuchMethodException, IOException {

    final ImmutableSet<Method> lookFor = ImmutableSet.of(System.class.getDeclaredMethod("getProperty", String.class),
            System.class.getDeclaredMethod("getProperty", String.class, String.class),
            Integer.class.getDeclaredMethod("getInteger", String.class),
            Integer.class.getDeclaredMethod("getInteger", String.class, int.class),
            Integer.class.getDeclaredMethod("getInteger", String.class, Integer.class),
            Long.class.getDeclaredMethod("getLong", String.class),
            Long.class.getDeclaredMethod("getLong", String.class, Long.class),
            Long.class.getDeclaredMethod("getLong", String.class, long.class),
            Boolean.class.getDeclaredMethod("getBoolean", String.class));
    List<Invocation> findUsages2 = Scanner.findUsages(ScannerTest2.class, lookFor);
    System.out.println("Scan 1 = " + findUsages2);
    Assert.assertThat(findUsages2, CoreMatchers.hasItem(
            Matchers.allOf(
                    Matchers.hasProperty("invokedMethod", Matchers.hasProperty("name", Matchers.equalTo("getProperty"))),
                    Matchers.hasProperty("parameters", Matchers.hasItemInArray("spf4j.perf.ms.defaultSsdumpFilePrefix")))));
  }

}
