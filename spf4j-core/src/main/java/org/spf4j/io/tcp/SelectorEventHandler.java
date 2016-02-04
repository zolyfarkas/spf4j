
package org.spf4j.io.tcp;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;


/**
 *
 * @author zoly
 */
public abstract class SelectorEventHandler {

    public abstract SelectionKey initialInterestRegistration() throws ClosedChannelException;

    public abstract boolean canRunAsync();

    public abstract void runAsync()
            throws IOException;

    public abstract void run() throws IOException;

}
