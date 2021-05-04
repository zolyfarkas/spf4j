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

import com.google.common.util.concurrent.Runnables;
import java.util.Collections;
import java.util.concurrent.Callable;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.base.Callables;
import org.spf4j.failsafe.InvalidRetryPolicyException;
import org.spf4j.failsafe.RetryDecision;
import org.spf4j.failsafe.RetryPolicies;
import org.spf4j.failsafe.RetryPredicate;
import org.spf4j.failsafe.avro.RetryPolicy;
import org.spf4j.failsafe.avro.RetryRule;
import org.spf4j.failsafe.avro.ScriptedRetryPredicateSupplier;
import org.spf4j.zel.vm.JavaMethodCall;

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
    Assert.assertEquals(3, se.eval("a + b", bindings));
  }

  @Test
  public void testEngine2() throws ScriptException, NoSuchMethodException {
    ScriptEngineManager em = new ScriptEngineManager();
    ScriptEngine se = em.getEngineByName("zel");
    CompiledScript cs = ((Compilable) se).compile("a < b");
    Assert.assertTrue((Boolean) ((Invocable) cs).invokeFunction(null, 1, 2));
  }

  @Test
  public void testEngine3() throws ScriptException, NoSuchMethodException {
    ScriptEngineManager em = new ScriptEngineManager();
    ScriptEngine se = em.getEngineByName("zel");
    Bindings bindings = se.createBindings();
    bindings.put("abort", new JavaMethodCall(RetryDecision.class, "abort"));
    bindings.put("retry", new JavaMethodCall(RetryDecision.class, "retry"));
    se.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
    CompiledScript cs = ((Compilable) se).compile(
            "startTime;endEndTime;  if (object > 3) {abort()} else {retry(10L, callable)}");
    Callable<Void> nothing = Callables.from(Runnables.doNothing());
    Assert.assertEquals(RetryDecision.abort(),
            (RetryDecision) ((Invocable) cs).invokeFunction(null, 1, 2, 10, nothing));
    Assert.assertEquals(10L,
            ((RetryDecision) ((Invocable) cs).invokeFunction(null, 1, 2, 2, nothing)).getDelayNanos());
  }

  @Test
  public void testRetryPolicyWithZEL() throws InvalidRetryPolicyException {
    org.spf4j.failsafe.RetryPolicy<Integer, Callable<Integer>> policy =
            RetryPolicies.create(new RetryPolicy(10,
           Collections.singletonList(new RetryRule("custom",
                   new ScriptedRetryPredicateSupplier("java", "zel",
                   "startTime;endEndTime; (object > 3) ? decision.abort() : decision.retry(10L, callable)", ""))),
            Collections.EMPTY_MAP));
     long nanoTime = System.nanoTime();
    RetryPredicate<Integer, Callable<Integer>> pred =  policy.getRetryPredicate(nanoTime, nanoTime + 10000000000L);
    Callable<Integer> nothing = Callables.constant(5);
    Assert.assertEquals(RetryDecision.abort(), pred.getDecision(10, nothing));
    RetryDecision<Integer, Callable<Integer>> decision = pred.getDecision(1, nothing);
    Assert.assertEquals(10L, decision.getDelayNanos());
  }

}
