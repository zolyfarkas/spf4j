package org.spf4j.beans;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

public final class DynaBeanImpl implements InvocationHandler {

    private final Map<Method, Object> values;

    DynaBeanImpl(final Map<Method, Object> values) {
        this.values = values;
    }
    
    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final String methodName = method.getName();
        if (methodName.startsWith("get") || methodName.startsWith("is")) {
            return values.get(method);
        } else if ("hashCode".equals(methodName)) {
            return proxy.hashCode(); // TODO: needs to be implemented.
        } else if ("equals".equals(methodName)) {
           return proxy.equals(args[0]); // TODO: needs to be implemented.
        } else {
            return method.invoke(proxy, args);
        }
    }
    
}
