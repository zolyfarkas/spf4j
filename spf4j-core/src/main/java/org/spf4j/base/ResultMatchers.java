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
package org.spf4j.base;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.apache.avro.util.CopyOnWriteMap;
import org.spf4j.avro.Configs;
import org.spf4j.base.avro.ObjectPattern;
import org.spf4j.base.avro.ThrowablePattern;
import org.spf4j.base.avro.OperationsResultPatterns;
import org.spf4j.base.avro.OperationResultPatterns;

/**
 * @author Zoltan Farkas
 */
public final class ResultMatchers {

  private static final Map<String, Either<Predicate<Throwable>, Predicate<Object>>> RESULT_MATCHERS;

  static {
    try {
      RESULT_MATCHERS  = new CopyOnWriteMap<>(getRegisteredResultMatchers());
    } catch (IOException ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  public interface Supplier extends Function<String, Either<Predicate<Throwable>, Predicate<Object>>> {
    default Supplier chain(final Supplier fallback) {
      Supplier current = this;
      return t -> {
        Either<Predicate<Throwable>, Predicate<Object>> result = current.apply(t);
        if (result != null) {
          return result;
        } else {
          return fallback.apply(t);
        }
      };
    }
  }

  private ResultMatchers() { }


  private static Map<String, Either<Predicate<Throwable>, Predicate<Object>>> getRegisteredResultMatchers()
          throws IOException {
    Map<String, Either<Predicate<Throwable>, Predicate<Object>>> reasons = new HashMap<>();
    for (ThrowableMatcher reason : ServiceLoader.load(ThrowableMatcher.class)) {
      reasons.put(reason.getOperationName(), Either.left(reason));
    }
    for (Enumeration<URL> e = ClassLoader.getSystemResources("result_matchers.json"); e.hasMoreElements();) {
      URL url = e.nextElement();
      try (InputStream is = url.openStream();
              Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
        Configs.read(OperationsResultPatterns.class, reader);
        OperationsResultPatterns patterns = Configs.read(OperationsResultPatterns.class, reader);
        for (OperationResultPatterns opPat : patterns.getPatterns().values()) {
          add(reasons, opPat.getThrowablePatterns(), true);
          add2(reasons, opPat.getReturnPatterns(), true);
        }
      }
    }
    return reasons;
  }

  public static Map<String, Either<Predicate<Throwable>, Predicate<Object>>> fromConfigValue(
          final String value) {
    Map<String, Either<Predicate<Throwable>, Predicate<Object>>> reasons = new HashMap<>();
    try (Reader reader = new StringReader(value)) {
      Configs.read(OperationsResultPatterns.class, reader);
      OperationsResultPatterns patterns = Configs.read(OperationsResultPatterns.class, reader);
      for (OperationResultPatterns opPat : patterns.getPatterns().values()) {
        add(reasons, opPat.getThrowablePatterns(), true);
        add2(reasons, opPat.getReturnPatterns(), true);
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return reasons;
  }

  @Nullable
  public static Either<Predicate<Throwable>, Predicate<Object>> getThrowableResultMatcher(final String reason) {
    return RESULT_MATCHERS.get(reason);
  }

  public static Supplier toSupplier() {
    return ResultMatchers::getThrowableResultMatcher;
  }

  public static Predicate<Throwable> toPredicate(final ThrowablePattern pattern) throws ClassNotFoundException {
    Class<? extends Throwable> clasz = (Class) Class.forName(pattern.getType());
    Pattern regex = pattern.getMsgPattern();
    Map<String, List<Integer>> codes = pattern.getCodes();
    return new ThrowablePredicate(clasz, regex, codes);
  }

  public static Predicate<Object> toPredicate(final ObjectPattern pattern) throws ClassNotFoundException {
    Class<? extends Object> clasz = (Class) Class.forName(pattern.getType());
    Map<String, List<Integer>> codes = pattern.getCodes();
    return new ObjectPredicate(clasz, codes);
  }

  public static void add(final Map<String, Either<Predicate<Throwable>, Predicate<Object>>> predicates,
          final Map<String, ThrowablePattern> patterns, final boolean lenient) {
    for (Map.Entry<String, ThrowablePattern> entry : patterns.entrySet()) {
      ThrowablePattern pattern = entry.getValue();
      String name = entry.getKey();
      try {
         predicates.put(name, Either.left(toPredicate(pattern)));
      } catch (ClassNotFoundException ex) {
        if (lenient) {
          Logger logger = Logger.getLogger(ResultMatchers.class.getName());
          logger.log(Level.WARNING, "Cannot register predicate {0}, class not found: {1}",
                          new Object[] {name, pattern.getType()});
          logger.log(Level.FINE, "Exception detail", ex);
        } else {
          throw new RuntimeException(ex);
        }
      }
    }
  }


  public static void add2(final  Map<String, Either<Predicate<Throwable>, Predicate<Object>>> predicates,
          final Map<String, ObjectPattern> patterns, final boolean lenient) {
    for (Map.Entry<String, ObjectPattern> entry : patterns.entrySet()) {
      ObjectPattern pattern = entry.getValue();
      String name = entry.getKey();
      try {
         predicates.put(name, Either.right(toPredicate(pattern)));
      } catch (ClassNotFoundException ex) {
        if (lenient) {
          Logger logger = Logger.getLogger(ResultMatchers.class.getName());
          logger.log(Level.WARNING, "Cannot register predicate {0}, class not found: {1}",
                          new Object[] {name, pattern.getType()});
          logger.log(Level.FINE, "Exception detail", ex);
        } else {
          throw new RuntimeException(ex);
        }
      }
    }
  }


  @SuppressFBWarnings("DMC_DUBIOUS_MAP_COLLECTION")
  private static class ObjectPredicate implements Predicate<Object> {

    private final Class<? extends Object> clasz;
    private final Map<String, List<Integer>> codes;

    ObjectPredicate(final Class<? extends Object> clasz, final Map<String, List<Integer>> codes) {
      this.clasz = clasz;
      this.codes = codes;
    }

    @Override
    public boolean test(final Object t) {
      Class<? extends Object> tClass = t.getClass();
      if (clasz.isAssignableFrom(tClass)) {
          for (Map.Entry<String, List<Integer>> entry : codes.entrySet()) {
            String getterName = Strings.attributeToMethod("get", entry.getKey());
            Method m = Reflections.getMethod(tClass, getterName);
            if (m == null) {
              return false;
            }
            Object result;
            try {
              result = m.invoke(t);
            } catch (IllegalAccessException | InvocationTargetException ex) {
              throw new RuntimeException(ex);
            }
            boolean hasCode = false;
            for (Integer code : entry.getValue()) {
              if (code.equals(result)) {
                hasCode = true;
                break;
              }
            }
            if (!hasCode) {
              return false;
            }
          }
          return true;
      }
      return false;
    }
  }

  @SuppressFBWarnings("DMC_DUBIOUS_MAP_COLLECTION")
  private static class ThrowablePredicate implements Predicate<Throwable> {

    private final Class<? extends Throwable> clasz;
    private final Pattern messageRegex;
    private final Map<String, List<Integer>> codes;

    ThrowablePredicate(final Class<? extends Throwable> clasz,
            final Pattern messageRegex, final Map<String, List<Integer>> codes) {
      this.clasz = clasz;
      this.messageRegex = messageRegex;
      this.codes = codes;
    }

    @Override
    public boolean test(final Throwable t) {
      Class<? extends Throwable> tClass = t.getClass();
      if (clasz.isAssignableFrom(tClass)) {
        String msg = t.getMessage();
        if (msg == null) {
          msg = "";
        }
        if (messageRegex.matcher(msg).matches()) {
          for (Map.Entry<String, List<Integer>> entry : codes.entrySet()) {
            String getterName = Strings.attributeToMethod("get", entry.getKey());
            Method m = Reflections.getMethod(tClass, getterName);
            if (m == null) {
              return false;
            }
            Object result;
            try {
              result = m.invoke(t);
            } catch (IllegalAccessException | InvocationTargetException ex) {
              throw new RuntimeException(ex);
            }
            boolean hasCode = false;
            for (Integer code : entry.getValue()) {
              if (code.equals(result)) {
                hasCode = true;
                break;
              }
            }
            if (!hasCode) {
              return false;
            }
          }
          return true;
        }
      }
      return false;
    }
  }


}
