package org.spf4j.zel.vm;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.spf4j.base.Reflections;

/**
 *
 * @author zoly
 */
public final class JavaMethodCall implements Method {

    private final String name;
    private final Class<?> objectClass;
    private final Object object;

    public JavaMethodCall(final Object object, final String methodName) {
        this.name = methodName;
        this.objectClass = object.getClass();
        this.object = object;
    }
    
    public JavaMethodCall(final Class<?> objectClass, final String methodName) {
        this.name = methodName;
        this.objectClass = objectClass;
        this.object = null;
    }
    
    @Override
    public Object invokeInverseParamOrder(final List<Object> parameters)
            throws IllegalAccessException, InvocationTargetException {
        int np = parameters.size();
        Class<?>[] classes = new Class<?>[np];
        Object[] params = new Object[np];
        int i = np - 1;
        for (Object obj : parameters) {
            Class<?> clasz = obj.getClass();
            classes[i] = clasz;
            params[i] = obj;
            i--;
        }
        return Reflections.getCompatibleMethodCached(objectClass, name, classes)
                .invoke(object, params);
    }

}
