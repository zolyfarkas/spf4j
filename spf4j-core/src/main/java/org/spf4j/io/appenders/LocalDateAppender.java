
package org.spf4j.io.appenders;

import java.io.IOException;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.spf4j.io.ObjectAppender;

/**
 *
 * @author zoly
 */
public final class LocalDateAppender implements ObjectAppender<LocalDate> {

    public static final DateTimeFormatter FMT = ISODateTimeFormat.date();
    
    @Override
    public void append(final LocalDate date, final Appendable appendTo) throws IOException {
        FMT.printTo(appendTo, date);
    }
    
}
