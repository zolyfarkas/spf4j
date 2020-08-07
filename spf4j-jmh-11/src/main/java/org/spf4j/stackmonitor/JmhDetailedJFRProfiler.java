/*
 * Copyright 2020 SPF4J.
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
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.TextResult;
import org.openjdk.jmh.runner.IterationType;
import org.openjdk.jmh.util.Utils;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import joptsimple.ValueConverter;
import org.openjdk.jmh.profile.ExternalProfiler;
import org.openjdk.jmh.profile.InternalProfiler;
import org.openjdk.jmh.profile.ProfilerException;

/**
 * A profiler based on Java Flight Recorder.
 * Inspired a bit by a JMH contrib from Jason Zaugg.
 * This implementation has been tested to work on a JDK 11 u8.
 * Relies on FlightRecorderMXBean being available, with the recorder option "destination" implemented.
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
public final class JmhDetailedJFRProfiler implements ExternalProfiler, InternalProfiler {


  private final File outDir;
  private final boolean debugNonSafePoints;
  private final String configName;
  private final Collection<String> flightRecorderOptions = new ArrayList<>();
  private PostProcessor postProcessor = null;

  private volatile boolean measurementStarted = false;
  private final List<File> generated = new ArrayList<>();
  private volatile long currentRecording;
  private File currentRecordingFile;
  private RecordingGranularity granularity;

  private final AtomicInteger measurementIterationCounter = new AtomicInteger(0);

  private final AtomicInteger warmupIterationCounter = new AtomicInteger(0);

  /**
   * required for serviceloader.
   */
  public JmhDetailedJFRProfiler() {
    this.outDir = new File(org.spf4j.base.Runtime.USER_DIR);
    this.debugNonSafePoints = true;
    this.configName = "profile";
  }


  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  public JmhDetailedJFRProfiler(final String initLine) throws
          ProfilerException {
    OptionParser parser = new OptionParser();

    parser.formatHelpWith(new CmdLineUtils.ProfilerOptionFormatter("jfr_detail"));

    OptionSpec<String> optDir = parser.accepts("dir",
            "Output directory to write jfr profile files.")
            .withRequiredArg().ofType(String.class).describedAs("dir");

    OptionSpec<String> optConfig = parser.accepts("configName",
            "Name of a predefined Flight Recorder configuration, e.g. profile or default.")
            .withRequiredArg().ofType(String.class).describedAs("name").defaultsTo("profile");

    OptionSpec<Boolean> optDebugNonSafePoints
            = parser.accepts("debugNonSafePoints",
                    "Gather cpu samples asynchronously, rather than with safepoint bias.")
                    .withRequiredArg().ofType(Boolean.class).describedAs("bool").defaultsTo(Boolean.TRUE);

    OptionSpec<Integer> optStackDepth = parser.accepts("stackDepth",
            "Maximum number of stack frames collected for each event.")
            .withRequiredArg().ofType(Integer.class).describedAs("frames");

    OptionSpec<RecordingGranularity> optGranularity = parser.accepts("detailLevel",
            "The detail granularity of the collected profiles. Can be iteration level,"
                    + " or warmup|measurement level aggregates")
            .withRequiredArg().ofType(RecordingGranularity.class)
            .withValuesConvertedBy(new GranularityValueConverter()).defaultsTo(RecordingGranularity.PER_ITERATION_TYPE)
            .describedAs("granularity");

    OptionSpec<String> optPostProcessor = parser.accepts("postProcessor",
            "The fully qualified name of a class that implements "
            + PostProcessor.class
            + ". This must have a public, no-argument constructor.")
            .withRequiredArg().ofType(String.class).describedAs("fqcn");

    OptionSet set = CmdLineUtils.parseInitLine(initLine, parser);

    try {
      this.granularity = optGranularity.value(set);
      this.debugNonSafePoints = optDebugNonSafePoints.value(set);
      this.configName = optConfig.value(set);
      if (set.has(optStackDepth)) {
        flightRecorderOptions.add("stackdepth="
                + optStackDepth.value(set));
      }
      if (!set.has(optDir)) {
        outDir = new File(org.spf4j.base.Runtime.USER_DIR);
      } else {
        outDir = new File(set.valueOf(optDir));
      }
      if (set.has(optPostProcessor)) {
        try {
          ClassLoader loader
                  = Thread.currentThread().getContextClassLoader();
          Class<?> postProcessorClass
                  = loader.loadClass(optPostProcessor.value(set));
          postProcessor = (PostProcessor) postProcessorClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
          ProfilerException pex =  new ProfilerException(e);
          pex.addSuppressed(e);
          throw pex;
        }
      }
    } catch (OptionException e) {
      ProfilerException profilerException = new ProfilerException(e.getMessage());
      profilerException.addSuppressed(e);
      throw profilerException;
    }
  }

  @Override
  public void beforeIteration(final BenchmarkParams benchmarkParams,
          final IterationParams iterationParams) {
           IterationType type = iterationParams.getType();
        switch (type) {
          case WARMUP:
            warmupIterationCounter.incrementAndGet();
            break;
          case MEASUREMENT:
            measurementIterationCounter.incrementAndGet();
            break;
          default:
            throw new UnsupportedOperationException("Unsupported Iteration type: " + type);
        }
      if (!measurementStarted) {
        measurementStarted = true;
        switch (type) {
          case WARMUP:
            currentRecordingFile = new File(outDir, "w_" + benchmarkParams.id()
                + '_' + warmupIterationCounter.get() + ".jfr");
            break;
          case MEASUREMENT:
            currentRecordingFile = new File(outDir, "m_" + benchmarkParams.id()
                + '_' + measurementIterationCounter.get() + ".jfr");
            break;
          default:
            throw new UnsupportedOperationException("Unsupported Iteration type: " + type);
        }
        currentRecording = JFRControler.startRecording(currentRecordingFile, configName);
      }
  }

  private boolean isEndOfWarmupOrMeasuremenets(final IterationParams iterationParams) {
          IterationType type = iterationParams.getType();
      switch (type) {
        case WARMUP:
          return warmupIterationCounter.get() == iterationParams.getCount();
        case MEASUREMENT:
          return measurementIterationCounter.get() == iterationParams.getCount();
        default:
          throw new UnsupportedOperationException("Unsupported Iteration type: " + type);
      }
  }

  @Override
  public Collection<? extends Result>
          afterIteration(final BenchmarkParams benchmarkParams, final IterationParams iterationParams,
                  final IterationResult iterationResult) {
      if (measurementStarted
              && (granularity == RecordingGranularity.PER_ITERATION
              || isEndOfWarmupOrMeasuremenets(iterationParams))) {
        JFRControler.closeRecording(currentRecording);
        currentRecording = -1L;
        measurementStarted = false;
        generated.add(currentRecordingFile);
        if (postProcessor != null) {
          generated.addAll(postProcessor.postProcess(benchmarkParams, currentRecordingFile));
        }
        return Collections.singletonList(result());
      }

    return Collections.emptyList();
  }

  private TextResult result() {
    StringWriter output = new StringWriter();
    try (PrintWriter pw = new PrintWriter(output)) {
      pw.println("JFR profiler results:");
      for (File file : generated) {
        pw.print("  ");
        pw.println(file.getPath());
      }
      pw.flush();
    }
    return new TextResult(output.toString(), "jfr");
  }

  @Override
  public Collection<String> addJVMInvokeOptions(final BenchmarkParams params) {
    return Collections.emptyList();
  }

  @Override
  public Collection<String> addJVMOptions(final BenchmarkParams params) {
    List<String> args = new ArrayList<>();
    if (debugNonSafePoints) {
      args.add("-XX:+UnlockDiagnosticVMOptions");
      args.add("-XX:+DebugNonSafepoints");
    }

    if (!flightRecorderOptions.isEmpty()) {
      args.add("-XX:FlightRecorderOptions="
              + Utils.join(flightRecorderOptions, ","));
    }
    // Unnecessary / deprecated for removal on JDKs 13.
    // TODO Could make this conditional on   majorVersion(params.getJdkVersion()) < 13
    args.add("-XX:+IgnoreUnrecognizedVMOptions");
    args.add("-XX:+FlightRecorder");

    return args;
  }

  @Override
  public void beforeTrial(final BenchmarkParams benchmarkParams) {
  }

  @Override
  public Collection<? extends Result> afterTrial(final BenchmarkResult br, final long pid,
          final File stdOut, final File stdErr) {
    return Collections.emptyList();
  }

  @Override
  public boolean allowPrintOut() {
    return true;
  }

  @Override
  public boolean allowPrintErr() {
    return true;
  }

  @Override
  public String getDescription() {
    return "JFR detailed profiler provider.";
  }

  @Override
  public String toString() {
    return "JmhDetailedJFRProfiler{" + "outDir=" + outDir + ", debugNonSafePoints="
            + debugNonSafePoints + ", configName=" + configName + ", flightRecorderOptions="
            + flightRecorderOptions + ", postProcessor=" + postProcessor + ", measurementStarted="
            + measurementStarted + ", generated=" + generated + ", currentRecording=" + currentRecording
            + ", currentRecordingFile=" + currentRecordingFile + ", granularity=" + granularity
            + ", measurementIterationCounter=" + measurementIterationCounter + ", warmupIterationCounter="
            + warmupIterationCounter + '}';
  }



  public interface PostProcessor {

    List<File> postProcess(BenchmarkParams benchmarkParams, File jfrFile);
  }

  private static class GranularityValueConverter implements ValueConverter<RecordingGranularity> {

    @Override
    public RecordingGranularity convert(final String arg) {
      return RecordingGranularity.valueOf(arg);
    }

    @Override
    public Class<RecordingGranularity> valueType() {
      return RecordingGranularity.class;
    }

    @Override
    public String valuePattern() {
      return "PER_ITERATION_TYPE|PER_ITERATION";
    }
  }
}
