
package org.spf4j.avro;

import org.spf4j.base.CharSequences;

/**
 * @author Zoltan Farkas
 */
public final class SchemaRef {

  private final String groupId;

  private final String artifactId;

  private final String version;

  private final String ref;

  public SchemaRef(final CharSequence sequence) {
    if (!CharSequences.isValidFileName(sequence)) {
      throw new IllegalArgumentException("Invalid charecters in " + sequence);
    }
    int l = sequence.length();
    int idx = -1;
    int idxP1 = idx + 1;
    idx = CharSequences.indexOf(sequence, idxP1, l, ':');
    if (idx < 0) {
      throw new IllegalArgumentException("Invalid schema id " + sequence);
    }
    groupId = sequence.subSequence(idxP1, idx).toString();
    idxP1 = idx + 1;
    idx = CharSequences.indexOf(sequence, idxP1, l, ':');
    if (idx < 0) {
      throw new IllegalArgumentException("Invalid schema id " + sequence);
    }
    artifactId = sequence.subSequence(idxP1, idx).toString();
    idxP1 = idx + 1;
    idx = CharSequences.indexOf(sequence, idxP1, l, ':');
    if (idx < 0) {
      throw new IllegalArgumentException("Invalid schema id " + sequence);
    }
    version = sequence.subSequence(idxP1, idx).toString();
    ref = sequence.subSequence(idx + 1, l).toString();
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return version;
  }

  public String getRef() {
    return ref;
  }

  @Override
  public String toString() {
    return groupId + ':' + artifactId + ':' + version + ':' + ref;
  }

}
