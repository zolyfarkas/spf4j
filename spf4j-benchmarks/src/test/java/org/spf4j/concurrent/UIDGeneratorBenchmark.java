
package org.spf4j.concurrent;

import java.util.UUID;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

@State(Scope.Benchmark)
@Fork(3)
@Threads(value = 8)
public class UIDGeneratorBenchmark {

    private static final UIDGenerator SCA_GEN = new UIDGenerator(new ScalableSequence(0, 100));
    
    private static final UIDGenerator ATO_GEN = new UIDGenerator(new AtomicSequence(0));
    
    @Benchmark
    public final String jdkUid() {
        return UUID.randomUUID().toString();
    }
    
    @Benchmark
    public final String scaUid() {
        return SCA_GEN.next();
    }
    
    @Benchmark
    public final String atoUid() {
        return ATO_GEN.next();
    }
    
}
