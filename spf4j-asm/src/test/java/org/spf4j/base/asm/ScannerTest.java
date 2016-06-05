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
public final class ScannerTest {

  public static class A {

    Object getValue() {
      return new Object();
    }
  }

  public static class B extends A {

    @Override
    String getValue() {
      return "B";
    }
  }

  public static final String DEFAULT_SS_DUMP_FILE_NAME_PREFIX =
            System.getProperty("spf4j.perf.ms.defaultSsdumpFilePrefix", ManagementFactory.getRuntimeMXBean().getName());


  public void testSomeMethod() {
    // read a System property
    System.getProperty("some.property", "default value");
    Integer.getInteger("someInt.value", 3);
    Long.getLong("someLong.value", 5);
  }

  public int method() {
    /**
     * another
     */
    return Integer.getInteger("spf4j.custom.prop2", 1);
  }

  @Test
  public void testScan() throws NoSuchMethodException, IOException {

    List<Invocation> findUsages = Scanner.findUsages(ScannerTest.class,
            ImmutableSet.of(A.class.getDeclaredMethod("getValue")));
    System.out.println(findUsages);
    final ImmutableSet<Method> lookFor = ImmutableSet.of(System.class.getDeclaredMethod("getProperty", String.class),
            System.class.getDeclaredMethod("getProperty", String.class, String.class),
            Integer.class.getDeclaredMethod("getInteger", String.class),
            Integer.class.getDeclaredMethod("getInteger", String.class, int.class),
            Integer.class.getDeclaredMethod("getInteger", String.class, Integer.class),
            Long.class.getDeclaredMethod("getLong", String.class),
            Long.class.getDeclaredMethod("getLong", String.class, Long.class),
            Long.class.getDeclaredMethod("getLong", String.class, long.class),
            Boolean.class.getDeclaredMethod("getBoolean", String.class));
    List<Invocation> findUsages2 = Scanner.findUsages(ScannerTest.class, lookFor);
    System.out.println("Scan 1 = " + findUsages2);
    List<Invocation> findUsages3 = Scanner.findUsages(ScannerTest.class.getPackage().getName(), lookFor);
    System.out.println("Scan 2 = " + findUsages3);
    Assert.assertThat(findUsages2, CoreMatchers.hasItem(
            Matchers.allOf(
                    Matchers.hasProperty("caleeMethodName",
                            Matchers.equalTo("method")),
                    Matchers.hasProperty("invokedMethod", Matchers.hasProperty("name", Matchers.equalTo("getInteger"))),
                    Matchers.hasProperty("parameters", Matchers.arrayContaining("spf4j.custom.prop2", 1)))));

    Assert.assertThat(findUsages2, CoreMatchers.hasItem(
            Matchers.allOf(
                    Matchers.hasProperty("caleeMethodName",
                            Matchers.equalTo("testSomeMethod")),
                    Matchers.hasProperty("invokedMethod", Matchers.hasProperty("name", Matchers.equalTo("getProperty"))),
                    Matchers.hasProperty("parameters", Matchers.arrayContaining("some.property", "default value")))));

     Assert.assertThat(findUsages2, CoreMatchers.hasItem(
            Matchers.allOf(
                    Matchers.hasProperty("caleeMethodName",
                            Matchers.equalTo("testSomeMethod")),
                    Matchers.hasProperty("invokedMethod", Matchers.hasProperty("name", Matchers.equalTo("getLong"))),
                    Matchers.hasProperty("parameters", Matchers.arrayContaining("someLong.value", 5L)))));


  }

}
