
package org.spf4j.base.asm;

import java.lang.reflect.Method;
import org.objectweb.asm.Type;

/**
 *
 * @author zoly
 */
public final class TypeUtils {

    private TypeUtils() { }

    public static String toString(final Method m) {
        return Type.getInternalName(m.getDeclaringClass()) + '/' + m.getName() + Type.getMethodDescriptor(m);
    }


}
