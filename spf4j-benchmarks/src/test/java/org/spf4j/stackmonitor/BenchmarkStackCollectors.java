
package org.spf4j.stackmonitor;

import java.util.List;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;

/**
 *
 * @author zoly
 */
@State(Scope.Benchmark)
@Fork(2)
@Threads(value = 1)
public class BenchmarkStackCollectors {

        private static final SimpleStackCollector SIMPLE = new SimpleStackCollector();
        private static final FastStackCollector FAST = new FastStackCollector(false);

        private static final MxStackCollector MX = new MxStackCollector();

        private static volatile List<Thread> testThreads;

        @Setup
        public static void setup() {
            testThreads = DemoTest.startTestThreads(8);
        }

        @TearDown
        public static void tearDown() throws InterruptedException {
            DemoTest.stopTestThreads(testThreads);
        }

        @Benchmark
        public final void testSimple() {
            SIMPLE.sample(Thread.currentThread());
        }

        @Benchmark
        public final void testFast() {
            FAST.sample(Thread.currentThread());
        }

        @Benchmark
        public final void testMx() {
            MX.sample(Thread.currentThread());
        }


}
