package org.spf4j.perf.impl.chart;

import java.util.List;
import org.jfree.chart.axis.NumberTickUnit;
import org.spf4j.base.ComparablePair;
import org.spf4j.perf.impl.Quanta;

/**
 *
 * @author zoly
 */
public final class QuantizedNumberTickUnit extends NumberTickUnit {

    private final List<ComparablePair<Quanta, Integer>> quantas;

    public QuantizedNumberTickUnit(final double size, final List<ComparablePair<Quanta, Integer>> quantas) {
        super(size);
        this.quantas = quantas;
    }

    @Override
    public String valueToString(final double value) {
        int idx = (int) Math.round(value);
        if (idx < 0) {
            return "NI";
        } else if (idx >= quantas.size()) {
            return "PI";
        }
        long val = quantas.get(idx).getFirst().getIntervalStart();
        if (val == Long.MIN_VALUE) {
            return "NI";
        } else {
            return Long.toString(val);
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        return 37 * hash + (this.quantas != null ? this.quantas.hashCode() : 0);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final QuantizedNumberTickUnit other = (QuantizedNumberTickUnit) obj;
        return (!(this.quantas != other.quantas && (this.quantas == null || !this.quantas.equals(other.quantas))));
    }

    private static final long serialVersionUID = 1L;
    
}
