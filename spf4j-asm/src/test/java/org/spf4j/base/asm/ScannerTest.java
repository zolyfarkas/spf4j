
package org.spf4j.base.asm;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.Test;
import org.objectweb.asm.Type;

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

    @Test
    public void testSomeMethod() throws NoSuchMethodException {
       String desc  = Type.getMethodDescriptor(B.class.getDeclaredMethod("getValue"));
       System.out.println(desc + new A().getValue());
       String desc2  = Type.getMethodDescriptor(A.class.getDeclaredMethod("getValue"));
       System.out.println(desc2 + ((A) new B()).getValue());
       // read a System property
       System.getProperty("some.property", "default value");
       Integer.getInteger("someInt.value", 3);
       Long.getLong("someLong.value", 5);
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

    }

}
