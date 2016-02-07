
package org.spf4j.io.tcp;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;


/**
 *
 * @author zoly
 */
public abstract class SelectorEventHandler {

    /**
     * Method must be invoked in selector thread.
     * THis will register this handler to Selector + channel.
     * @return
     * @throws ClosedChannelException
     */
    public abstract SelectionKey initialInterestRegistration() throws ClosedChannelException;

    public abstract boolean canRunAsync();

    public abstract void runAsync(SelectionKey key)
            throws IOException;

    public abstract void run(SelectionKey key) throws IOException;

}
