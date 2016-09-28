package org.spf4j.avro;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.spf4j.base.Either;

/**
 *
 * @author zoly
 */
@State(Scope.Benchmark)
@Fork(2)
@Threads(value = 8)
public class EitherBenchmark {

  @State(Scope.Thread)
  public static class ThreadState1 {
    public String obj = "str";

    public Deque<Either<String, Object>> list = new ArrayDeque<>();
  }

  @State(Scope.Thread)
  public static class ThreadState2 {
    public Object obj = "str";

    public Deque<Object> list = new ArrayDeque();
  }

  @Benchmark
  public Object testEither(final ThreadState1 ts) throws InstantiationException, IllegalAccessException {
    Either<String, Object> left = Either.left(ts.obj);
    ts.list.add(left);
    Either<String, Object> get = ts.list.removeFirst();
    if (get.isLeft()) {
      return get.getLeft();
    } else {
      return "";
    }
  }

  @Benchmark
  public CharSequence testNaive(final ThreadState2 ts) throws IOException {
    ts.list.add(ts.obj);
    Object get = ts.list.removeFirst();
    if (get instanceof CharSequence) {
      return (CharSequence) get;
    } else {
      return "";
    }
  }





}
