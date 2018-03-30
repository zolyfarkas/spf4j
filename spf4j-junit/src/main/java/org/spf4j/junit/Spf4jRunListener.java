/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Additionally licensed with:
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.junit;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import javax.annotation.concurrent.NotThreadSafe;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.stackmonitor.FastStackCollector;
import org.spf4j.stackmonitor.Sampler;

/**
 * A Junit run listener that will collect performance profiles for your unit tests.
 *
 * Works only if unit tests are NOT executed in parallel inside the same JVM.
 *
 * To enable this Run listener with maven, you will need to configure your maven-surefire-plugin with:
 *
 *       <plugin>
 *       <groupId>org.apache.maven.plugins</groupId>
 *       <artifactId>maven-surefire-plugin</artifactId>
 *       <configuration>
 *         <properties>
 *           <property>
 *             <name>listener</name>
 *             <value>org.spf4j.junit.Spf4jRunListener</value>
 *           </property>
 *           <property>
 *             <name>spf4j.junit.sampleTimeMillis</name>
 *             <value>5</value>
 *           </property>
 *         </properties>
 *       </configuration>
 *     </plugin>
 *
 * and add spf4j-junit to your test classpath:
 *
 *   <dependency>
 *     <groupId>org.spf4j</groupId>
 *     <artifactId>spf4j-junit</artifactId>
 *     <version>${spf4j.version}</version>
 *     <scope>test</scope>
 *   </dependency>
 *
 * @author zoly
 */
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
@NotThreadSafe
public final class Spf4jRunListener extends RunListener {

  private static final Logger LOG = LoggerFactory.getLogger(Spf4jRunListener.class);

  private final Sampler sampler;

  private final File destinationFolder;

  private volatile File lastWrittenFile;

  public Spf4jRunListener() {
    sampler = new Sampler(Integer.getInteger("spf4j.junit.sampleTimeMillis", 5),
          Integer.getInteger("spf4j.junit.dumpAfterMillis", Integer.MAX_VALUE),
          (t) -> new FastStackCollector(false, true, new Thread[] {t}));

    destinationFolder = new File(System.getProperty("spf4j.junit.destinationFolder",
          "target/junit-ssdump"));

    if (!destinationFolder.mkdirs() && !destinationFolder.canWrite()) {
      throw new ExceptionInInitializerError("Unable to write to " + destinationFolder);
    }
  }

  @Override
  public void testFailure(final Failure failure)
          throws IOException {
    File dumpToFile = sampler.dumpToFile(new File(destinationFolder, failure.getTestHeader() + ".ssdump2"));
    if (dumpToFile != null) {
      LOG.info("Profile saved to {}", dumpToFile);
    }
  }

  @Override
  public void testFinished(final Description description)
          throws IOException {
    File dump = sampler.dumpToFile(new File(destinationFolder, description.getDisplayName() + ".ssdump2"));
    if (dump != null) {
      LOG.info("Profile saved to {}", dump);
      lastWrittenFile = dump;
    }
  }

  @Override
  public void testRunFinished(final Result result) throws InterruptedException {
    sampler.stop();
  }

  @Override
  public void testRunStarted(final Description description)  {
   sampler.start();
  }

  public Sampler getSampler() {
    return sampler;
  }

  public File getDestinationFolder() {
    return destinationFolder;
  }

  public File getLastWrittenFile() {
    return lastWrittenFile;
  }


  @Override
  public String toString() {
    return "Spf4jRunListener{" + "sampler=" + sampler + '}';
  }

}
