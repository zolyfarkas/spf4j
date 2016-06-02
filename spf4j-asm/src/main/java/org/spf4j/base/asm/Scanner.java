
package org.spf4j.base.asm;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedInputStream;
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

    public static List<Invocation> findUsages(final Class<?> clasz, final Set<Method> invokedMethods) {
        return findUsages(clasz.getClassLoader(), clasz.getName().replaceAll("\\.", "/") + ".class", invokedMethods);
    }

    public static List<Invocation> findUsages(final ClassLoader cl,
            final String claszResourcePath, final Set<Method> invokedMethods) {
        Supplier<InputStream> supplier = new Supplier<InputStream>() {

            @Override
            public InputStream get() {
                return new BufferedInputStream(cl.getResourceAsStream(claszResourcePath));
            }
        };
        try {
            return findUsages(supplier, invokedMethods);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }



    public static List<Invocation> findUsages(final String packageName, final Set<Method> invokedMethods)
            throws IOException {
        final ClassLoader cl = ClassLoader.getSystemClassLoader();
        ClassPath cp = ClassPath.from(cl);
        ImmutableSet<ClassPath.ClassInfo> classes = cp.getAllClasses();
        List<Invocation> result = new ArrayList();
        for (ClassPath.ClassInfo info : classes) {
            if (info.getName().startsWith(packageName)) {
                result.addAll(findUsages(cl, info.getResourceName(), invokedMethods));
            }
        }
        return result;
    }



}
