
package org.spf4j.concurrent;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.spf4j.base.IntMath;

@State(Scope.Benchmark)
@Fork(2)
@Threads(value = 8)
public class CacheBenchmark {


    private static final CacheLoader<String, String> TEST_LOADER
            = new CacheLoader<String, String>() {

        @Override
        public String load(String key) throws Exception {
            return "TEST";
        }
    };

    private static final LoadingCache<String, String> GUAVA = CacheBuilder.newBuilder()
            .initialCapacity(16)
            .concurrencyLevel(16)
            .build(TEST_LOADER);
    private static final LoadingCache<String, String> SPF4J = new UnboundedLoadingCache<>(16, 16, TEST_LOADER);


    private static final IntMath.XorShift32 RND = new IntMath.XorShift32();

    @Benchmark
    public final String spf4jCache() {
        return SPF4J.getUnchecked("key" + (RND.nextInt() % 100));
    }

    @Benchmark
    public final String guavaCache() {
        return GUAVA.getUnchecked("key" + (RND.nextInt() % 100));
    }


}
