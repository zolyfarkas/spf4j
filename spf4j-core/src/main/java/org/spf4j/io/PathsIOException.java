
package org.spf4j.io;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

/**
 *
 * @author zoly
 */
public final class PathsIOException extends IOException {

  private static final long serialVersionUID = 1L;

  private final List<URI> paths;

  public PathsIOException(final Path path, final IOException ex) {
    this.paths = new ArrayList<>();
    paths.add(path.toUri());
    this.addSuppressed(ex);
  }

  public void add(final Path path, final IOException ex) {
    paths.add(path.toUri());
    this.addSuppressed(ex);
  }

  @Nonnull
  public List<URI> getPaths() {
    return paths;
  }

  @Override
  public String toString() {
    return "PathsIOException{" + "paths=" + paths + '}';
  }

}
