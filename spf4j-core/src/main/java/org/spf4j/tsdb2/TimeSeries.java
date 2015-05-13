
package org.spf4j.tsdb2;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings({ "EI_EXPOSE_REP", "EI_EXPOSE_REP2" })
public final class TimeSeries {

    private final long [] timeStamps;

    private final long[][] values;

    public TimeSeries(final long[] timeStamps, final  long[][] values) {
        this.timeStamps = timeStamps;
        this.values = values;
    }

    public long[] getTimeStamps() {
        return timeStamps;
    }

    public long[][] getValues() {
        return values;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("TimeSeries(\n");
        for (int i = 0; i < timeStamps.length; i++) {
            result.append(timeStamps[i]).append(':').append(Arrays.toString(values[i])).append('\n');
        }
        result.append(')');
        return result.toString();
    }



}
