
package org.spf4j.io.tcp;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import org.spf4j.ds.UpdateablePriorityQueue;

/**
 *
 * @author zoly
 */
public interface ClientHandler {

    void handle(Selector serverSelector, SocketChannel clientChannel, ExecutorService exec,
            BlockingQueue<Runnable> tasksToRunBySelector, UpdateablePriorityQueue<DeadlineAction> deadlineActions)
            throws IOException;

}
