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
package org.spf4j.base;

import com.google.common.base.Throwables;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author zoly
 */
public abstract class AbstractRunnable implements Runnable {

    private final boolean lenient;

    private final String threadName;

    /**
     * If lenient is true, it means that nobody is waiting for this runnable's result(finish)
     * so To not loose the exception, the runnable will LOG it as an error and not rethrow it
     * @param lenient
     */
    public AbstractRunnable(final boolean lenient, @Nullable final String threadName) {
        this.lenient = lenient;
        this.threadName = threadName;
    }

    /**
     * If lenient is true, it means that nobody is waiting for this runnable's result(finish)
     * so To not loose the exception, the runnable will LOG it as an error, and not retrow it
     * @param lenient
     */
    public AbstractRunnable(final boolean lenient) {
       this(lenient, null);
    }

    public AbstractRunnable() {
        this(false, null);
    }

    public AbstractRunnable(final String threadName) {
        this(false, null);
    }

    public static final int ERROR_EXIT_CODE = 666;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRunnable.class);

    @Override
    public final void run() {
        Thread thread = null;
        String origName = null;
        if (threadName != null) {
             thread = Thread.currentThread();
             origName = thread.getName();
             thread.setName(threadName);
        }

        try {
            doRun();
        } catch (Exception ex) {
            if (Throwables.getRootCause(ex) instanceof Error) {
                Runtime.goDownWithError(ex, ERROR_EXIT_CODE);
            }
            if (lenient) {
                LOGGER.error("Exception in runnable: ", ex);
            } else {
                throw new RuntimeException(ex);
            }
        } catch (Throwable ex) {
           Runtime.goDownWithError(ex, ERROR_EXIT_CODE);
        } finally {
            if (thread != null) {
               thread.setName(origName);
            }
        }
    }

    public abstract void doRun() throws Exception;

}
