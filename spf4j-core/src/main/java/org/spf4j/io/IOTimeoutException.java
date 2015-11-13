package org.spf4j.io;

import java.io.IOException;
import org.joda.time.format.ISODateTimeFormat;

/**
 *
 * @author zoly
 */
public class IOTimeoutException extends IOException {


    private final long deadline;

    private final long millisAfterDeadline;

    public IOTimeoutException(final long deadline, final long millisAfterDeadline) {
        super("Timeout encountered, " + millisAfterDeadline + " ms after deadline: "
                + ISODateTimeFormat.dateTime().print(deadline));
        this.deadline = deadline;
        this.millisAfterDeadline = deadline;
    }

    public final long getDeadline() {
        return deadline;
    }

    public final long getMillisAfterDeadline() {
        return millisAfterDeadline;
    }



}
