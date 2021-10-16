                  
# Spf4j (Simple performance framework for java)
                  

## Overview

 The spf4j libraries is a collection of components designed to improve the observability and performance of java applications.
 The spf4j library covers the 3 pillars of observability [(Logging, Metrics, Tracing)](https://www.oreilly.com/library/view/distributed-systems-observability/9781492033431/ch04.html)
 and adds a 4-th: [Profiling](profiling.html). The goal of spf4j is to help with the right balance between observability data and observer effect.

 Additionally this library contains useful components that are currently not available in
 the right form in other open source libraries ([Retry/Hedged execution](http://www.spf4j.org/spf4j-core/xref/index.html), object pool,
 [test logging backend](http://www.spf4j.org/spf4j-slf4j-test/index.html) ...)
 This library also contains ZEL, a simple expression language that can be easily embedded in any java application.
 ZEL is easy to extend to your needs and also has some cool features like async functions, memorization...
 You can learn more by checking out the [spf4j-zel](http://www.spf4j.org/spf4j-zel/index.html) module.

## License and Contributions

 This library is [LGPL](http://www.gnu.org/licenses/lgpl.html)
 and [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) licensed.

 Contributions/Contributors to spf4j are welcome, join the community: [![Gitter chat](https://badges.gitter.im/zolyfarkas/spf4j.png)](https://gitter.im/spf4j/Lobby).

## Code, Binaries, Build

 [SPF4J Github hosted repo](https://github.com/zolyfarkas/spf4j/)

 [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.spf4j/spf4j-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.spf4j/spf4j-core/)
 [![Coverity Badge](https://scan.coverity.com/projects/3158/badge.svg)](https://scan.coverity.com/projects/3158)
 [![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=org.spf4j%3Aspf4j&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.spf4j%3Aspf4j)
 [![Codacy Badge](https://api.codacy.com/project/badge/Grade/48b50176945242729f4386b05be8c8dc)](https://www.codacy.com/app/zolyfarkas/spf4j?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=zolyfarkas/spf4j&amp;utm_campaign=Badge_Grade)
 [![CI badge](https://app.travis-ci.com/zolyfarkas/spf4j.svg?branch=master)](https://app.travis-ci.com/github/zolyfarkas/spf4j)

## Architecture

 Observability at any scale.

 ![Monitoring Architecture](images/MonitoringDiagram.svg)

 Architecture is similar to how air planes are being [monitored](http://787updates.newairplane.com/24-7-Customer-Support/Connected-Flight),
 where the local storage is the functional equivalent of the black box, and its goal is to keep high resolution observability data
 for a limited amount of time. Just as with airplanes, lower resolution observability data should be sent in real time to a monitoring system.
 The monitoring system should be able to on demand "drill into" and get to the local detail(via /actuator).

 Observing a system changes the system. The code in spf4j is carefully written to be high performance and to provide minimum overhead / functionality.

 To achieve this we utilize:

 * Thread Local Counters for metrics, for a good evaluation see:
  [Concurrent counters by numbers](http://psy-lob-saw.blogspot.com/2013/06/java-concurrent-counters-by-numbers.html)
 * Binary avro files for metrics. [see](metrics.html)
 * Binary avro files for logs and optimized log formatters and bridges. [see](https://github.com/zolyfarkas/spf4j-logback) for more detail.
 * ExecutionContext attributed profiling, [see](https://github.com/zolyfarkas/jaxrs-spf4j-demo/wiki/ContinuousProfiling).

## Details

### [Metrics](metrics.html)

### [Profiling](profiling.html)

### [Logging](logging.html)

### [Other utilities](misc.html)

