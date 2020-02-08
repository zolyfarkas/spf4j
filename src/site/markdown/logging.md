# Logging


## Overview

Logging is a core aspect of every application.
The recommended approach to logging is [structured logging](https://stackify.com/what-is-structured-logging-and-why-developers-need-it/).

To have have efficient and structured logging, your application needs to log to files.
(The console is not ideal for structured data due to various libraries, including JVM using it for messages in various formats...)
One good approach is to log to avro binary files compressed with snappy. An additional benefit is that you can leverage your hadoop
stack to analyze your logs. The log record schema is described [at](https://zolyfarkas.github.io/core-schema/avrodoc.html#/schema/org.spf4j.base.avro.LogRecord)


Let's look at how logging structured data is done in detail:

* Let's call the demo app endpoint with debug logging on:

```
curl -X POST "https://demo.spf4j.org/avql/query" -H "accept: application/json" -H "log-level: DEBUG" -H "Content-Type: text/plain" -d "select * from planets"
```

* And take a look at [logs](https://demo.spf4j.org/logs/cluster?_Accept=application/json):

```json
{
  "origin": "jaxrs-spf4j-demo-5f846dbdc8-nmg2k:/var/log/jaxrs-spf4j-demo-5f846dbdc8-nmg2k_2020-02-08.avro:7622",
  "trId": "XNm4uQJip.1.12uqj7.m0l",
  "level": "INFO",
  "ts": "2020-02-08T12:28:22.848Z",
  "logger": "org.spf4j.demo.resources.aql.PlanetsResourceImpl->getData",
  "thr": "http:8080-worker-2",
  "msg": "returning",
  "xtra": [[
    {
    "name": "earth",
    "planetClass": "M",
    "age": 512731872312,
    "description": ""
    },
    {
    "name": "vulcan",
    "planetClass": "M",
    "age": 612731872312,
    "description": ""
    },
    {
    "name": "andoria",
    "planetClass": "M",
    "age": 602731872312,
    "description": ""
    }
  ]],
  "attrs": {
  "origLevel": "DEBUG"
  }
},

```

* Let's look at the [code](https://github.com/zolyfarkas/spf4j-jaxrs/blob/master/spf4j-jaxrs-server/src/main/java/org/spf4j/jaxrs/server/Spf4jInterceptionService.java#L150) that logs the above:

```java
  import org.spf4j.log.ExecContextLogger;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  ...
  log = new ExecContextLogger(LoggerFactory.getLogger("org.spf4j.demo.resources.aql.PlanetsResourceImpl->getData"));
  ...
  log.debug("returning", result);
```

   * The wrapping of the slf4j logger with ExecContextLogger allows us to enable debug logging at request level (-H "log-level: DEBUG").

   * In the log.debug statement, the result object is not part of he message format as such instead
     of being dropped by logback (by default), it is logged in json format.
     Any parameter that is not an argument for the message format will be logged in json format. This is implemented using
     [Slf4jMessageFormatter.java](https://github.com/zolyfarkas/spf4j/blob/master/spf4j-core/src/main/java/org/spf4j/base/Slf4jMessageFormatter.java)
     and the and appender [from](https://github.com/zolyfarkas/spf4j-logback).


## Components

### Test logger

 A slf4j logging backend that is optimal to be used for your unit and integration tests.
 It minimizes the amount of useless detail spewed to the console by providing it only on test failure, allows you to assert
 your logging behavior and more. For more detail [see](http://www.spf4j.org/spf4j-slf4j-test/index.html)

### Avro and json logback encoders and appenders

  * log to avro binary files. see[schema](https://zolyfarkas.github.io/core-schema/avrodoc.html#/schema/org.spf4j.base.avro.LogRecord)
  * [Json Encoder](https://github.com/zolyfarkas/spf4j-logback/blob/master/src/main/java/org/spf4j/log/AvroLogbackEncoder.java) to use with any lockback appender.

 Logging to console as it is being rec

### Utilities in spf4-core library

  * High performance [java.util -> slf4j bridge](https://github.com/zolyfarkas/spf4j/blob/master/spf4j-core/src/main/java/org/spf4j/log/SLF4JBridgeHandler.java)
  * High performance and more flexible [Slf4j message formatter](https://github.com/zolyfarkas/spf4j/blob/master/spf4j-core/src/main/java/org/spf4j/base/Slf4jMessageFormatter.java).
  * Execution Context aware [logging wrapper](https://github.com/zolyfarkas/spf4j/blob/master/spf4j-core/src/main/java/org/spf4j/log/ExecContextLogger.java) for "DEBUG on error" logging.
  * A more compact and richer string representation of stack traces. [see](https://github.com/zolyfarkas/spf4j/blob/master/spf4j-core/src/main/java/org/spf4j/base/Throwables.java)

### REST endpoints for serving your logs:

  *  [process level log serving.](https://github.com/zolyfarkas/spf4j-jaxrs/tree/master/spf4j-jaxrs-actuator/src/main/java/org/spf4j/actuator/logs)
  *  [cluster level log serving.](https://github.com/zolyfarkas/spf4j-jaxrs/tree/master/spf4j-jaxrs-actuator-cluster/src/main/java/org/spf4j/actuator/cluster/logs)
