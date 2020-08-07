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

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import jdk.management.jfr.FlightRecorderMXBean;

/**
 * JFR utility class.
 * @author Zoltan Farkas
 */
public final class JFRControler {

  private static final FlightRecorderMXBean JFR_BEAN
          = ManagementFactory.getPlatformMXBean(FlightRecorderMXBean.class);

  private JFRControler() { }

  public static long startRecording(final File recordingFile, final String predefinedConfigName) {
    long rec = JFR_BEAN.newRecording();
    JFR_BEAN.setPredefinedConfiguration(rec, predefinedConfigName);
    JFR_BEAN.setRecordingOptions(rec, ImmutableMap.of("destination",
            recordingFile.getPath(), "disk", "true"));
    JFR_BEAN.startRecording(rec);
    return rec;
  }

  public static void closeRecording(final long recording) {
    try {
      JFR_BEAN.closeRecording(recording);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

}
