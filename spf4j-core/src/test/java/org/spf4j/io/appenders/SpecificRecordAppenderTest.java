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
package org.spf4j.io.appenders;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.Collections;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.avro.JThrowable;

/**
 * @author Zoltan Farkas
 */
@SuppressFBWarnings("LO_INCORRECT_NUMBER_OF_ANCHOR_PARAMETERS")
public class SpecificRecordAppenderTest {

  private static final Logger LOG = LoggerFactory.getLogger(SpecificRecordAppenderTest.class);

  @Test
  @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // this is exactly what we are testing
  public void testSpecificRecordAppender() throws IOException {
    JThrowable jThrowable = new JThrowable(null,
            null, Collections.EMPTY_LIST, null, Collections.EMPTY_LIST);
    LOG.debug("Broken Object", jThrowable);
    SpecificRecordAppender ap = new SpecificRecordAppender();
    StringBuilder sb = new StringBuilder();
    ap.append(jThrowable, sb);
    String str = sb.toString();
    Assert.assertThat(str, Matchers.containsString("SerializationError"));
    Assert.assertThat(str, Matchers.containsString("java.lang.NullPointerException"));
  }

}
