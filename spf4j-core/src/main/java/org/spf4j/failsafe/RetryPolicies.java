/*
 * Copyright (c) 2001-2017, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Additionally licensed with:
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
package org.spf4j.failsafe;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.spf4j.base.Either;
import org.spf4j.base.ResultMatchers;
import org.spf4j.failsafe.avro.RetryParams;
import org.spf4j.failsafe.avro.RetryRule;
import org.spf4j.failsafe.avro.ScriptedRetryPredicateSupplier;

/**
 * a Factory with retry named retry Rules.
 *
 * @author Zoltan Farkas
 */
public final class RetryPolicies {

  private static final ScriptEngineManager SCRIPT_ENGINE_MANAGER = new ScriptEngineManager();

  private static final Map<String,
          Either<PartialExceptionRetryPredicateSupplier, PartialResultRetryPredicateSupplier>> REGISTRY;

  static {
    REGISTRY = load();
  }

  private RetryPolicies() {
  }

  private static Map<String,
         Either<PartialExceptionRetryPredicateSupplier, PartialResultRetryPredicateSupplier>> load() {
    Map<String, Either<PartialExceptionRetryPredicateSupplier, PartialResultRetryPredicateSupplier>> result
            = new HashMap<>();
    for (PartialExceptionRetryPredicateSupplier s
            : ServiceLoader.load(PartialExceptionRetryPredicateSupplier.class)) {
      result.put(s.getName(), Either.left(s));
    }
    for (PartialResultRetryPredicateSupplier s
            : ServiceLoader.load(PartialResultRetryPredicateSupplier.class)) {
      result.put(s.getName(), Either.right(s));
    }
    return result;
  }

  public static Either<PartialExceptionRetryPredicateSupplier, PartialResultRetryPredicateSupplier>
          getRetryPredicateSupplier(final String name) {
    return REGISTRY.get(name);
  }

  @Nullable
  public static <T, C extends Callable<? extends T>>
          Either<TimedSupplier<PartialExceptionRetryPredicate<T, C>>,
         TimedSupplier<PartialResultRetryPredicate<T, C>>> getResultRetryPredicateSupplier(
                  final String resultName, final RetryParams rp) {
    Either<Predicate<Throwable>, Predicate<Object>> resPred = ResultMatchers.getThrowableResultMatcher(resultName);
    if (resPred == null) {
      return null;
    }

    if (resPred.isLeft()) {
      Predicate<Throwable> predicate = resPred.getLeft();
      return Either.left(new TimedSupplier<PartialExceptionRetryPredicate<T, C>>() {
        @Override
        public PartialExceptionRetryPredicate<T, C> get(final long startTimeNanos, final long deadlineNanos) {
          RetryDelaySupplier ds = new JitteredDelaySupplier(
                  new FibonacciRetryDelaySupplier(rp.getNrInitialImmediateRetries(),
                          rp.getStartDelayNanos(), rp.getMaxDelayNanos()), rp.getRetryDelayJitter());
          CountLimitedPartialRetryPredicate clp
                  = new CountLimitedPartialRetryPredicate<T, Throwable, C>(rp.getMaxNrRetries(),
                          new PartialExceptionRetryPredicate<T, C>() {
                    @Override
                    public RetryDecision getExceptionDecision(final Throwable value, final Callable what) {

                      if (predicate.test(value)) {
                        return RetryDecision.retry(ds.nextDelay(), what);
                      }
                      return null;
                    }

                  });
          TimeLimitedPartialRetryPredicate tlp
                  = new TimeLimitedPartialRetryPredicate<T, Throwable, C>(startTimeNanos, deadlineNanos,
                          rp.getMaxTimeToRetryNanos(), TimeUnit.NANOSECONDS, rp.getMaxTimeToRetryFactor(), clp);

          return (Throwable value, C what) -> tlp.apply(value, what);

        }
      });
    } else {
      Predicate<Object> predicate = resPred.getRight();
      return Either.right(new TimedSupplier<PartialResultRetryPredicate<T, C>>() {
        @Override
        public PartialResultRetryPredicate<T, C> get(final long startTimeNanos, final long deadlineNanos) {
          RetryDelaySupplier ds = new JitteredDelaySupplier(
                  new FibonacciRetryDelaySupplier(rp.getNrInitialImmediateRetries(),
                          rp.getStartDelayNanos(), rp.getMaxDelayNanos()), rp.getRetryDelayJitter());
          CountLimitedPartialRetryPredicate clp
                  = new CountLimitedPartialRetryPredicate<T, T, C>(rp.getMaxNrRetries(),
                          new PartialResultRetryPredicate<T, C>() {
                    @Override
                    public RetryDecision getDecision(final Object value, final Callable what) {

                      if (predicate.test(value)) {
                        return RetryDecision.retry(ds.nextDelay(), what);
                      }
                      return null;
                    }

                  });
          TimeLimitedPartialRetryPredicate tlp
                  = new TimeLimitedPartialRetryPredicate<T, T, C>(startTimeNanos, deadlineNanos,
                          rp.getMaxTimeToRetryNanos(), TimeUnit.NANOSECONDS, rp.getMaxTimeToRetryFactor(), clp);

          return (T value, C what) -> tlp.apply(value, what);

        }
      });
    }

  }

