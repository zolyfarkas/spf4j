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
package org.spf4j.recyclable.impl;

import org.spf4j.base.AbstractRunnable;
import org.spf4j.recyclable.ObjectCreationException;
import org.spf4j.recyclable.ObjectDisposeException;
import org.spf4j.recyclable.RecyclingSupplier;
import org.spf4j.recyclable.Scanable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.spf4j.base.Handler;

/**
 *
 * @author zoly
 */
public final class RecyclingSupplierBuilder<T> {

    private final int maxSize;
    private final RecyclingSupplier.Factory<T> factory;
    private long timeoutMillis;
    private boolean fair;
    private ScheduledExecutorService maintenanceExecutor;
    private long maintenanceIntervalMillis;
    private Handler<T, ? extends Exception> borrowHook;
    private Handler<T, ? extends Exception> returnHook;
    private int initialSize;
    private boolean collectBorrowed;

    public RecyclingSupplierBuilder(final int maxSize, final RecyclingSupplier.Factory<T> factory) {
        this.fair = true;
        this.timeoutMillis = 60000;
        this.maxSize = maxSize;
        this.factory = factory;
        this.initialSize = 0;
    }

    public RecyclingSupplierBuilder<T> unfair() {
        this.fair = false;
        return this;
    }

    public RecyclingSupplierBuilder<T> withInitialSize(final int pinitialSize) {
        this.initialSize = pinitialSize;
        return this;
    }
    
    public RecyclingSupplierBuilder<T> withOperationTimeout(final long ptimeoutMillis) {
        this.timeoutMillis = ptimeoutMillis;
        return this;
    }

    public RecyclingSupplierBuilder<T> withMaintenance(final ScheduledExecutorService pexec,
            final long pmaintenanceIntervalMillis, final boolean pcollectBorrowed) {
        this.maintenanceExecutor = pexec;
        this.maintenanceIntervalMillis = pmaintenanceIntervalMillis;
        this.collectBorrowed = pcollectBorrowed;
        return this;
    }

    public RecyclingSupplierBuilder<T> withGetHook(final Handler<T, ? extends Exception> phook) {
        this.borrowHook = phook;
        return this;
    }

    public RecyclingSupplierBuilder<T> withRecycleHook(final Handler<T, ? extends Exception> phook) {
        this.returnHook = phook;
        return this;
    }

    public RecyclingSupplier<T> build() throws ObjectCreationException {
        final ScalableObjectPool<T> underlyingPool =
                new ScalableObjectPool<T>(initialSize, maxSize, factory, timeoutMillis, fair);
        final RecyclingSupplier<T> pool;
        if (borrowHook != null || returnHook != null) {
            pool = new ObjectPoolWrapper<T>(underlyingPool, borrowHook, returnHook);
        } else {
            pool = underlyingPool;
        }
        if (maintenanceExecutor != null) {
            maintenanceExecutor.scheduleWithFixedDelay(new AbstractRunnableImpl<T>(underlyingPool, collectBorrowed),
                    maintenanceIntervalMillis, maintenanceIntervalMillis, TimeUnit.MILLISECONDS);
        }
        return pool;
    }

    public static final class AbstractRunnableImpl<T> extends AbstractRunnable {

        private final ScalableObjectPool<T> underlyingPool;
        private final boolean collectBorrowed;

        public AbstractRunnableImpl(final ScalableObjectPool<T> underlyingPool, final boolean collectBorrowed) {
            super(true);
            this.underlyingPool = underlyingPool;
            this.collectBorrowed = collectBorrowed;
        }

        @Override
        public void doRun() throws Exception {
                if (collectBorrowed) {
                    underlyingPool.requestReturnFromBorrowersIfNotInUse();
                }
                underlyingPool.scan(new Scanable.ScanHandler<ObjectHolder<T>>() {
                    @Override
                    public boolean handle(final ObjectHolder<T> object) throws ObjectDisposeException {
                        object.validateObjectIfNotBorrowed();
                        return true;
                    }
                });
           
        }
    }
}
