/**
 * (c) Zoltan Farkas 2012
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
            if (Throwables.getRootCause(ex) instanceof Error)
                Runtime.goDownWithError(ex, 666);
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
