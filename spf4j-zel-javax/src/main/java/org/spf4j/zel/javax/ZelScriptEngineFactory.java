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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import javax.annotation.Nullable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import org.apache.commons.text.StringEscapeUtils;
import org.spf4j.io.Csv;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.Program;

/**
 * A javax script engine interface for the ZEL language.
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("FCCD_FIND_CLASS_CIRCULAR_DEPENDENCY")
public final class ZelScriptEngineFactory implements ScriptEngineFactory {

  private final Map<String, Object> parameters;

  public ZelScriptEngineFactory() {
    parameters = new HashMap<>();
    parameters.put(ScriptEngine.NAME, "zel");
    parameters.put(ScriptEngine.ENGINE, "zel");
    parameters.put(ScriptEngine.LANGUAGE, "zel");
    parameters.put(ScriptEngine.ENGINE_VERSION, ExecutionContext.class.getPackage().getImplementationVersion());
    parameters.put(ScriptEngine.LANGUAGE_VERSION, Program.class.getPackage().getImplementationVersion());
    parameters.put("THREADING", "STATELESS");
  }

  @Override
  public String getEngineName() {
    return "zel";
  }

  @Override
  @Nullable
  public String getEngineVersion() {
    return (String) parameters.get(ScriptEngine.ENGINE_VERSION);
  }

  @Override
  public List<String> getExtensions() {
    return Collections.emptyList();
  }

  @Override
  public List<String> getMimeTypes() {
    return Collections.singletonList("text/zel");
  }

  @Override
  public List<String> getNames() {
    return Collections.singletonList("zel");
  }

  @Override
  public String getLanguageName() {
    return "zel";
  }

  @Override
  public String getLanguageVersion() {
    return (String) parameters.get(ScriptEngine.LANGUAGE_VERSION);
  }

  @Override
  public Object getParameter(final String key) {
    return parameters.get(key);
  }

  @Override
  public String getMethodCallSyntax(final String obj, final String m, final String... args) {
    Object[] params = new Object[args.length];
    for (int i = 0; i < args.length; i++) {
      Object v = args[i];
      if (v == null) {
        params[i] = "null";
      } else {
        params[i] = v;
      }
    }
    if (obj != null) {
      return obj + "." + m + Csv.CSV.toCsvRowString(params);
    } else {
      return  m + Csv.CSV.toCsvRowString(params);
    }
  }

  @Override
  public String getOutputStatement(final String toDisplay) {
    return "out(\"" + StringEscapeUtils.escapeJava(toDisplay) + "\")";
  }

  @Override
  public String getProgram(final String... statements) {
    return String.join(";", statements);
  }

  @Override
  public ScriptEngine getScriptEngine() {
    return new ZelScriptEngine(Collections.unmodifiableMap(parameters));
  }

  @Override
  public String toString() {
    return "ZelScriptEngineFactory{" + "parameters=" + parameters + '}';
  }

}
