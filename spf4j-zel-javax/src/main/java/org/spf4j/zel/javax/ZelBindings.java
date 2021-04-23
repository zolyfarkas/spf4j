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
package org.spf4j.zel.javax;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import javax.script.Bindings;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("SCII_SPOILED_CHILD_INTERFACE_IMPLEMENTOR")
public final class ZelBindings extends HashMap<String, Object> implements Bindings {

  private static final long serialVersionUID = 1L;

  public ZelBindings(final int i, final float f) {
    super(i, f);
  }

  public ZelBindings(final int i) {
    super(i);
  }

  public ZelBindings() {
  }

  public ZelBindings(final Map<? extends String, ? extends Object> map) {
    super(map);
  }

}
