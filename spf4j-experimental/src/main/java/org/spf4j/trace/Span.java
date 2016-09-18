package org.spf4j.trace;

import com.google.common.annotations.Beta;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * see http://research.google.com/pubs/pub36356.html for terminology details.
 * @author zoly
 */
@Beta
public interface Span {

  CharSequence getName();

  int getSpanId();

  long getStartTimeMillis();

  long getElapsedTimeMillis();

  @Nullable
  default Span getParent() {
    return null;
  }

  @Nonnull
  default List<Span> getChildren() {
    return Collections.EMPTY_LIST;
  }

}
