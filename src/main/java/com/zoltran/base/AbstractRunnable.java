
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
package com.zoltran.base;

import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author zoly
 */
public abstract class AbstractRunnable implements Runnable {

    protected final boolean lenient;

    public AbstractRunnable(boolean lenient) {
        this.lenient = lenient;
    }

    public AbstractRunnable() {
        this(false);
    }  
    
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRunnable.class);
    
    @Override
    public final void run() {
        try {
            doRun();
        } 
        catch (Exception ex) {
            if (Throwables.getRootCause(ex) instanceof Error) {
                Runtime.goDownWithError(ex, 666);
            }
            if (lenient) {
                LOGGER.warn("Exception in runnable: ", ex);
            } else {
                LOGGER.error("Exception in runnable: ", ex);
                throw new RuntimeException(ex);
            }
        }
        catch (Throwable ex) {
           Runtime.goDownWithError(ex, 666);
        }
    }
    
    public abstract void doRun() throws Exception;
    
}
