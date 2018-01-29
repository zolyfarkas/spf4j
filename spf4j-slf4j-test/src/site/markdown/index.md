# spf4j-slf4j-test the module that allows you to leverage logging in unit tests.

## 1. Overview

 The spf4j-slf4j-test module is a backend implementation for the slf4j api.

 This is an opinionated logging backend implementation for testing(junit) with the following features:

 *  Readable and parse-able output.
 *  Fast. (logging is no reason to have slow builds)
 *  Ability to assert Logging behavior.
 *  Fail unit tests that log an Error by default (if respective Error logs are not asserted against).
 *  Make debug logs available on unit test failure. This helps performance a lot by not requiring you to run your unit tests
with tons of debug info dumped to output all the time. But making it available when you actually need it (Unit test failure)
 *  Easily change logging configuration via API.
 *  Uncaught exceptions from other threads will fail your tests. You can assert them if they are expected.
 *  No configurable format, It is the best format so everybody should be using it. Format will be evolved as needed.

## 2. How to use it.

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
              <value>org.spf4j.test.log.junit.DetailOnFailureRunListener</value> <!-- comma separate multiple listeners -->
            </property>
          </properties>
        </configuration>
      </plugin>

### Examples:

 Change LOG print config for a log category:

      try (HandlerRegistration printer = TestLoggers.config().print("my.log.package", Level.TRACE)) {
        ...
        LOG.error("Booo", new RuntimeException());
        ..
      }

 Assert that you expect message to be logged:

      LogAssert expect = TestLoggers.config().expect("org.spf4j.test", Level.ERROR,
                LogMatchers.hasFormat(Matchers.equalTo("Booo")));
      LOG.error("Booo", new RuntimeException());
      expect.assertObservation(); // whithout assert the unit test fails when logging an error.

 Assert uncaught exceptions:

      AsyncObservationAssert obs = TestLoggers.config().expectUncaughtException(Matchers.hasProperty("throwable",
              Matchers.any(IllegalStateException.class)));
      executor.execute(new Runnable() {

        @Override
        public void run() {
          throw new IllegalStateException("Yohoo");
        }
      });
      obs.assertObservation(5, TimeUnit.SECONDS);

 Global configuration system properties with the defaults:

      spf4j.testLog.rootPrintLevelIDE = DEBUG
      spf4j.testLog.rootPrintLevel = INFO


 The log format is:

      [ISO UTC Timestaamp] [LOG Level] [Markers(jsonstr/json obj)]? [LOGGER] [THREAD(jsonstr)] [MESSAGE(json str)] [Extra Objects(array<jsonstr>)]?
      [StackTrace] ?

 sample log messages:

      2018-01-25T19:55:06.080Z ERROR "TEST" o.s.t.l.TestLoggerFactoryTest "main" "Hello logger"
      2018-01-25T19:55:06.081Z ERROR "TEST" o.s.t.l.TestLoggerFactoryTest "main" "Hello logger 1"
      2018-01-25T19:55:06.081Z ERROR "TEST" o.s.t.l.TestLoggerFactoryTest "main" "Hello logger 1 2 3"
      2018-01-25T19:55:06.081Z ERROR "TEST" o.s.t.l.TestLoggerFactoryTest "main" "Hello logger 1 2 3"
      java.lang.RuntimeException
              at o.s.t.l.TestLoggerFactoryTest.logMarkerTests(TestLoggerFactoryTest.java:116)[test-classes/]
              at ^.testLogging(^:43)[^]
              at s.r.NativeMethodAccessorImpl.invoke0(Native Method)[:1.8.0_162]
              at ^.invoke(^:62)[^]
              at s.r.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)[^]
              at j.l.r.Method.invoke(Method.java:498)[^]
              ...
