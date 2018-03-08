package org.spf4j.jdiff;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;

@SuppressFBWarnings("AI_ANNOTATION_ISSUES_NEEDS_NULLABLE")
public abstract class BaseJDiffMojo
        extends AbstractMojo {

  /**
   * The working directory for this plugin.
   */
  @Parameter(defaultValue = "${project.build.directory}/jdiff", readonly = true)
  private File workingDirectory;

  /**
   * The javadoc executable.
   */
  @Parameter(property = "javadocExecutable")
  private String javadocExecutable;

  /**
   * List of packages.
   */
  @Parameter(property = "includePackageNames")
  private ArrayList<String> includePackageNames = new ArrayList<>(2);

  @Component
  private ToolchainManager toolchainManager;

  /**
   * The current build mavenSession instance.
   */
  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  private MavenSession mavenSession;

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject mavenProject;

  @Parameter(defaultValue = "${mojoExecution}", required = true, readonly = true)
  private MojoExecution mojoExecution;

  /**
   * The entry point to Aether, i.e. the component doing all the work.
   *
   * @component
   */
  @Component
  private RepositorySystem repoSystem;

  final MavenSession getMavenSession() {
    return mavenSession;
  }

  @SuppressWarnings("unchecked")
  final Map<String, Artifact> getPluginArtifactMap() {
    return mojoExecution.getMojoDescriptor().getPluginDescriptor().getArtifactMap();
  }

  final PluginDescriptor getPluginDescriptor() {
    return mojoExecution.getMojoDescriptor().getPluginDescriptor();
  }

  /**
   * Get the path of the Javadoc tool executable depending the user entry or try to find it depending the OS or the
   * <code>java.home</code> system property or the <code>JAVA_HOME</code> environment variable.
   *
   * @return the path of the Javadoc tool
   * @throws IOException if not found
   */
  final String getJavadocExecutable()
          throws IOException {
    Toolchain tc = getToolchain();

    if (tc != null) {
      getLog().info("Toolchain in jdiff-maven-plugin: " + tc);
      if (javadocExecutable != null) {
        getLog().warn("Toolchains are ignored, 'javadocExecutable' parameter is set to " + javadocExecutable);
      } else {
        javadocExecutable = tc.findTool("javadoc");
      }
    }

    String javadocCommand = "javadoc" + (SystemUtils.IS_OS_WINDOWS ? ".exe" : "");

    File javadocExe;

    // ----------------------------------------------------------------------
    // The javadoc executable is defined by the user
    // ----------------------------------------------------------------------
    if (StringUtils.isNotEmpty(javadocExecutable)) {
      javadocExe = new File(javadocExecutable);

      if (javadocExe.isDirectory()) {
        javadocExe = new File(javadocExe, javadocCommand);
      }

      if (SystemUtils.IS_OS_WINDOWS && javadocExe.getName().indexOf('.') < 0) {
        javadocExe = new File(javadocExe.getPath() + ".exe");
      }

      if (!javadocExe.isFile()) {
        throw new IOException("The javadoc executable '" + javadocExe
                + "' doesn't exist or is not a file. Verify the <javadocExecutable/> parameter.");
      }

      return javadocExe.getAbsolutePath();
    }
    File javaHome = SystemUtils.getJavaHome();
    // ----------------------------------------------------------------------
    // Try to find javadocExe from System.getProperty( "java.home" )
    // By default, System.getProperty( "java.home" ) = JRE_HOME and JRE_HOME
    // should be in the JDK_HOME
    // ----------------------------------------------------------------------
    // For IBM's JDK 1.2
    if (SystemUtils.IS_OS_AIX) {
      javadocExe
              = new File(javaHome + File.separator + ".." + File.separator + "sh", javadocCommand);
    } else if (SystemUtils.IS_OS_MAC_OSX) {
      javadocExe = new File(javaHome + File.separator + "bin", javadocCommand);
    } else {
      javadocExe
              = new File(javaHome + File.separator + ".." + File.separator + "bin", javadocCommand);
    }

    // ----------------------------------------------------------------------
    // Try to find javadocExe from JAVA_HOME environment variable
    // ----------------------------------------------------------------------
    if (!javadocExe.exists() || !javadocExe.isFile()) {
      Properties env = CommandLineUtils.getSystemEnvVars();
      String javaHomeStr = env.getProperty("JAVA_HOME");
      if (StringUtils.isEmpty(javaHomeStr)) {
        throw new IOException("The environment variable JAVA_HOME is not correctly set: javahome='"
                + javaHomeStr + '\'');
      }
      File javaHomeFile = new File(javaHomeStr);
      if ((!javaHomeFile.exists()) || (!javaHomeFile.isDirectory())) {
        throw new IOException("The environment variable JAVA_HOME=" + javaHome
                + " doesn't exist or is not a valid directory.");
      }

      javadocExe = new File(javaHomeStr + File.separator + "bin", javadocCommand);
    }

    if (!javadocExe.exists() || !javadocExe.isFile()) {
      throw new IOException("The javadoc executable '" + javadocExe
              + "' doesn't exist or is not a file. Verify the JAVA_HOME environment variable.");
    }

    return javadocExe.getAbsolutePath();
  }

  private Toolchain getToolchain() {
    Toolchain tc = null;
    if (toolchainManager != null) {
      tc = toolchainManager.getToolchainFromBuildContext("jdk", mavenSession);
    }

    return tc;
  }

  final List<String> getCompileSourceRoots() {
    if ("pom".equalsIgnoreCase(mavenProject.getPackaging())) {
      return Collections.emptyList();
    } else {
      List<String> compileSourceRoots = mavenProject.getCompileSourceRoots();
      return compileSourceRoots == null ? Collections.EMPTY_LIST : compileSourceRoots;
    }
  }

  final MavenProject getMavenProject() {
    return mavenProject;
  }

  final File getWorkingDirectory() {
    return workingDirectory;
  }

  final MojoExecution getMojoExecution() {
    return mojoExecution;
  }

  final List<RemoteRepository> getProjectRepos() {
    return mavenProject.getRemoteProjectRepositories();
  }

  final RepositorySystem getRepoSystem() {
    return repoSystem;
  }

  final ArrayList<String> getIncludePackageNames() {
    return includePackageNames;
  }

  final ToolchainManager getToolchainManager() {
    return toolchainManager;
  }


  /**
   * Overwite this class for proper toString.
   * @return
   */
  @Override
  public String toString() {
    return "BaseJDiffMojo{" + "workingDirectory=" + workingDirectory + ", javadocExecutable="
            + javadocExecutable + ", includePackageNames=" + includePackageNames + '}';
  }

}
