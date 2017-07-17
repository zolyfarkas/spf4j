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
package org.spf4j.stackmonitor;

import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.base.CharSequences;
import org.spf4j.base.IntMath;
import org.spf4j.concurrent.DefaultExecutor;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;
import org.spf4j.perf.memory.GCUsageSampler;
import org.spf4j.ssdump2.Converter;

/**
 * Utility to sample stack traces. Stack traces can be persisted for later analysis.
 *
 * please read http://sape.inf.usi.ch/sites/default/files/publication/pldi10.pdf pure java stack sampling will probably
 * have safepoint bias.
 *
 * @author zoly
 */
@ThreadSafe
public final class Sampler {

  private static final int STOP_FLAG_READ_MILLIS = Integer.getInteger("spf4j.perf.ms.stopFlagReadMIllis", 2000);

    public static final String DEFAULT_SS_DUMP_FOLDER = System.getProperty("spf4j.perf.ms.defaultSsdumpFolder",
                System.getProperty("java.io.tmpdir"));

    public static final String DEFAULT_SS_DUMP_FILE_NAME_PREFIX = CharSequences.validatedFileName(
            System.getProperty("spf4j.perf.ms.defaultSsdumpFilePrefix",
                    ManagementFactory.getRuntimeMXBean().getName()));



  private static final StackTraceElement[] GC_FAKE_STACK = new StackTraceElement[]{
    new StackTraceElement("java.lang.System", "gc", "System.java", -1)
  };

  private static final DateTimeFormatter TS_FORMAT = ISODateTimeFormat.basicDateTimeNoMillis();

  private volatile boolean stopped;
  private volatile int sampleTimeMillis;
  private volatile int dumpTimeMillis;
  private final StackCollector stackCollector;
  private volatile long lastDumpTime = System.currentTimeMillis();

  @GuardedBy("this")
  private Future<?> samplerFuture;

  private final String filePrefix;

  private final IntMath.XorShift32 random = new IntMath.XorShift32();

  @Override
  public String toString() {
    return "Sampler{" + "stopped=" + stopped + ", sampleTimeMillis="
            + sampleTimeMillis + ", dumpTimeMillis=" + dumpTimeMillis + ", lastDumpTime="
            + lastDumpTime + ", filePrefix=" + filePrefix + '}';
  }

  public Sampler() {
    this(10, 3600000, new FastStackCollector(false));
  }

  public Sampler(final int sampleTimeMillis) {
    this(sampleTimeMillis, 3600000, new FastStackCollector(false));
  }

  public Sampler(final StackCollector collector) {
    this(10, 3600000, collector);
  }

  public Sampler(final int sampleTimeMillis, final int dumpTimeMillis, final StackCollector collector) {
    this(sampleTimeMillis, dumpTimeMillis, collector,
            DEFAULT_SS_DUMP_FOLDER, DEFAULT_SS_DUMP_FILE_NAME_PREFIX);
  }

  public Sampler(final int sampleTimeMillis, final int dumpTimeMillis, final StackCollector collector,
          final String dumpFolder, final String dumpFilePrefix) {
    CharSequences.validatedFileName(dumpFilePrefix);
    stopped = true;
    if (sampleTimeMillis < 1) {
      throw new IllegalArgumentException("Invalid sample time " + sampleTimeMillis);
    }
    this.sampleTimeMillis = sampleTimeMillis;
    this.dumpTimeMillis = dumpTimeMillis;
    this.stackCollector = collector;
    this.filePrefix = dumpFolder + File.separator + dumpFilePrefix;
  }

  public void registerJmx() {
    Registry.export(this);
  }

