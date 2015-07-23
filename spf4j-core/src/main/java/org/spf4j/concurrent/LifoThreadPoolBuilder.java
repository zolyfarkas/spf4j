package org.spf4j.concurrent;

import java.util.ArrayDeque;
import java.util.Queue;
import static org.spf4j.concurrent.RejectedExecutionHandler.REJECT_EXCEPTION_EXEC_HANDLER;

/**
 *
 * @author zoly
 */
//CHECKSTYLE IGNORE HiddenField FOR NEXT 2000 LINES
public final class LifoThreadPoolBuilder {


    private String poolName;
    private int coreSize;
    private int maxSize;
    private int maxIdleTimeMillis;
    private Queue<Runnable> taskQueue;
    private int queueSizeLimit;
    private boolean daemonThreads;
    private int spinLockCount;
    private RejectedExecutionHandler rejectionHandler;
    private int threadPriority;
    private boolean mutable;
    private boolean jmxEnabled;



    private LifoThreadPoolBuilder() {
        poolName = "Lifo Pool";
        rejectionHandler = REJECT_EXCEPTION_EXEC_HANDLER;
        coreSize = 0;
        maxSize = Short.MAX_VALUE;
        maxIdleTimeMillis = 60000;
        taskQueue = new ArrayDeque<>(256);
        queueSizeLimit = 0;
        daemonThreads = false;
        spinLockCount = 1024;
        threadPriority = Thread.NORM_PRIORITY;
        mutable = false;
        jmxEnabled = false;
    }

    public static LifoThreadPoolBuilder newBuilder() {
        return new LifoThreadPoolBuilder();
    }

    public LifoThreadPoolBuilder withPoolName(final String poolName) {
        this.poolName = poolName;
        return this;
    }

    public LifoThreadPoolBuilder withCoreSize(final int coreSize) {
        this.coreSize = coreSize;
        return this;
    }

    public LifoThreadPoolBuilder withMaxSize(final int maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    public LifoThreadPoolBuilder withMaxIdleTimeMillis(final int maxIdleTimeMillis) {
        this.maxIdleTimeMillis = maxIdleTimeMillis;
        return this;
    }

    public LifoThreadPoolBuilder withTaskQueue(final Queue<Runnable> taskQueue) {
        this.taskQueue = taskQueue;
        return this;
    }

    public LifoThreadPoolBuilder withQueueSizeLimit(final int queueSizeLimit) {
        this.queueSizeLimit = queueSizeLimit;
        return this;
    }

    public LifoThreadPoolBuilder withDaemonThreads(final boolean daemonThreads) {
        this.daemonThreads = daemonThreads;
        return this;
    }

    public LifoThreadPoolBuilder withSpinLockCount(final int spinLockCount) {
        this.spinLockCount = spinLockCount;
        return this;
    }

    public LifoThreadPoolBuilder withRejectionHandler(final RejectedExecutionHandler rejectionHandler) {
        this.rejectionHandler = rejectionHandler;
        return this;
    }

    public LifoThreadPoolBuilder withThreadPriority(final int threadPriority) {
        this.threadPriority = threadPriority;
        return this;
    }

    public LifoThreadPoolBuilder mutable() {
        this.mutable = true;
        return this;
    }

    public LifoThreadPoolBuilder enableJmx() {
        this.jmxEnabled = true;
        return this;
    }

    public LifoThreadPool build() {
        LifoThreadPool result;
        if (mutable) {
            result = new MutableLifoThreadPoolExecutorSQP(poolName, coreSize, maxSize, maxIdleTimeMillis,
                            taskQueue, queueSizeLimit, daemonThreads, spinLockCount, rejectionHandler, threadPriority);
        } else {
            result = new LifoThreadPoolExecutorSQP(poolName, coreSize, maxSize, maxIdleTimeMillis,
                            taskQueue, queueSizeLimit, daemonThreads, spinLockCount, rejectionHandler, threadPriority);
        }
        if (jmxEnabled) {
            result.exportJmx();
        }
        return result;
    }



}
