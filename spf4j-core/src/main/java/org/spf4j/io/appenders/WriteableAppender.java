
package org.spf4j.io.appenders;

import java.io.IOException;
import org.spf4j.base.Writeable;
import org.spf4j.io.ObjectAppender;

/**
 *
 * @author zoly
 */
public final class WriteableAppender implements ObjectAppender<Writeable> {

  @Override
  public void append(final Writeable object, final Appendable appendTo) throws IOException {
    object.writeTo(appendTo);
  }

}
