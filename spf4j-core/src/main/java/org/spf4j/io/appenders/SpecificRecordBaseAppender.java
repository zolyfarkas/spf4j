package org.spf4j.io.appenders;

import java.io.IOException;
import org.apache.avro.specific.SpecificRecordBase;
import org.spf4j.io.ObjectAppender;

/**
 *
 * @author zoly
 */
public final class SpecificRecordBaseAppender implements ObjectAppender<SpecificRecordBase> {
  
  private static final SpecificRecordAppender SA = new SpecificRecordAppender();

  @Override
  public void append(final SpecificRecordBase object, final Appendable appendTo) throws IOException {
    SA.append(object, appendTo);
  }
  
}
