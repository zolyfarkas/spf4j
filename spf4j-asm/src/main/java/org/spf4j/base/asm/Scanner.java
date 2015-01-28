
package org.spf4j.base.asm;

import com.google.common.base.Supplier;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.ClassReader;

/**
 *
 * @author zoly
 */
public final class Scanner {


    private Scanner() { }


    @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE") // Findbugs does not like try with resources.
    public static List<Invocation> findUsages(final Supplier<InputStream> classSupplier,
            final Set<Method> invokedMethods)
                        throws IOException {
        try (InputStream is = classSupplier.get()) {
            ClassReader reader = new ClassReader(is);
            List<Invocation> result = new ArrayList<>();
            reader.accept(new MethodInvocationClassVisitor(result, invokedMethods), 0);
            return result;
        }
    }





}
