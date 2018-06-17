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
package org.spf4j.test.matchers;

import java.util.regex.Pattern;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * @author Zoltan Farkas
 */
public final class PatternMatcher extends TypeSafeMatcher<String> {

  private final Pattern pattern;

  private PatternMatcher(final Pattern pattern) {
    this.pattern = pattern;
  }

  @Override
  protected boolean matchesSafely(final String item) {
    return pattern.matcher(item).matches();
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText("a string matching the pattern '" + pattern + "'");
  }

  /**
   * Creates a matcher of {@link String} that matches when the examined string exactly matches the given
   * {@link Pattern}.
   */
  @Factory
  public static Matcher<String> matchesPattern(final Pattern pattern) {
    return new PatternMatcher(pattern);
  }

  /**
   * Creates a matcher of {@link String} that matches when the examined string exactly matches the given regular
   * expression, treated as a {@link Pattern}.
   */
  @Factory
  public static Matcher<String> matchesPattern(final String regex) {
    return new PatternMatcher(Pattern.compile(regex));
  }
}
