/*
 * Copyright 2021 SPF4J.
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
package org.spf4j.ui;

import java.time.Instant;
import java.util.Collections;
import org.spf4j.stackmonitor.SampleNode;

/**
 *
 * @author Zoltan Farkas
 */
final class OneStackSampleSupplier implements StackSampleSupplier {

  private final Instant from;
  private final Instant to;
  private final SampleNode samples;

  OneStackSampleSupplier(final Instant from, final Instant to, final SampleNode samples) {
    this.from = from;
    this.to = to;
    this.samples = samples;
  }

  @Override
  public Instant getMin() {
    return from;
  }

  @Override
  public Instant getMax() {
    return to;
  }

  @Override
  public ProfileMetaData getMetaData(final Instant pfrom, final Instant pto) {
    return new ProfileMetaData(Collections.singletonList("default"), Collections.singletonList("default"));
  }

  @Override
  public SampleNode getSamples(final String context, final String tag, final Instant pfrom, final Instant pto) {
    return samples;
  }

  @Override
  public String toString() {
    return "OneStackSampleSupplier{" + "from=" + from + ", to=" + to + ", samples=" + samples + '}';
  }


}
