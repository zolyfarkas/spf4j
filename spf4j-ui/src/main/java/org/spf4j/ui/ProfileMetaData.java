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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("EI_EXPOSE_REP") //not true
public final class ProfileMetaData {

  private final Set<String> contexts;

  private final Set<String> tags;

  ProfileMetaData(final Collection<String> contexts, final Collection<String> tags) {
    this.contexts = Collections.unmodifiableSet(new HashSet<>(contexts));
    this.tags = Collections.unmodifiableSet(new HashSet<>(tags));
  }

  public Set<String> getContexts() {
    return contexts;
  }

  public Set<String> getTags() {
    return tags;
  }

  @Override
  public String toString() {
    return "ProfileMetaData{" + "contexts=" + contexts + ", tags=" + tags + '}';
  }




}
