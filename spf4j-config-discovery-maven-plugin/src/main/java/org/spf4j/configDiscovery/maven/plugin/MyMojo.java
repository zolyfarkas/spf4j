package org.spf4j.configDiscovery.maven.plugin;

import com.google.common.collect.ImmutableSet;
import java.io.BufferedInputStream;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Set;
import com.google.common.base.Supplier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.spf4j.base.asm.Invocation;
import org.spf4j.base.asm.Scanner;


@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresProject = true)
public class MyMojo
        extends AbstractMojo {

    /**
     * Location of the file.
     */
    @Parameter(defaultValue = "${project.build.directory}/classes/META-INF",
            property = "outputDir", required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${project.artifactId}.avdl",
            property = "outputFile", required = true)
    private String fileName;

    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String encoding;

    @Parameter(defaultValue = "${project.build.directory}/classes")
    private File classes;



    /**
     * target namespace of the configurations.
     */
    @Parameter
    private String namespace;


    private final  Set<Method> methods = getSystemPropertyMethods();

    private static Set<Method> getSystemPropertyMethods() {
        try {
            return ImmutableSet.of(System.class.getDeclaredMethod("getProperty", String.class),
                    System.class.getDeclaredMethod("getProperty", String.class, String.class),
                    Integer.class.getDeclaredMethod("getInteger", String.class),
                    Integer.class.getDeclaredMethod("getInteger", String.class, int.class),
                    Integer.class.getDeclaredMethod("getInteger", String.class, Integer.class),
                    Long.class.getDeclaredMethod("getLong", String.class),
                    Long.class.getDeclaredMethod("getLong", String.class, Long.class),
                    Long.class.getDeclaredMethod("getLong", String.class, long.class),
                    Boolean.class.getDeclaredMethod("getBoolean", String.class));
        } catch (NoSuchMethodException | SecurityException ex) {
            throw new RuntimeException(ex);
        }
    }


    public void processClasses(final File location, final Map<String, Object> avdlWriter) throws IOException {
        if (!location.exists()) {
            return;
        }
        if (location.isDirectory()) {
            File[] listFiles = location.listFiles();
            for (File file : listFiles) {
                processClasses(file, avdlWriter);
            }
        } else if (location.getName().endsWith(".class")) {
            List<Invocation> invocations = Scanner.findUsages(new Supplier<InputStream> () {

                @Override
                public InputStream get() {
                    try {
                        return new BufferedInputStream(new FileInputStream(location));
                    } catch (FileNotFoundException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }, methods);
            for (Invocation invocation: invocations) {
                Class<?> returnType = invocation.getInvokedMethod().getReturnType();
                Object[] parameters = invocation.getParameters();
                String [] attrPath = ((String) parameters[0]).split("\\.");
                Map<String, Object> objs = avdlWriter;
                for (int i = 0; i < attrPath.length - 1; i++) {
                    final String pv = attrPath[i];
                    Map<String, Object> get = (Map<String, Object>) objs.get(pv);
                    if (get == null) {
                        get = new HashMap<>();
                        objs.put(pv, get);
                    }
                    objs = get;
                }
                String fname = attrPath[attrPath.length - 1];
                FieldInfo fi = (FieldInfo) objs.get(fname);
                if (fi == null) {
                    if (parameters.length > 1) {
                        fi = new FieldInfo(returnType, parameters[1]);
                    } else {
                        fi = new FieldInfo(returnType);
                    }
                    objs.put(fname, fi);
                }
            }
        }
    }


    @Override
    public void execute() throws MojoExecutionException {
        File f = outputDirectory;

        if (!f.exists()) {
            if (!f.mkdirs()) {
                throw new MojoExecutionException("Unable to create directory " + outputDirectory);
            }
        }

        File outFile = new File(f, fileName);

        try (Writer w = new OutputStreamWriter(new FileOutputStream(outFile), encoding)) {
            if (namespace != null) {
                w.write("@namespace(\"");
                w.write(namespace);
                w.write("\")\n");
            }
            w.write("protocol SystemProperties {\n");
            w.write("record SystemProperties {\n");

            Map<String, Object> record = new HashMap<>();
            processClasses(classes, record);

            w.write("}\n");
            w.write("}\n");
        } catch (IOException ex) {
           throw new MojoExecutionException("Cannot generate config description", ex);
        }
    }
}
