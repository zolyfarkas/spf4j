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
package org.spf4j.io.avro;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class SnappyUtilsTest {


  @Test
  public void testCompressUncompress() throws IOException {
    String testStr = "jhsfgajdhgfjhsgdfjhagkdjfhgasjdhfur89ewfh4898run2dw9b2cyrufscerbhc87w5hbeghrgobrcshbf8woh4o8";
    byte[] testBytes = testStr.getBytes(StandardCharsets.UTF_8);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    SnappyFrameUtils.writeFrame(bos, testBytes, 0, testBytes.length, 0.5f);
    SnappyFrameUtils.Frame readFrame =
            SnappyFrameUtils.readFrame(new ByteArrayInputStream(bos.toByteArray()), (int size) -> new byte[size]);
    Assert.assertArrayEquals(testBytes, readFrame.getData());
  }

}
