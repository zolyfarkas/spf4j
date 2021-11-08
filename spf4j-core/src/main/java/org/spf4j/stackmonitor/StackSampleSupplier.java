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
package org.spf4j.stackmonitor;

import java.io.IOException;
import java.time.Instant;
import javax.annotation.Nullable;

/**
 *
 * @author Zoltan Farkas
 */
public interface StackSampleSupplier {

  Instant getMin() throws IOException;

  Instant getMax() throws IOException;

  ProfileMetaData getMetaData(Instant from, Instant to) throws IOException;

  SampleNode getSamples(@Nullable String context, @Nullable String tag,
          Instant from, Instant to) throws IOException;

}
