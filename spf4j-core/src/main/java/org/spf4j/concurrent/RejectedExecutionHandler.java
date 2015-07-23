
package org.spf4j.concurrent;

import java.util.concurrent.RejectedExecutionException;

/**
 *
 * @author zoly
 */
public interface RejectedExecutionHandler {

    void rejectedExecution(Runnable r, LifoThreadPool executor);

    RejectedExecutionHandler REJECT_EXCEPTION_EXEC_HANDLER = new RejectedExecutionHandler() {

        @Override
        public void rejectedExecution(final Runnable r, final LifoThreadPool executor) {
            throw new RejectedExecutionException();
        }
    };

}
