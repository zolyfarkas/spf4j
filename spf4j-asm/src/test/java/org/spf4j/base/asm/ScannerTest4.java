package org.spf4j.base.asm;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
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
public class ScannerTest4 {

  public static final class Something {
    public static final int NRPROCS;
    static {
        NRPROCS = java.lang.Runtime.getRuntime().availableProcessors();
    }
  }

 private static final int SPIN_LIMITER = Integer.getInteger("spf4j.lifoTp.maxSpinning",
            Something.NRPROCS / 2);

  public static String getStuff(final Something bla) {
    return "caca" + SPIN_LIMITER;
  }

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
    List<Invocation> findUsages2 = Scanner.findUsages(ScannerTest4.class, lookFor);
    System.out.println("Scan 1 = " + findUsages2);
    Assert.assertThat(findUsages2, CoreMatchers.hasItem(
        Matchers.allOf(
             Matchers.hasProperty("invokedMethod", Matchers.hasProperty("name", Matchers.equalTo("getInteger"))),
             Matchers.hasProperty("parameters", Matchers.hasItemInArray("spf4j.lifoTp.maxSpinning")))));
  }

}
