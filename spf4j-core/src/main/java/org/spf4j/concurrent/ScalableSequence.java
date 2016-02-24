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
package org.spf4j.concurrent;

import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author zoly
 */
public final class ScalableSequence implements Sequence {

    private final AtomicLong counter;

    private final long bucketSize;

    private static final class Bucket {

        private long current;
        private long max;

        Bucket(final long start, final long end) {
            this.current = start;
            this.max = end;
        }

        public void reset(final long start, final long end) {
            this.current = start;
            this.max = end;
        }

        public boolean hasValue() {
            return current < max;
        }

        public long getValue() {
            return current++;
        }

    }

    private final ThreadLocal<Bucket> buckets;

    public ScalableSequence(final long start, final int bucketSize) {
        if (bucketSize < 10) {
            throw new IllegalArgumentException("Bucket size must be greater than 10 and not " + bucketSize);
        }
        this.counter = new AtomicLong(start);
        this.bucketSize = bucketSize;
        this.buckets = new ThreadLocal() {
            @Override
            protected Bucket initialValue() {
                long start = counter.getAndAdd(bucketSize);
                return new Bucket(start, start + bucketSize);
            }
        };
    }

    @Override
    public long next() {
        Bucket bucket = buckets.get();
        if (bucket.hasValue()) {
            return bucket.getValue();
        } else {
            long start = counter.getAndAdd(bucketSize);
            bucket.reset(start + 1, start + bucketSize);
            return start;
        }
    }

    @Override
    public String toString() {
        return "ScalableSequence{" + "counter=" + counter + ", bucketSize=" + bucketSize + '}';
    }

}
