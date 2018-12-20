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
package org.spf4j.log;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("LO_INCORRECT_NUMBER_OF_ANCHOR_PARAMETERS")
public class LogAttributeTest {

  private static final Logger LOG = LoggerFactory.getLogger(LogAttributeTest.class);

  @Test
  public void testLogAttribute() {
    LogAttribute<LogAttribute<String>> test = LogAttribute.of("text", LogAttribute.of("nested", "nextsed val"));
    StringBuilder json = new StringBuilder();
    test.writeJsonTo(json);
    LOG.debug("Json", json);
    LogAttribute<Object> fromJson = LogAttribute.fromJson(json);
    Assert.assertEquals(json.toString(), fromJson.toString());
  }

  @Test
  public void testLogAttribute2() {
    LogAttribute<Collection<LogAttribute<String>>> test = LogAttribute.of("text", Collections.singletonList(
            LogAttribute.of("nested", "nextsed val")));
    StringBuilder json = new StringBuilder();
    test.writeJsonTo(json);
    LOG.debug("Json", json);
    LogAttribute<Object> fromJson = LogAttribute.fromJson(json);
    Assert.assertEquals(json.toString(), fromJson.toString());
  }

}
