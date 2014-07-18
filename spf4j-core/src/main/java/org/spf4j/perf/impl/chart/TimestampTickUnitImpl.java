
package org.spf4j.perf.impl.chart;

import org.jfree.chart.axis.NumberTickUnit;
import org.joda.time.format.DateTimeFormatter;

/**
 *
 * @author zoly
 */
@edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "NFF_NON_FUNCTIONAL_FIELD")
class TimestampTickUnitImpl extends NumberTickUnit {
    private static final long serialVersionUID = 0L;
    private final long[] timestamps;
    private final long stepMillis;
    private final transient DateTimeFormatter formatter;

    public TimestampTickUnitImpl(final double size,
            final long[] timestamps, final long stepMillis, final DateTimeFormatter formatter) {
        super(size);
        this.timestamps = timestamps;
        this.formatter = formatter;
        this.stepMillis = stepMillis;
    }

    @Override
    public String valueToString(final double value) {
        int ival = (int) Math.round(value);
        long val;
        if (ival >= timestamps.length) {
            val = timestamps[timestamps.length - 1] + stepMillis * (ival - timestamps.length + 1);
        } else if (ival < 0) {
            val = timestamps[0] + ival * stepMillis;
        } else {
            val = timestamps[ival];
        }
        return formatter.print(val);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + java.util.Arrays.hashCode(this.timestamps);
        hash = 89 * hash + (int) (this.stepMillis ^ (this.stepMillis >>> 32));
        return 89 * hash + (this.formatter != null ? this.formatter.hashCode() : 0);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TimestampTickUnitImpl other = (TimestampTickUnitImpl) obj;
        if (!java.util.Arrays.equals(this.timestamps, other.timestamps)) {
            return false;
        }
        if (this.stepMillis != other.stepMillis) {
            return false;
        }
        return !(this.formatter != other.formatter
                && (this.formatter == null || !this.formatter.equals(other.formatter)));
    }
    
}
