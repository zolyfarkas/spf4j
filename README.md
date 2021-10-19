spf4j
=====

Simple performance framework for java

Available on [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.spf4j/spf4j-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.spf4j/spf4j-core/)

Join the discussion on Gitter: [![Gitter chat](https://badges.gitter.im/zolyfarkas/spf4j.png)](https://gitter.im/spf4j/Lobby)

Coverity: [![Coverity Badge](https://scan.coverity.com/projects/3158/badge.svg)](https://scan.coverity.com/projects/3158)

Codacy: [![Codacy Badge](https://api.codacy.com/project/badge/Grade/48b50176945242729f4386b05be8c8dc)](https://www.codacy.com/app/zolyfarkas/spf4j?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=zolyfarkas/spf4j&amp;utm_campaign=Badge_Grade)

SonarCloud: [![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=org.spf4j%3Aspf4j&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.spf4j%3Aspf4j)

Travis Continuous integration: [![CI badge](https://app.travis-ci.com/zolyfarkas/spf4j.svg?branch=master)](https://app.travis-ci.com/github/zolyfarkas/spf4j)

Github Continuous Integration: [![Java CI](https://github.com/zolyfarkas/spf4j/actions/workflows/maven.yml/badge.svg)](https://github.com/zolyfarkas/spf4j/actions/workflows/maven.yml)

see more detail at: http://zolyfarkas.github.com/spf4j/


# Build/DEV of the spf4j libraries.

IDE Preference: Netbeans.
For Eclipse you need https://bugs.eclipse.org/bugs/show_bug.cgi?id=538885 implemented for best experience.
For InteliJ you need https://youtrack.jetbrains.com/oauth?state=%2Fissue%2FIDEA-190385  implemented for best experience.

Build with Maven 3.5.2
Run maven with JDK 1.8

To build project all you need to do is: mvn install

Spf4j builds against [an avro fork](https://github.com/zolyfarkas/avro) which is published to github repos.
Until Github removes the authentication requirements, you will need to configure
your authentication credentials in your settings.xml. ([see](https://github.com/zolyfarkas/avro) the avro fork readme for more detail)

Please use -Dgpg.skip=true argument is you do not have gpg installed or configured.

You can run the spf4j unit tests with JDK 11 by activating the jdk-11-validation profile. (-P jdk-11-validation profile)
please define java11.home property with the home of JDK 11 in your settings.xml

See pom.xml for other profiles for openjdk, zolyfarkas/avro fork validations.


When running on java 9 or higher you will receive warnings like:

```
WARNING: Illegal reflective access by  ...
```

you can remove these warnings by adding to you java command line:


```
--add-opens=java.base/java.lang=ALL-UNNAMED
```

