## spf4j-junit the module that allows to profile your usint tests.

 To enable the spf4j profilling, you will need to configure your maven-surefire-plugin to run the spf4j run listener:

        <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
          <properties>
            <property>
              <name>listener</name>
              <value>org.spf4j.junit.Spf4jRunListener</value>
            </property>
            <property>
              <name>spf4j.junit.sampleTimeMillis</name>
              <value>10</value>
            </property>
          </properties>
        </configuration>
      </plugin>

  and add spf4j-junit to your test classpath:

    <dependency>
      <groupId>org.spf4j</groupId>
      <artifactId>spf4j-junit</artifactId>
      <version>${spf4j.version}</version>
      <scope>test</scope>
    </dependency>

 after which by default your ssdump2 profiles will be available in /target/junit-ssdump.
 (you can inspect them with spf4j-ui)