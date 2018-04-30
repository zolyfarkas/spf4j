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
package org.spf4j.base.chars;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Zoltan Farkas
 */
public class InterviewTest {

  private static final Logger LOG = LoggerFactory.getLogger(InterviewTest.class);

  @Test
  public void testCombinations() {
    Interview.combinations("10", (x) -> {
      LOG.debug("{}", x);
    });
    Interview.combinations("101010?345?234", (x) -> {
      LOG.debug("{}", x);
    });
    Interview.combinations("???", (x) -> {
      LOG.debug("{}", x);
    });
  }

}
