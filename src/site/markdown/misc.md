# Other components shipped part of the spf4j library.

## High Performance Object Pool

### Why another object pool implementation?

 In my(I am not alone) view current available object pool implementations are less than perfect.
 Beside the scalability issues and bugs, this implementation improves upon:

   * Test on borrow is pointless, there is no guarantee that you will get a valid object even if you test on borrow.
     This encourages the developer to disregard handling of this case when it receives an invalid object.
     This practice also often is a performance issue, and it is not supported by the spf4j implementation.

   * Indiscriminate test on return is not optimal either.
     Test on return should be done only in the case where there is a suspicion that the object is invalid,
     otherwise the performance impact will be too high to be acceptable in most cases.
     Pool client should be able to provide feedback on return for that case, and as such the client is able to provide
     encountered error information to the pool on return. As such the pool can test the connection before making it available again to a borrower.

   * Exception swallowing and root cause loss. The spf4j connection pool makes sure errors  and root causes are not dropped.

### Use case

 The classical use case for an object pool is to pool your jdbc connections or any type of network connection.

### How to use the object pool

 Creating a pool is simple:

```
RecyclingSupplier<ExpensiveTestObject> pool = new RecyclingSupplierBuilder(10, new ExpensiveTestObjectFactory()).build();
```

 at minimum you will need to provide the maximum size and the object factory.
 To do something with a object from a pool you will need to:

```
Template.doOnSupplied(new Handler<PooledObject, SomeException>() {
   @Override
   public void handle( PooledObject object, long deadline) throws SomeException {
   object.doStuff();
   }
}, pool, imediateRetries, maxBackoffDelay, timeout );
```

 You can also use the get and recycle methods on the object pool to get and return an object to the pool.

### How does the pool work?

 The architecture above biases object to threads, so a thread will most likely get the same object minimizing
 object contention (if pool size >= thread nr which is the recommended sizing).

```
      [code context]  <----> [Thread Local Pool] <----> [Global Pool]
```

 On the other hand this pool implementation will prefer to create a new connection
 instead of reusing a connection that has already has been used by another thread.
 This is alleviated by the maintenance process which can bring back unused local object to the global pool.

## The Execution Context.

 When building systems often the need arises to have parameters that address system wide concerns passed across
 function invocation transparently. (ThreadLocals) Some examples in practice are JDBC transaction contexts(Spring)
 tracing information (Opentrace/Jaeger/...). In spf4j the main use case for something like this arised for
 the propagation of deadlines, which are fundamental when coding distributed systems.

 as such the ExecutionContext can be easily created like:

 ```
 try (ExecutionContext ctx = ExecutionContexts.start("operation_name", timeout, timeUnit)) {
   ....
 }

 ```

 the execution context can be retrieved with:

 ```
    ExecutionContext ctx = ExecutionContexts.current();
 ```

 Execution contexts are transfered across Thread boundaries with: ContextPropagatingExecutorService
 (can wrap any executorservice) or ContextPropagationgCompletableFuture or
 you can create context transfering Callables and Runnables with ExecutorContexts utility methods.

 Execution context implementations are customizeable with the system property: spf4j.execContentFactoryClass
 where you can specify your custom ExecutionContextFactory implementation.


## Retry/failure handling utilities.

 Although there are other libraries that provide this functionality, I haven't found any that can do:

   * 1) No Exception lost + ability to propagate checked exceptions (Sync mode only).
   * 2) Retry operation can be different from original operation. (redirect, fallback, etc...)
   * 3) The retry operation can be executed with delay which can be a function of the response or exception.
   * 4) Timeouts are core functionality. (Support)
   * 5) sync and async retry capabilities.
   * 6) ability to propagate checked exceptions.

 Here is example:

 ```
         RetryPolicy.<Response, ServerCall>newBuilder()
            .withDeadlineSupplier((c) -> c.getDeadlineNanos())
            .withDefaultThrowableRetryPredicate() // use known transient exceptions.
            .withResultPartialPredicate((resp, sc) -> {
              switch (resp.getType()) {
                case CLIENT_ERROR:
                  return RetryDecision.abort();
                case REDIRECT:
                  return RetryDecision.retry(0, new ServerCall(sc.getServer(),
                          new Request((String) resp.getPayload(), sc.getRequest().getDeadlineMSEpoch())));
                case RETRY_LATER:
                  return RetryDecision.retry(
                          TimeUnit.NANOSECONDS.convert((Long) resp.getPayload() - System.currentTimeMillis(),
                                  TimeUnit.MILLISECONDS), sc);
                case TRANSIENT_ERROR:
                  return RetryDecision.retryDefault(sc);
                case ERROR:
                  return null;
                case OK:
                  return RetryDecision.abort();
                default:
                  throw new IllegalStateException("Unsupported " + resp.getType());
              }
            }).withResultPartialPredicate((resp, sc)
            -> (resp.getType() == Response.Type.ERROR)
            ? RetryDecision.retryDefault(sc)
            : RetryDecision.abort(), 3)
            .withExecutorService(es)
            .build().call(serverCall, IOException.class);
  ```

## Other utilities

 Lifo Threadpool: org.spf4j.concurrent.LifoThreadPoolBuilder

 Retry utility implementation: see org.spf4j.base.Callables and org.spf4j.concurrent.RetryExecutor

 Union: see org.spf4j.base.Either

 Unique ID and Scalable sequence generators: org.spf4j.concurrent.UIDgenerator and org.spf4j.concurrent.ScalableSequence

 Csv: org.spf4j.io.Csv, org.spf4j.avro.csv.CsvEncoder, org.spf4j.avro.csv.CsvDecoder

 IPC: org.spf4j.concurrent.FileBasedLock

 Data Structures: org.spf4j.ds.RTree; org.spf4j.ds.Graph; org.spf4j.ds.UpdateablePriorityQueue

 Process control: org.spf4j.base.Runtime

 Object recyclers: org.spf4j.recyclable.impl.*

 Concurrency: org.spf4j.io.PipedOutputStream

 String performance utilities: org.spf4j.base.Strings

 NIO TCP proxy server: org.spf4j.io.tcp.proxy.*

 Distributed semaphore: org.spf4j.concurrent.jdbc.JdbcSemaphore

