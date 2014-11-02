
package org.spf4j.stackmonitor;

import java.util.ArrayList;
import java.util.List;

public final class DemoTest {


    private static volatile boolean stopped;


    public static void stopTestThreads(final List<Thread> threads) throws InterruptedException {
        stopped = true;
        for (Thread t : threads) {
            t.join();
        }
    }

    public static List<Thread> startTestThreads(final int nrThreads) {
        stopped = false;
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < nrThreads; i++) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (!stopped) {
                            doStuff();
                            Thread.sleep(1);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                }

                private double doStuff() {
                    return getStuff(10) * getStuff(10) * getStuff(10) * getStuff(10);
                }

                private double getStuff(final double nr) {
                    return Math.exp(nr);
                }
            }, "Thread" + i);
            t.start();
            threads.add(t);
        }
        return threads;

    }
}
