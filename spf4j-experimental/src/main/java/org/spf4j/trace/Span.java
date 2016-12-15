package org.spf4j.trace;

import com.google.common.annotations.Beta;
import java.util.List;
import org.spf4j.stackmonitor.SampleNode;

/**
 * see http://research.google.com/pubs/pub36356.html for terminology details.
 * @author zoly
 */
@Beta
public interface Span {

  CharSequence getName();

  int getSpanId();

  int getParentId();

  long getStartTimeMillis();

  long getElapsedTimeMillis();

  SampleNode getSamples();

  List<TraceAnnotation> getAnnotations();


}
