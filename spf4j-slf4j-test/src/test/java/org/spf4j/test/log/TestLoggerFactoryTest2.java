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
package org.spf4j.test.log;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.log.Level;
import org.spf4j.test.log.annotations.CollectLogs;
import org.spf4j.test.log.junit4.Spf4jTestLogJUnitRunner;

/**
 * @author Zoltan Farkas
 */
@RunWith(Spf4jTestLogJUnitRunner.class)
public class TestLoggerFactoryTest2 {

  private static final Logger LOG = LoggerFactory.getLogger(TestLoggerFactoryTest2.class);


  @Test
  @CollectLogs(minLevel = Level.OFF)
  public void testNoIgnore() {
    Assume.assumeFalse(TestUtils.isExecutedFromIDE());
    Assert.assertFalse(LOG.isDebugEnabled());
  }


}
