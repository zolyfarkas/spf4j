package org.spf4j.spf4j.systemprops.maven.plugin;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;


@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresProject = true)
public class MyMojo
        extends AbstractMojo {

    /**
     * Location of the file.
     */
    @Parameter(defaultValue = "${project.build.directory}/systemProperties",
            property = "outputDir", required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${project.artifactId}.avdl",
            property = "outputFile", required = true)
    private String fileName;

    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String encoding;

    @Parameter
    private String namespace;

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
            w.write("}\n");
            w.write("}\n");
        } catch (IOException ex) {
           throw new MojoExecutionException("Cannot generate config description", ex);
        }
    }
}
