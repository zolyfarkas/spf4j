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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.base.CharSequences;
import org.spf4j.base.DateTimeFormats;
import org.spf4j.base.TimeSource;
import org.spf4j.base.Timing;
import org.spf4j.concurrent.DefaultExecutor;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;
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

  private static final Logger LOG = LoggerFactory.getLogger(Sampler.class);

  private static Sampler instance;

  private static final int STOP_FLAG_READ_MILLIS = Integer.getInteger("spf4j.perf.ms.stopFlagReadMIllis", 2000);

  public static final String DEFAULT_SS_DUMP_FOLDER = System.getProperty("spf4j.perf.ms.defaultSsdumpFolder",
          System.getProperty("java.io.tmpdir"));

  public static final String DEFAULT_SS_DUMP_FILE_NAME_PREFIX = CharSequences.validatedFileName(
          System.getProperty("spf4j.perf.ms.defaultSsdumpFilePrefix",
                  ManagementFactory.getRuntimeMXBean().getName()));

  private volatile boolean stopped;
  private volatile int sampleTimeNanos;
  private volatile long dumpTimeNanos;
  private final SamplerSupplier stackCollectorSupp;
  private volatile long lastDumpTimeNanos;

  private final Object sync = new Object();

  @GuardedBy("sync")
  private ISampler stackCollector;

  @GuardedBy("this")
  private Future<?> samplerFuture;

  private final String filePrefix;

  private final File dumpFolder;

  @Override
  public String toString() {
    return "Sampler{" + "stopped=" + stopped + ", sampleTimeNanos="
            + sampleTimeNanos + ", dumpTimeNanos=" + dumpTimeNanos + ", lastDumpTimeNanos="
            + lastDumpTimeNanos + ", dumpFolder=" + dumpFolder + ", filePrefix=" + filePrefix + '}';
  }

  public Sampler() {
    this(10, 3600000, (t) -> new FastStackCollector(false, true, new Thread[]{t}));
  }

  public Sampler(final int sampleTimeMillis) {
    this(sampleTimeMillis, 3600000, (t) -> new FastStackCollector(false, true, new Thread[]{t}));
  }

  public Sampler(final int sampleTimeMillis, final SamplerSupplier collector) {
    this(sampleTimeMillis, 3600000, collector);
  }

  public Sampler(final SamplerSupplier collector) {
    this(10, 3600000, collector);
  }

  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  public Sampler(final int sampleTimeMillis, final int dumpTimeMillis) {
    this(sampleTimeMillis, dumpTimeMillis, (t) -> new FastStackCollector(false, true, new Thread[]{t}),
            DEFAULT_SS_DUMP_FOLDER, DEFAULT_SS_DUMP_FILE_NAME_PREFIX);
  }

  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  public Sampler(final int sampleTimeMillis, final int dumpTimeMillis, final SamplerSupplier collector) {
    this(sampleTimeMillis, dumpTimeMillis, collector,
            DEFAULT_SS_DUMP_FOLDER, DEFAULT_SS_DUMP_FILE_NAME_PREFIX);
  }


  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  public Sampler(final int sampleTimeMillis, final int dumpTimeMillis, final SamplerSupplier collector,
          final String dumpFolder, final String dumpFilePrefix) {
    this(sampleTimeMillis, dumpTimeMillis, collector, new File(dumpFolder), dumpFilePrefix);
  }

  public Sampler(final int sampleTimeMillis, final int dumpTimeMillis, final SamplerSupplier collector,
          final File dumpFolder, final String dumpFilePrefix) {
    CharSequences.validatedFileName(dumpFilePrefix);
    stopped = true;
    if (sampleTimeMillis < 1) {
      throw new IllegalArgumentException("Invalid sample time " + sampleTimeMillis);
    }
    this.sampleTimeNanos = (int) TimeUnit.MILLISECONDS.toNanos(sampleTimeMillis);
    if (sampleTimeNanos < 0) {
      throw new IllegalArgumentException("Invalid sample time " + sampleTimeMillis);
    }
    this.dumpTimeNanos = TimeUnit.MILLISECONDS.toNanos(dumpTimeMillis);
    this.stackCollectorSupp = collector;
    this.filePrefix = dumpFilePrefix;
    this.dumpFolder = dumpFolder;
  }

  public static synchronized Sampler getSampler(final int sampleTimeMillis,
          final int dumpTimeMillis,
          final File dumpFolder, final String dumpFilePrefix) throws InterruptedException {
    return getSampler(sampleTimeMillis, dumpTimeMillis,
            (t) -> new FastStackCollector(false, true, new Thread[]{t}), dumpFolder, dumpFilePrefix);
  }

  public static synchronized Sampler getSampler(final int sampleTimeMillis,
          final int dumpTimeMillis, final SamplerSupplier collector,
          final File dumpFolder, final String dumpFilePrefix) throws InterruptedException {
    if (instance == null) {
      try {
        instance = new Sampler(sampleTimeMillis, dumpTimeMillis, collector,
                dumpFolder.getCanonicalFile(), dumpFilePrefix);
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
      instance.registerJmx();
      return instance;
    } else {
      instance.dispose();
      try {
        instance = new Sampler(sampleTimeMillis, dumpTimeMillis, collector,
                dumpFolder.getCanonicalFile(), dumpFilePrefix);
      } catch (IOException ex) {
       throw new UncheckedIOException(ex);
      }
      instance.registerJmx();
      return instance;
    }
  }

  public void registerJmx() {
    Registry.export(this);
  }

  @JmxExport(description = "start stack sampling")
  public synchronized void start() {
    if (stopped) {
      stopped = false;
      final int stNanos = sampleTimeNanos;
      samplerFuture = DefaultExecutor.INSTANCE.submit(new AbstractRunnable("SPF4J-Sampling-Thread") {

        @SuppressWarnings("SleepWhileInLoop")
        @SuppressFBWarnings({"MDM_THREAD_YIELD", "PREDICTABLE_RANDOM"})
        @Override
        public void doRun() throws IOException, InterruptedException {
          lastDumpTimeNanos = TimeSource.nanoTime();
          synchronized (sync) {
            stackCollector = stackCollectorSupp.get(Thread.currentThread());
          }
          final ThreadLocalRandom random = ThreadLocalRandom.current();
          int dumpCounterNanos = 0;
          int coarseCounter = 0;
          int coarseCount = STOP_FLAG_READ_MILLIS / stNanos;
          boolean lstopped = stopped;
          int sleepTimeNanos = 0;
          int halfStNanos = stNanos / 2;
          if (halfStNanos == 0) {
            halfStNanos = 1;
          }
          int maxSleeepNanos = stNanos + halfStNanos;
          while (!lstopped) {
            synchronized (sync) {
              stackCollector.sample();
            }
            dumpCounterNanos += sleepTimeNanos;
            coarseCounter++;
            if (coarseCounter >= coarseCount) {
              coarseCounter = 0;
              lstopped = stopped;
            }
            if (dumpCounterNanos >= dumpTimeNanos) {
              long nanosSinceLastDump = TimeSource.nanoTime() - lastDumpTimeNanos;
              if (nanosSinceLastDump >= dumpTimeNanos) {
                dumpCounterNanos = 0;
                File dumpFile = dumpToFile();
                lastDumpTimeNanos = TimeSource.nanoTime();
                LOG.info("Stack samples written to {}", dumpFile);
              }
            }
            sleepTimeNanos = random.nextInt(halfStNanos, maxSleeepNanos);
            TimeUnit.NANOSECONDS.sleep(sleepTimeNanos);
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
            + DateTimeFormats.TS_FORMAT.format(Timing.getCurrentTiming().fromNanoTimeToInstant(lastDumpTimeNanos))
            + '_' + DateTimeFormats.TS_FORMAT.format(Instant.now()));
    File file = new File(dumpFolder, fileName);
    return dumpToFile(file);
  }

  @Nullable
  @SuppressFBWarnings("PATH_TRAVERSAL_IN") // not possible the provided ID is validated for path separators.
  public File dumpToFile(@Nonnull final File pfile) throws IOException {
    Map<String, SampleNode> collections;
    synchronized (sync) {
      collections = stackCollector.getCollectionsAndReset();
    }
    if (collections.isEmpty()) {
      return null;
    }
    if (collections.size() == 1) {
      Map.Entry<String, SampleNode> es = collections.entrySet().iterator().next();
      File file;
      if (pfile.getName().endsWith(".ssdump2")) {
        file = pfile;
      } else {
        file = new File(pfile.getParentFile(), pfile.getName() + es.getKey()  + ".ssdump2");
      }
      Converter.save(file, es.getValue());
      return file;
    } else {
      File file;
      if (pfile.getName().endsWith(".ssdump3")) {
        file = pfile;
      } else {
        file = new File(pfile.getParentFile(), pfile.getName() + ".ssdump3");
      }
      Converter.saveLabeledDumps(file, collections);
      return file;
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
    return (int) TimeUnit.NANOSECONDS.toMillis(sampleTimeNanos);
  }

  @JmxExport
  public void setSampleTimeMillis(final int sampleTimeMillis) {
    this.sampleTimeNanos = (int) TimeUnit.MILLISECONDS.toNanos(sampleTimeMillis);
  }

  @JmxExport(description = "is the stack sampling stopped")
  public boolean isStopped() {
    return stopped;
  }

  @JmxExport(description = "clear in memory collected stack samples")
  public void clear() {
    synchronized (sync) {
      stackCollector.getCollectionsAndReset();
    }
  }

  public Map<String, SampleNode> getStackCollectionsAndReset() {
    synchronized (sync) {
      return stackCollector.getCollectionsAndReset();
    }
  }

  public Map<String, SampleNode> getStackCollections() {
    synchronized (sync) {
      return stackCollector.getCollections();
    }
  }

  @PreDestroy
  public void dispose() throws InterruptedException {
    stop();
    Registry.unregister(this);
  }

  @JmxExport(description = "interval in milliseconds to save stack stamples periodically")
  public int getDumpTimeMillis() {
    return (int) TimeUnit.NANOSECONDS.toMillis(dumpTimeNanos);
  }

  @JmxExport
  public void setDumpTimeMillis(final int dumpTimeMillis) {
    this.dumpTimeNanos = TimeUnit.MILLISECONDS.toNanos(dumpTimeMillis);
  }

}
