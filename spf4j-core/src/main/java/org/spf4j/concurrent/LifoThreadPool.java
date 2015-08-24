/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.concurrent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;
import org.spf4j.jmx.JmxExport;

/**
 *
 * @author zoly
 */
public interface LifoThreadPool extends ExecutorService {

    void exportJmx();

    void unregisterJmx();

    @JmxExport
    int getMaxIdleTimeMillis();

    @JmxExport
    int getMaxThreadCount();

    @JmxExport
    @SuppressFBWarnings(value = "MDM_WAIT_WITHOUT_TIMEOUT", justification = "Holders of this lock will not block")
    int getNrQueuedTasks();

    @JmxExport
    String getPoolName();

    @JmxExport
    int getQueueSizeLimit();

    ReentrantLock getStateLock();

    Queue<Runnable> getTaskQueue();

    @JmxExport
    int getThreadCount();

    @JmxExport
    int getThreadPriority();

    @JmxExport
    boolean isDaemonThreads();

}
