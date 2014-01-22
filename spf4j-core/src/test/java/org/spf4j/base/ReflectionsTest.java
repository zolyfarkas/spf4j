/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.base;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.lang.reflect.Method;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import static org.spf4j.base.Reflections.getCompatibleMethod;

/**
 *
 * @author zoly
 */
public final class ReflectionsTest {

    private static final LoadingCache<Reflections.MethodDesc, Method> CACHE = CacheBuilder.newBuilder()
            .build(new CacheLoader<Reflections.MethodDesc, Method>() {
                @Override
                public Method load(final Reflections.MethodDesc k) throws Exception {
                    return getCompatibleMethod(k.getClasz(), k.getName(), k.getParamTypes());
                }
            });

    public static Method getCompatibleMethodCachedGuava(final Class<?> c,
            final String methodName,
            final Class<?>... paramTypes) {
        return CACHE.getUnchecked(new Reflections.MethodDesc(c, methodName, paramTypes));
    }

    @Test
    public void testPerformance() {
        Class<?>[] params = new Class<?>[]{String.class, Integer.class};
        long startTimeS = System.currentTimeMillis();
        Method reflect = null;
        for (int i = 0; i < 1000000; i++) {
            reflect = Reflections.getCompatibleMethod(String.class, "indexOf", params);
        }
        long startTime = System.currentTimeMillis();
        Method fastM = null;
        for (int i = 0; i < 10000000; i++) {
            fastM = Reflections.getCompatibleMethodCached(String.class, "indexOf", params);
        }
        long startTimeG = System.currentTimeMillis();
        Method guavaM = null;
        for (int i = 0; i < 10000000; i++) {
            guavaM = getCompatibleMethodCachedGuava(String.class, "indexOf", params);
        }
        long endTime = System.currentTimeMillis();
        assertEquals(fastM, guavaM);
        System.out.println("Guava Exec Time " + (endTime - startTimeG));
        System.out.println("Fast Exec Time " + (startTimeG - startTime));
        System.out.println("Uncached Reflection Exec Time " + (startTime- startTimeS)*10);

        assertTrue("guava caching should be slower", (endTime - startTimeG) > (startTimeG - startTime));
        assertTrue("uncached reflection should be slowest", (startTime- startTimeS)*10 > (endTime - startTimeG));
    }

}