  public static <T, C extends Callable<? extends T>> RetryPolicy<T, C> create(
          final org.spf4j.failsafe.avro.RetryPolicy policy) throws InvalidRetryPolicyException {
    RetryPolicy.Builder<T, C> builder = RetryPolicy.newBuilder();
    builder.withMaxExceptionChain(policy.getMaxSupressedExceptions());
    for (RetryRule rule : policy.getRetryRules()) {
      ScriptedRetryPredicateSupplier ps = rule.getPredicateSupplier();
      if (ps == null) {
        Either<PartialExceptionRetryPredicateSupplier, PartialResultRetryPredicateSupplier> ups
                = getRetryPredicateSupplier(rule.getName());
        if (ups.isLeft()) {
          TimedSupplier<PartialExceptionRetryPredicate<T, C>> ets = ups.getLeft();
          builder.withExceptionPartialPredicateSupplier(ets);
        } else {
          TimedSupplier<PartialResultRetryPredicate<T, C>> rts = ups.getRight();
          builder.withResultPartialPredicateSupplier(rts);
        }
      } else {
        ScriptEngine engine = SCRIPT_ENGINE_MANAGER.getEngineByName(ps.getLanguage());
        String rps = ps.getReturnPredicateSupplier();
        if (!rps.isEmpty()) {
          Invocable invocable;
          try {
            invocable = toInvocable(engine, rps);
          } catch (ScriptException ex) {
            throw new InvalidRetryPolicyException("Invalid Script: " + rps, ex);
          }
          builder.withResultPartialPredicateSupplier(
                  (start, deadline) -> (object, callable)
                  -> {
            try {
              return (RetryDecision) invocable.invokeFunction(null, object, callable);
            } catch (ScriptException | NoSuchMethodException ex) {
              Logger.getLogger(RetryPolicies.class.getName()).log(Level.SEVERE,
                      "Failed predicate {0}", new Object[]{rps, ex});
              return RetryDecision.ABORT;
            }
          });
        }
        String tps = ps.getThrowablePredicateSupplier();
        if (!tps.isEmpty()) {
          Invocable invocable;
          try {
            invocable = toInvocable(engine, tps);
          } catch (ScriptException ex) {
            throw new InvalidRetryPolicyException("Invalid Script: " + tps, ex);
          }
          builder.withResultPartialPredicateSupplier(
                  (start, deadline) -> (object, callable)
                  -> {
            try {
              return (RetryDecision) invocable.invokeFunction(null, object, callable);
            } catch (ScriptException | NoSuchMethodException ex) {
              Logger.getLogger(RetryPolicies.class.getName()).log(Level.SEVERE,
                      "Failed predicate {0}", new Object[]{tps, ex});
              return RetryDecision.ABORT;
            }
          });
        }
      }
    }
    for (Map.Entry<String, RetryParams> entry : policy.getResponse2RetryParams().entrySet()) {
      String reasonName = entry.getKey();
      Either<TimedSupplier<PartialExceptionRetryPredicate<T, C>>,
              TimedSupplier<PartialResultRetryPredicate<T, C>>> result
              = getResultRetryPredicateSupplier(reasonName, entry.getValue());
      if (result == null) {
        throw new InvalidRetryPolicyException("No reason matcher defined for: " + reasonName);
      }
      if (result.isLeft()) {
        TimedSupplier<PartialExceptionRetryPredicate<T, C>> ets = result.getLeft();
        builder.withExceptionPartialPredicateSupplier(ets);
      } else {
        TimedSupplier<PartialResultRetryPredicate<T, C>> rts = result.getRight();
        builder.withResultPartialPredicateSupplier(rts);
      }
    }
    return builder.build();
  }

  /**
   * Scripted predicates are not there for application users to modify.
   * Scripted predicates should be written by Operation engineers.
   * The script engine choice needs to be carefully done as to not allow even ops to do bad stuff by mistake.
   * (calling system.exit, etc...)
   * The main reason for allowing scripted predicates is deployment speed.
   * (a config change will propagates to your fleet several order of magnitudes faster than binaries,
   * and will not require a process restart)
   * Now this speed means also any stupid stuff can propagate faster, so canarying a config change is also a must.
   * @param engine
   * @param script
   * @return
   * @throws ScriptException
   */
  @SuppressFBWarnings("SCRIPT_ENGINE_INJECTION")
  public static Invocable toInvocable(final ScriptEngine engine, final String script) throws ScriptException {
    final Invocable invocable;
    if (engine instanceof Compilable) {
      Compilable ceng = (Compilable) engine;
      final CompiledScript predicateScript = ceng.compile(script);
      if (predicateScript instanceof Invocable) {
        invocable = (Invocable) predicateScript;
      } else {
        Object result = predicateScript.eval();
        if (result instanceof Invocable) {
          invocable = (Invocable) result;
        } else {
          throw new ScriptException("Script must evaluate to a Invocable/function, not: " + result);
        }
      }
    } else {
      Object result = engine.eval(script);
      if (result instanceof Invocable) {
        invocable = (Invocable) result;
      } else {
        throw new ScriptException("Script must evaluate to a Invocable/function, not: " + result);
      }
    }
    return invocable;
  }

}
