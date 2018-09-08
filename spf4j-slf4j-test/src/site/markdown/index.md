# spf4j-slf4j-test the module that allows you to leverage logging in unit tests.

## 1. Overview

 The spf4j-slf4j-test module is a backend implementation for the slf4j api.

 This is an opinionated logging backend implementation for testing(junit) with the following features:

 *  Ability to assert Logging behavior.
 *  Readable and parse-able output.
 *  Fail unit tests that log an Error by default (if respective Error logs are not asserted against). (similar to the junit default of failing on exception by default)
 *  Make debug logs available on unit test failure. This helps performance a lot by not requiring you to run your unit tests
with tons of debug info dumped to output all the time. But making it available when you actually need it (Unit test failure)
 *  Easily change logging configuration via API.
 *  Uncaught exceptions from other threads will fail your tests. You can assert them if they are expected.
 *  No configurable format, It is the best format so everybody should be using it. Format will be evolved as needed.
 *  Ability to log other object payload additionally to the log message.
 *  Type level String image customization. (if unhappy with default toString, json format is desired, or performance optimization is desired)
 *  Fast. (logging is no reason to have slow builds)
 *  Lossless and fast java.util.logging redirect. (source class, source method ... are not being lost)
 *  Environment specific configurations with best defaults right out of the box. (DEBUG when running from IDE, INFO otherwise)
 *  Assert logging made from various logging APIs. java.util.logging supported out of the box, for everything else [see](https://www.slf4j.org/legacy.html)
 *  Ability to control your timing based on TimeSource.

## 2. How to use it.

### Pom changes:

 Add to your pom.xml dependency section (make sure it is ahead of other slf4j backends you might have in your classpath):

    <dependency>
      <groupId>org.spf4j</groupId>
      <artifactId>spf4j-slf4j-test</artifactId>
      <scope>test</scope>
      <version>LATEST</version>
    </dependency>

 Register the junit runListener in your surefire plugin config:

      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
          <properties>
            <property>
              <name>listener</name>
              <value>org.spf4j.test.log.junit4.Spf4jTestLogRunListener</value> <!-- comma separate multiple listeners -->
            </property>
          </properties>
        </configuration>
      </plugin>

 Or if your IDE is not smart enough (you InteliJ and Eclipse) to pick the run listeners up from your pom.xml,
 you can use the Spf4jTestLogJUnitRunner in conjunction junit @RunWith annotation. If you have CustomClassRunners already,
 you can easily upgrade them to register Spf4jTestLogRunListener as done in Spf4jTestLogJUnitRunner.

 NOTE: since logging is JVM global, you should run your unit tests single threaded to be able to easily reason about your logging,
 and more accurate log message attribution.

### Examples:

#### Assert that you expect message to be logged:

      LogAssert expect = TestLoggers.sys().expect("org.spf4j.test", Level.ERROR,
                LogMatchers.hasFormat("Booo"));
      LOG.error("Booo", new RuntimeException());
      expect.assertObservation(); // whithout assert the unit test fails when logging an error.

#### Assert that you expect message to be logged asynchronously:

      LogAssert expect = TestLoggers.sys().expect("org.spf4j.test.log", Level.ERROR, LogMatchers.hasFormat("async"));
      new Thread(() -> {
        LOG.error("async");
      }).start();
      expect.assertObservation(3, TimeUnit.SECONDS);

#### Assert uncaught exceptions:

      AsyncObservationAssert obs = TestLoggers.sys().expectUncaughtException(Matchers.hasProperty("throwable",
              Matchers.any(IllegalStateException.class)));
      executor.execute(new Runnable() {

        @Override
        public void run() {
          throw new IllegalStateException("Yohoo");
        }
      });
      obs.assertObservation(5, TimeUnit.SECONDS);

#### Collect LogRecords:

    try (LogCollection<Long> c = TestLoggers.sys().collect("org.spf4j.test", Level.INFO, true, Collectors.counting())) {
      LOG.info("m1");
      LOG.info("m2");
      Assert.assertEquals(2L, (long) c.get());
    }

#### Change LOG print config for a log category for a code section with API:

      try (HandlerRegistration printer = TestLoggers.sys().print("my.log.package", Level.TRACE)) {
        ...
        LOG.error("Booo", new RuntimeException());
        ..
      }

or for a unit test:


    @Test
    @PrintLogs(category = "org.spf4", ideMinLevel = Level.TRACE)
    public void testSomeHandler2() {
      ....
      LOG.trace("test");
      ....
    }

or a more complex print config:

      @Test
      @PrintLogsConfigs(
              {
                @PrintLogs(ideMinLevel = Level.TRACE),
                @PrintLogs(category = "com.sun", ideMinLevel = Level.WARN)
              }
      )
      public void testLogging() {
         ....


#### Log Additional objects:

      LOG.debug("log {} {} {}", 1, 2, 3, /* extra object */ 4);

 will be logged as:

      09:23:32.731 DEBUG o.s.t.l.TestLoggerFactoryTest "main" "log 1 2 3" ["4"]


#### Customize String image that is logged.

 If your objects implement JsonWriteable or you register a Json serializer for a particular type like:

       LogPrinter.getAppenderSupplier().register(TestLoggerFactoryTest.class,
            MimeTypes.APPLICATION_JSON, (o, a) -> {a.append("{\"a\" : \"b\"}");});
       LOG.info("Json Payload", this); // this will be not part of the messages string and will be logged as extra payload

 will be logged like:

       12:13:00.656 INFO o.s.t.l.TestLoggerFactoryTest "main" "Json Payload" [{"a" : "b"}]


#### Debug detail on demand.

 For example if you have spf4j.testLog.rootPrintLevel=DEBUG and you want everything
 above trace available if a unit test fails, you can either set globaly spf4j.test.log.collectMinLevel=TRACE or you can
 control this at test level like:

      @Test
      @CollectTrobleshootingLogs(minLevel = Level.TRACE)
      public void testLogging4() {
        LOG.trace("lala");
        LOG.debug("log {}", 1);
        LOG.debug("log {} {}", 1, 2);
        LOG.debug("log {} {} {}", 1, 2, 3);
        LOG.debug("log {} {} {}", 1, 2, 3, 4);
        Assert.fail("booo");
      }

 Will result in the following output:

      Running org.spf4j.test.log.TestLoggerFactoryTest
      09:23:32.691 DEBUG o.s.t.l.TestLoggerFactoryTest "main" "log 1"
      09:23:32.731 DEBUG o.s.t.l.TestLoggerFactoryTest "main" "log 1 2"
      09:23:32.731 DEBUG o.s.t.l.TestLoggerFactoryTest "main" "log 1 2 3"
      09:23:32.731 DEBUG o.s.t.l.TestLoggerFactoryTest "main" "log 1 2 3" ["4"]
      09:23:32.759 INFO o.s.t.l.j.Spf4jTestLogRunListenerSingleton "main" "Dumping last 100 unprinted logs for testLogging4(org.spf4j.test.log.TestLoggerFactoryTest)"
      09:23:32.691 TRACE o.s.t.l.TestLoggerFactoryTest "main" "lala"
      09:23:32.759 INFO o.s.t.l.j.Spf4jTestLogRunListenerSingleton "main" "End dump for testLogging4(org.spf4j.test.log.TestLoggerFactoryTest)"
      testLogging4(org.spf4j.test.log.TestLoggerFactoryTest)  Time elapsed: 0.054 s  <<< FAILURE!
      java.lang.AssertionError: booo
              at org.junit.Assert.fail(Assert.java:88)
              at org.spf4j.test.log.TestLoggerFactoryTest.testLogging4(TestLoggerFactoryTest.java:200)
              at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
              at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)


#### Available global configuration system properties with the defaults:

      # default root log level when tests executed from IDE. see TestUtils.class for more info.
      spf4j.testLog.rootPrintLevelIDE = DEBUG
      # default root log level.
      spf4j.testLog.rootPrintLevel = INFO
      # default log level collected for availability when a unit test fails.
      spf4j.test.log.collectMinLevel = DEBUG
      # maximum number of logs to collect for availability in case of a failure. (by default only unprinted logs are collected)
      spf4j.test.log.collectmaxLogs = 100
      # collect printed logs.
      spf4j.test.log.collectPrintedLogs

#### Configuring the default log printing configuration.

 You can add to your test resources a file with the name spf4j-test-prtcfg.properties or spf4j-test-prtcfg-ide.properties if you want
 different configuration when executing your tests from the IDE. The file format is a property file with key=values in the format:

      [category(package) name]=[LOG,LEVEL](,[greedy])?

#### Customized timing for testing.
 
 You can control TimeSource like:

        @Test
        public void testAssertionError() {
          TestTimeSource.setTimeStream(0L, 1L, 2L);
          Assert.assertEquals(0L, TimeSource.nanoTime());
          Assert.assertEquals(1L, TimeSource.nanoTime());
          ....

#### The log format is:

      [ISO UTC Timestaamp] [LOG Level] [Markers(jsonstr/json obj)]? [LOGGER] [THREAD(jsonstr)] [MESSAGE(json str)] [Extra Objects(array<json>)]?
      [StackTrace]?

 sample log messages:

      2018-01-25T19:55:06.080Z ERROR o.s.t.l.TestLoggerFactoryTest "main" "Hello logger"
      2018-01-25T19:55:06.081Z ERROR o.s.t.l.TestLoggerFactoryTest "main" "Hello logger 1"
      2018-01-25T19:55:06.081Z ERROR o.s.t.l.TestLoggerFactoryTest "main" "Hello logger 1 2 3"
      2018-01-25T19:55:06.081Z ERROR o.s.t.l.TestLoggerFactoryTest "main" "Hello logger 1 2 3" ["4"]
      java.lang.RuntimeException
              at o.s.t.l.TestLoggerFactoryTest.logMarkerTests(TestLoggerFactoryTest.java:116)[test-classes/]
              at ^.testLogging(^:43)[^]
              at s.r.NativeMethodAccessorImpl.invoke0(Native Method)[:1.8.0_162]
              at ^.invoke(^:62)[^]
              at s.r.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)[^]
              at j.l.r.Method.invoke(Method.java:498)[^]
              ...
