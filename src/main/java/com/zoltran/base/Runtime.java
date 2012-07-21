/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zoly
 */
public final class Runtime {
 
    private Runtime () {}
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Runtime.class);

    
    public static  void goDownWithError(Throwable t, int exitCode) {
        try {
            LOGGER.error("Unrecoverable Error, going down", t);
        } finally {
            try {
                t.printStackTrace();
            } finally {
                System.exit(exitCode);
            }
        }
    }
    
}
