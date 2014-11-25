/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.spf4j.net;

public final class Timing {
    
    // system time computed from NTP server response
    private final long ntpTime;

    // value of System.nanotime corresponding to mNtpTime in nanoseconds
    private final long ntpTimeReference;

    // round trip time in milliseconds
    private final long roundTripTime;

    public Timing(final long ntpTime, final long ntpTimeReference, final long roundTripTime) {
        this.ntpTime = ntpTime;
        this.ntpTimeReference = ntpTimeReference;
        this.roundTripTime = roundTripTime;
    }

    public long getNtpTime() {
        return ntpTime;
    }

    public long getNtpTimeReference() {
        return ntpTimeReference;
    }

    public long getRoundTripTime() {
        return roundTripTime;
    }
    
    public long getTime() {
        return ntpTime + (System.nanoTime() / 1000000L - ntpTimeReference);
    }

    @Override
    public String toString() {
        return "Timing{" + "ntpTime=" + ntpTime + ", ntpTimeReference="
                + ntpTimeReference + ", roundTripTime=" + roundTripTime + '}';
    }

}
