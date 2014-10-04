package org.spf4j.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;


/**
 *
 * @author zoly
 */
public final class BeanBuilder {
    private static final Class[] PROXY_PARAMS = {InvocationHandler.class};
    static <T> T createBean(final Class<T> type, final Map<Method, Object> objects) {
        Class<T> proxyClass = (Class<T>) Proxy.getProxyClass(type.getClassLoader(), type);
        Constructor<T> constructor;
        try {
            constructor = proxyClass.getConstructor(PROXY_PARAMS);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        }
        try {
            return constructor.newInstance(new DynaBeanImpl(objects));
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }
}
