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

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.test.log.Attachments;
import org.spf4j.log.Level;
import org.spf4j.test.log.TestLogRecordImpl;

/**
 *
 * @author Zoltan Farkas
 */
public class LogMatchersTest {


  @Test(expected = AssertionError.class)
  public void testMatching() {
    TestLogRecordImpl rec = new TestLogRecordImpl("test", Level.ERROR, "la la {} bla", "a", "b");
    rec.attach(Attachments.PRINTED);
    rec.attach(Attachments.ASSERTED);
    Assert.assertThat(rec, Matchers.allOf(LogMatchers.noAttachment(Attachments.ASSERTED),
                  LogMatchers.hasLevel(Level.ERROR)));

    Assert.assertThat(rec, Matchers.allOf(LogMatchers.hasMessageWithPattern("la la a .*"),
                  LogMatchers.hasLevel(Level.ERROR)));
    Assert.assertThat(rec, Matchers.allOf(LogMatchers.hasArgumentAt(0, "a"),
                  LogMatchers.hasLevel(Level.ERROR)));
    Assert.assertThat(rec, Matchers.allOf(LogMatchers.hasExtraArgumentAt(0, "b"),
                  LogMatchers.hasLevel(Level.ERROR)));

  }

  @Test
  public void testArgumentMatch() {
    TestLogRecordImpl rec = new TestLogRecordImpl("test", Level.ERROR, "la la {} bla", "a", "b", 5L);
    Assert.assertThat(rec, Matchers.allOf(LogMatchers.hasMatchingArguments(
            Matchers.arrayContaining((Matcher) Matchers.equalTo("a"),
                    (Matcher) Matchers.equalTo("b"),
                    (Matcher) Matchers.allOf(Matchers.greaterThan(1L), Matchers.lessThan(7L)))
    )));


  }

}
