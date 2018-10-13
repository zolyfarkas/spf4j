/*
 * Copyright 2018 SPF4J.
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
package org.spf4j.maven.plugin.avro.avscp.validation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.avro.Schema;
import org.spf4j.maven.plugin.avro.avscp.validation.impl.SchemaDocValidator;

/**
 * @author Zoltan Farkas
 */
public final class Validators {

  private static final Map<String, Validator<?>> VALIDATORS = new HashMap<>();

  static {
    SchemaDocValidator sdVal = new SchemaDocValidator();
    VALIDATORS.put(sdVal.getName(), sdVal);
    ServiceLoader<Validator> factories
            = ServiceLoader.load(Validator.class);
    Iterator<Validator> iterator = factories.iterator();
    while (iterator.hasNext()) {
      Validator v = iterator.next();
      Validator ex = VALIDATORS.put(v.getName(), v);
      if (ex != null) {
        Logger.getLogger(Validators.class.getName()).log(Level.WARNING,
                "Ignoring {0} because of {1}", new Object[]{ex, v});
      }
    }
  }

  private final Map<String, Validator<?>> validators;

  public Validators(final List<String> exclude) {
    validators = new HashMap<>(VALIDATORS);
    for (String vname : exclude) {
      validators.remove(vname);
    }
  }

  public Map<String,  Validator.Result> validate(final Schema s) {
    Map<String,  Validator.Result> result = new HashMap<>(4);
    for (Validator v : validators.values()) {
      Validator.Result res = v.validate(s);
      if (res.isFailed()) {
        result.put(v.getName(), res);
      }
    }
    return result;
  }

  @Override
  public String toString() {
    return "Validators{" + "validators=" + validators + '}';
  }
  
}
