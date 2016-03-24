
package org.spf4j.io.appenders;

import java.io.IOException;
import java.sql.Date;
import org.spf4j.io.ObjectAppender;

/**
 *
 * @author zoly
 */
public final class SqlDateAppender implements ObjectAppender<Date> {

    @Override
    public void append(final Date date, final Appendable appendTo) throws IOException {
        LocalDateAppender.FMT.printTo(appendTo, date.getTime());
    }
    
}
