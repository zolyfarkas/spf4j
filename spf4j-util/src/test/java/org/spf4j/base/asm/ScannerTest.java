
package org.spf4j.base.asm;

import org.spf4j.base.asm.Scanner;
import org.spf4j.base.asm.Invocation;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
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
        final Supplier<InputStream> claszSupplier = new Supplier<InputStream>() {
            
            @Override
            public InputStream get() {
                return new BufferedInputStream(ScannerTest.class.getClassLoader().getResourceAsStream(
                        ScannerTest.class.getName().replaceAll("\\.", "/") + ".class"));
            }
        };
       
        List<Invocation> findUsages = Scanner.findUsages(claszSupplier, ImmutableSet.of(A.class.getDeclaredMethod("getValue")));
        System.out.println(findUsages);
        List<Invocation> findUsages2 = Scanner.findUsages(claszSupplier,
                ImmutableSet.of(System.class.getDeclaredMethod("getProperty", String.class),
                        System.class.getDeclaredMethod("getProperty", String.class, String.class),
                        Integer.class.getDeclaredMethod("getInteger", String.class),
                        Integer.class.getDeclaredMethod("getInteger", String.class, int.class),
                        Integer.class.getDeclaredMethod("getInteger", String.class, Integer.class),
                        Long.class.getDeclaredMethod("getLong", String.class),
                        Long.class.getDeclaredMethod("getLong", String.class, Long.class),
                        Long.class.getDeclaredMethod("getLong", String.class, long.class)));
        System.out.println(findUsages2);

        
    }
    
}
