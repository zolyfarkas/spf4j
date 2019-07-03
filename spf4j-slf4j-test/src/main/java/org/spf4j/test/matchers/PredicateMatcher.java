/*
 * Copyright 2019 SPF4J.
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
package org.spf4j.test.matchers;

import java.util.function.Predicate;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 *
 * @author Zoltan Farkas
 */
public final class PredicateMatcher<T> extends TypeSafeMatcher<T> {

  private final Predicate<T> predicate;

  public PredicateMatcher(final Predicate<T> predicate) {
    this.predicate = predicate;
  }

  @Override
  protected boolean matchesSafely(final T item) {
    return predicate.test(item);
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText("a object testing positively for '" + predicate + "'");
  }

  public static <T> Matcher<T> matchesPredicate(final Predicate<T> pred) {
    return new PredicateMatcher<>(pred);
  }

}