  @JmxExport(description = "start stack sampling")
  public synchronized void start() {
    if (stopped) {
      stopped = false;
      final int stMillis = sampleTimeMillis;
      final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
      samplerFuture = DefaultExecutor.INSTANCE.submit(new AbstractRunnable("SPF4J-Sampling-Thread") {

        @SuppressWarnings("SleepWhileInLoop")
        @SuppressFBWarnings("MDM_THREAD_YIELD")
        @Override
        public void doRun() throws IOException, InterruptedException {
          final Thread samplerThread = Thread.currentThread();
          int dumpCounterMs = 0;
          int coarseCounter = 0;
          int coarseCount = STOP_FLAG_READ_MILLIS / stMillis;
          boolean lstopped = stopped;
          long prevGcTime = 0;
          int sleepTime = 0;
          int halfStMillis = stMillis / 2;
          if (halfStMillis == 0) {
            halfStMillis = 1;
          }
          while (!lstopped) {
            stackCollector.sample(samplerThread);
            dumpCounterMs += sleepTime;
            coarseCounter++;
            if (coarseCounter >= coarseCount) {
              coarseCounter = 0;
              lstopped = stopped;
              long gcTime = GCUsageSampler.getGCTime(gcBeans);
              if (gcTime > prevGcTime) {
                int fakeSamples = (int) ((gcTime - prevGcTime)) / stMillis;
                for (int i = 0; i < fakeSamples; i++) { // can be optimized
                  stackCollector.addSample(GC_FAKE_STACK);
                }
                prevGcTime = gcTime;
              }
            }
            if (dumpCounterMs >= dumpTimeMillis) {
              long timeSinceLastDump = System.currentTimeMillis() - lastDumpTime;
              if (timeSinceLastDump >= dumpTimeMillis) {
                dumpCounterMs = 0;
                dumpToFile();
              } else {
                dumpCounterMs -= dumpTimeMillis - timeSinceLastDump;
              }
            }
            sleepTime = stMillis + (random.nextInt() % halfStMillis);
            Thread.sleep(sleepTime);
          }
        }
      });
    } else {
      throw new IllegalStateException("Sampling can only be started once for " + this);
    }

  }

  @JmxExport(description = "save stack samples to file")
  @Nullable
  public File dumpToFile() throws IOException {
    return dumpToFile((String) null);
  }

  /**
   * Dumps the sampled stacks to file. the collected samples are reset
   *
   * @param id - id will be added to file name returns the name of the file.
   * @return - the file name where the data was persisted or null if there was no data to persist.
   * @throws IOException - io issues while persisting data.
   */
  @JmxExport(value = "dumpToSpecificFile", description = "save stack samples to file")
  @Nullable
  @SuppressFBWarnings("PATH_TRAVERSAL_IN") // not possible the provided ID is validated for path separators.
  public synchronized File dumpToFile(
          @JmxExport(value = "fileID", description = "the ID that will be part of the file name")
          @Nullable final String id) throws IOException {
    String fileName = filePrefix + CharSequences.validatedFileName(((id == null) ? "" : '_' + id) + '_'
            + TS_FORMAT.print(lastDumpTime) + '_'
            + TS_FORMAT.print(System.currentTimeMillis()) + ".ssdump2");
    File file = new File(fileName);
    return dumpToFile(file);
  }

  @Nullable
  public File dumpToFile(@Nonnull final File file) throws IOException {
    Preconditions.checkArgument(file.getName().endsWith(".ssdump2"),
            "File name must have ssdump2 extension not %s", file);
    SampleNode collected = stackCollector.clear();
    if (collected != null) {
      Converter.save(file, collected);
      lastDumpTime = System.currentTimeMillis();
      return file;
    } else {
      return null;
    }
  }

  @JmxExport(description = "stop stack sampling")
  public synchronized void stop() throws InterruptedException {
    if (!stopped) {
      stopped = true;
      try {
        samplerFuture.get(STOP_FLAG_READ_MILLIS << 2, TimeUnit.MILLISECONDS);
      } catch (TimeoutException ex) {
        samplerFuture.cancel(true);
        throw new Spf4jProfilerException(ex);
      } catch (ExecutionException ex) {
        throw new Spf4jProfilerException(ex);
      }
    }
  }

  @JmxExport(description = "stack sample time in milliseconds")
  public int getSampleTimeMillis() {
    return sampleTimeMillis;
  }

  @JmxExport
  public void setSampleTimeMillis(final int sampleTimeMillis) {
    this.sampleTimeMillis = sampleTimeMillis;
  }

  @JmxExport(description = "is the stack sampling stopped")
  public boolean isStopped() {
    return stopped;
  }

  @JmxExport(description = "clear in memory collected stack samples")
  public void clear() {
    stackCollector.clear();
  }

  public StackCollector getStackCollector() {
    return stackCollector;
  }

  @PreDestroy
  public void dispose() throws InterruptedException {
    stop();
    Registry.unregister(this);
  }

  @JmxExport(description = "interval in milliseconds to save stack stamples periodically")
  public int getDumpTimeMillis() {
    return dumpTimeMillis;
  }

  @JmxExport
  public void setDumpTimeMillis(final int dumpTimeMillis) {
    this.dumpTimeMillis = dumpTimeMillis;
  }

}
