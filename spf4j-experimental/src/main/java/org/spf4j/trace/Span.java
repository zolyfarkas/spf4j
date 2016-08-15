package org.spf4j.trace;

import com.google.common.annotations.Beta;
import javax.annotation.Nullable;

/**
 * see http://research.google.com/pubs/pub36356.html for terminology details.
 * @author zoly
 */
@Beta
public interface Span {

  CharSequence getName();

  long getStartTimeMillis();

  long getElapsedTimeMillis();
  
  default Span getParent() {
    return null;
  }

}
