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
public class ScannerTest3 {

  public enum Something {
    BLA
  }

  public static final Object[] DEFAULT = new Object[]{
    System.getProperty("spf4j.jdbc.heartBeats.sql.tableName", "HEARTBEATS"),
    System.getProperty("spf4j.jdbc.heartBeats.sql.ownerColumn", "OWNER"),
    System.getProperty("spf4j.jdbc.heartBeats.sql.intervalMillisColumn", "INTERVAL_MILLIS"),
    System.getProperty("spf4j.jdbc.heartBeats.sql.lastHeartBeatMillisColumn", "LAST_HEARTBEAT_INSTANT_MILLIS"),
    System.getProperty("spf4j.jdbc.heartBeats.sql.currTsSqlFunction", getStuff(Something.BLA))};

  public static String getStuff(final Something bla) {
    return "caca" + bla;
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
    List<Invocation> findUsages2 = Scanner.findUsages(ScannerTest3.class, lookFor);
    System.out.println("Scan 1 = " + findUsages2);
    Assert.assertThat(findUsages2, CoreMatchers.hasItem(
        Matchers.allOf(
             Matchers.hasProperty("invokedMethod", Matchers.hasProperty("name", Matchers.equalTo("getProperty"))),
             Matchers.hasProperty("parameters", Matchers.hasItemInArray("spf4j.jdbc.heartBeats.sql.currTsSqlFunction")))));
  }

}
