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
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.script.Bindings;
import javax.script.ScriptContext;
import org.spf4j.io.EmptyReader;
import org.spf4j.io.NullWriter;

/**
 * @author Zoltan Farkas
 */
final class ZelScriptContext implements ScriptContext {

  private final SortedMap<Integer, Bindings> scope2Bindings;

  private  Reader in;

  private  Writer out;

  private  Writer err;

  ZelScriptContext() {
    scope2Bindings = new TreeMap<>();
    in = EmptyReader.INSTANCE;
    out = NullWriter.INSTANCE;
    err =  NullWriter.INSTANCE;
  }

  @Override
  public void setBindings(final Bindings bindings, final int scope) {
    scope2Bindings.put(scope, bindings);
  }

  @Override
  public Bindings getBindings(final int scope) {
    return scope2Bindings.get(scope);
  }

  @Override
  public void setAttribute(final String name, final Object value, final int scope) {
    scope2Bindings.get(scope).put(name, value);
  }

  @Override
  public Object getAttribute(final String name, final int scope) {
    return scope2Bindings.get(scope).get(name);
  }

  @Override
  public Object removeAttribute(final String name, final int scope) {
    return scope2Bindings.get(scope).remove(name);
  }

  @Override
  public Object getAttribute(final String name) {
    Iterator<Bindings> iterator = scope2Bindings.values().iterator();
    if (iterator.hasNext()) {
      return iterator.next().get(name);
    } else {
      return null;
    }
  }

  @Override
  public int getAttributesScope(final String name) {
    for (Map.Entry<Integer, Bindings> entry : scope2Bindings.entrySet()) {
      Object val = entry.getValue().get(name);
      if (val != null) {
        return entry.getKey();
      }
    }
    return -1;
  }

  @Override
  public Writer getWriter() {
   return out;
  }

  @Override
  public Writer getErrorWriter() {
    return err;
  }

  @Override
  public void setWriter(final Writer writer) {
    this.out = writer;
  }

  @Override
  public void setErrorWriter(final Writer writer) {
    this.err = writer;
  }

  @Override
  public Reader getReader() {
    return in;
  }

  @Override
  public void setReader(final Reader reader) {
    this.in = reader;
  }

  @Override
  public List<Integer> getScopes() {
    return new ArrayList<>(scope2Bindings.keySet());
  }

  @Override
  public String toString() {
    return "ZelScriptContext{" + "scope2Bindings=" + scope2Bindings + ", in="
            + in + ", out=" + out + ", err=" + err + '}';
  }



}
