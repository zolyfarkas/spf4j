
package org.spf4j.base;

import javax.annotation.Nullable;


/**
 *
 * @author zoly
 */
public final class Closeables {

    private Closeables() { }

    public static <T extends Exception> void closeAll(@Nullable final T throwed, final AutoCloseable ... closeables)
    throws T {
        T ex = throwed;
        for (AutoCloseable closeable : closeables) {
            try {
                closeable.close();
            } catch (Exception ex1) {
                if (ex == null) {
                    ex = (T) ex1;
                } else {
                    ex = (T) Throwables.suppress(ex1, ex);
                }
            }
        }
        if (ex != null) {
            throw ex;
        }

    }


}
