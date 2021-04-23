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

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Zoltan Farkas
 */
public class ZelScriptEngineTest {

  @Test
  public void testEngine() throws ScriptException {
    ScriptEngineManager em = new ScriptEngineManager();
    ScriptEngine se = em.getEngineByName("zel");
    Bindings bindings = se.createBindings();
    bindings.put("a", 1);
    bindings.put("b", 2);
    Assert.assertEquals(3, se.eval("a +  b", bindings));
  }

}
