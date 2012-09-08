/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.perf.impl;

/**
 * this class ordering is based on start Interval ordering
 */
@edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "EQ_COMPARETO_USE_OBJECT_EQUALS")
public class Quanta implements Comparable<Quanta> {
    private final long intervalStart;
    private final long intervalEnd;

    public Quanta(long intervalStart, long intervalEnd) {
        this.intervalStart = intervalStart;
        this.intervalEnd = intervalEnd;
    }

    public Quanta(String stringVariant) {
        int undLocation = stringVariant.indexOf('_');
        if (undLocation < 0) {
            throw new IllegalArgumentException("Invalid Quanta DataSource " + stringVariant);
        }
        String startStr = stringVariant.substring(1, undLocation);
        String endStr = stringVariant.substring(undLocation + 1);
        if (startStr.equals("NI")) {
            this.intervalStart = Long.MIN_VALUE;
        } else {
            this.intervalStart = Long.valueOf(startStr);
        }
        if (endStr.equals("PI")) {
            this.intervalEnd = Long.MAX_VALUE;
        } else {
            this.intervalEnd = Long.valueOf(endStr);
        }
    }

    public long getIntervalEnd() {
        return intervalEnd;
    }

    public long getIntervalStart() {
        return intervalStart;
    }

    public long getClosestToZero() {
        return (intervalStart < 0) ? intervalEnd : intervalStart;
    }

    @Override
    public String toString() {
        return "Q" + ((intervalStart == Long.MIN_VALUE) ? "NI" : intervalStart) + "_" + ((intervalEnd == Long.MAX_VALUE) ? "PI" : intervalEnd);
    }

    public int compareTo(Quanta o) {
        if (this.intervalStart < o.intervalStart) {
            return -1;
        } else if (this.intervalStart > o.intervalStart) {
            return 1;
        } else {
            return 0;
        }
    }
    
}
