/**
 * To be able to use the annotations in this package, you need to use the junit RunListener like:
 *
 *
 * {@code <plugin>
 *       <groupId>org.apache.maven.plugins</groupId>
 *       <artifactId>maven-surefire-plugin</artifactId>
 *       <configuration>
 *         <properties>
 *           <property>
 *             <name>listener</name>
 *             <value>org.spf4j.test.log.junit4.Spf4jTestLogRunListener</value>
 *           </property>
 *         </properties>
 *       </configuration>
 *     </plugin>}
 *
 * you can also use @RunWith(Spf4jTestLogJUnitRunner.class) but it is not as convenient.
 *
 */
package org.spf4j.test.log.annotations;
