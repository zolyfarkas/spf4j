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

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.spf4j.io.ReaderInputStream;
import org.spf4j.io.WriterOutputStream;
import org.spf4j.zel.vm.ProcessIOStreams;
import org.spf4j.zel.vm.Program;

/**
 *
 * @author Zoltan Farkas
 */
public final class ZelCompiledScript extends CompiledScript implements Invocable {

  private final Program program;

  private final ScriptEngine scriptEngine;

  public ZelCompiledScript(final Program program, final ScriptEngine scriptEngine) {
    this.program = program;
    this.scriptEngine = scriptEngine;
  }

  @Override
  public Object eval(final ScriptContext context) throws ScriptException {
    Map<String, Integer> lst = program.getLocalSymbolTable();
    Object[] params = new Object[program.getLocalMemSize()];
    for (Map.Entry<String, Integer> entry : lst.entrySet()) {
      params[entry.getValue()] = context.getAttribute(entry.getKey());
    }
    try {
      Charset defaultCharset = Charset.defaultCharset();
      return program.execute(
              new ProcessIOStreams(new ReaderInputStream(context.getReader(), defaultCharset, 1024),
              new WriterOutputStream(context.getWriter(), defaultCharset),
              new WriterOutputStream(context.getErrorWriter(), defaultCharset)),
              params);
    } catch (ExecutionException ex) {
      throw new ScriptException(ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new ScriptException(ex);
    }
  }

  @Override
  public ScriptEngine getEngine() {
    return scriptEngine;
  }

  @Override
  public String toString() {
    return "ZelCompiledScript{" + "program=" + program + '}';
  }

  @Override
  public Object invokeMethod(final Object thiz, final String name, final Object... args)
          throws ScriptException, NoSuchMethodException {
    if (name == null || name.isEmpty()) {
      try {
        return program.execute(org.spf4j.base.Arrays.preppend(args, thiz));
      } catch (ExecutionException ex) {
        throw new ScriptException(ex);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new ScriptException(ex);
      }
    } else {
      Integer addr = program.getGlobalSymbolTable().get(name);
      if (addr != null) {
        Object obj = program.getGlobalMem()[addr];
        if (obj instanceof Program) {
          try {
            return ((Program) obj).execute(org.spf4j.base.Arrays.preppend(args, thiz));
          } catch (ExecutionException ex) {
            throw new ScriptException(ex);
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ScriptException(ex);
          }
        }
      }
    }
    throw new NoSuchMethodException(name);
  }

  @Override
  public Object invokeFunction(final String name, final Object... args)
          throws ScriptException, NoSuchMethodException {
   if (name == null || name.isEmpty()) {
      try {
        return program.execute(args);
      } catch (ExecutionException ex) {
        throw new ScriptException(ex);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new ScriptException(ex);
      }
    } else {
      Integer addr = program.getGlobalSymbolTable().get(name);
      if (addr != null) {
        Object obj = program.getGlobalMem()[addr];
        if (obj instanceof Program) {
          try {
            return ((Program) obj).execute(args);
          } catch (ExecutionException ex) {
            throw new ScriptException(ex);
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ScriptException(ex);
          }
        }
      }
    }
    throw new NoSuchMethodException(name);
  }

  @Override
  public <T> T getInterface(final Class<T> clasz) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T getInterface(final Object thiz, final Class<T> clasz) {
    throw new UnsupportedOperationException();
  }

}
