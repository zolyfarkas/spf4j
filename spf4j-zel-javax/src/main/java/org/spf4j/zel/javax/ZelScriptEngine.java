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

import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Map;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import org.spf4j.base.Pair;
import org.spf4j.zel.vm.CompileException;
import org.spf4j.zel.vm.MemoryBuilder;
import org.spf4j.zel.vm.Program;

/**
 *
 * @author Zoltan Farkas
 */
public final class ZelScriptEngine implements ScriptEngine, Compilable {

  private ScriptContext context;


  public ZelScriptEngine(final Map<String, Object> globalBindings) {
    this.context = new ZelScriptContext();
    this.context.setBindings(new ZelBindings(globalBindings), ScriptContext.GLOBAL_SCOPE);
    this.context.setBindings(new ZelBindings(), ScriptContext.ENGINE_SCOPE);
  }



  @Override
  public Object eval(final String script, final ScriptContext evalContext) throws ScriptException {
    return compile(script).eval(evalContext);
  }

  @Override
  public Object eval(final Reader reader, final ScriptContext evalContext) throws ScriptException {
    return compile(reader).eval(evalContext);
  }

  @Override
  public Object eval(final String script) throws ScriptException {
    return compile(script).eval();
  }

  @Override
  public Object eval(final Reader reader) throws ScriptException {
    return compile(reader).eval();
  }

  @Override
  public Object eval(final String script, final Bindings n) throws ScriptException {
    return compile(script).eval(n);
  }

  @Override
  public Object eval(final Reader reader, final Bindings n) throws ScriptException {
     return compile(reader).eval(n);
  }

  @Override
  public void put(final String key, final Object value) {
    context.getBindings(ScriptContext.ENGINE_SCOPE).put(key, value);
  }

  @Override
  public Object get(final String key) {
    return context.getBindings(ScriptContext.ENGINE_SCOPE).get(key);
  }

  @Override
  public Bindings getBindings(final int scope) {
    return context.getBindings(scope);
  }

  @Override
  public void setBindings(final Bindings bindings, final int scope) {
   context.setBindings(bindings, scope);
  }

  @Override
  public Bindings createBindings() {
    return new ZelBindings();
  }

  @Override
  public ScriptContext getContext() {
    return this.context;
  }

  @Override
  public void setContext(final ScriptContext context) {
    this.context = context;
  }

  @Override
  public ScriptEngineFactory getFactory() {
    return new ZelScriptEngineFactory();
  }

  @Override
  public CompiledScript compile(final String script) throws ScriptException {
    return compile(new StringReader(script));
  }

  @Override
  public CompiledScript compile(final Reader script) throws ScriptException {
    MemoryBuilder builder =  Program.getGlobalMemoryBuilder();
    for (Integer scope : this.context.getScopes()) {
      Bindings bindings = this.context.getBindings(scope);
      for (Map.Entry<String, Object> entry : bindings.entrySet()) {
        builder.addSymbol(entry.getKey(), entry.getValue());
      }
    }
    Pair<Object[], Map<String, Integer>> memSymb = builder.build();
    try {
      return new ZelCompiledScript(Program.compile(
              "reader", "anon", script, Collections.EMPTY_MAP, memSymb.getFirst(), memSymb.getSecond()), this);
    } catch (CompileException ex) {
      throw new ScriptException(ex);
    }
  }

  @Override
  public String toString() {
    return "ZelScriptEngine{" + "context=" + context + '}';
  }

  public ZelScriptEngine() {
  }

}
