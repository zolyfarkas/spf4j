
package org.spf4j.base;

import java.io.UnsupportedEncodingException;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

/**
 *
 * @author zoly
 */
@State(Scope.Benchmark)
@Fork(2)
@Threads(value = 4)
public class ReflectionsBenchmark {

    public static final java.lang.reflect.Method TEST_METHOD;

    static {
      try {
        TEST_METHOD = System.class.getMethod("arraycopy",
                Object.class, int.class, Object.class, int.class, int.class);
      } catch (NoSuchMethodException | SecurityException ex) {
        throw new ExceptionInInitializerError(ex);
      }
    }

    @Benchmark
    public final Class<?> [] optimizedGetTypes() throws UnsupportedEncodingException {
        return Reflections.getParameterTypes(TEST_METHOD);
    }

    @Benchmark
    public final Class<?> [] normalGetTypes() {
        return TEST_METHOD.getParameterTypes();
    }



}
