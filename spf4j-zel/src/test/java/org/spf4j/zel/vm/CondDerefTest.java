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
package org.spf4j.zel.vm;

import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zoly
 */
public final class CondDerefTest {


  private static final Logger LOG = LoggerFactory.getLogger(CondDerefTest.class);

  @Test
  public void test() throws CompileException, ExecutionException, InterruptedException {
    Program prog = Program.compile("a?.url", "a");
    LOG.debug("Program = {}", prog);
    String result = (String) prog.execute((Object) null);
    Assert.assertNull(result);
  }

  @Test
  public void test2() throws CompileException, ExecutionException, InterruptedException {
    Program prog = Program.compile("a?[\"url\"]", "a");
    LOG.debug("Program = {}", prog);
    String result = (String) prog.execute((Object) null);
    Assert.assertNull(result);
  }

  @Test(expected = NullPointerException.class)
  public void test3() throws CompileException, ExecutionException, InterruptedException {
    Program prog = Program.compile("a[\"url\"]", "a");
    LOG.debug("Program = {}", prog);
    prog.execute((Object) null);
  }

  @Test(expected = NullPointerException.class)
  public void test4() throws CompileException, ExecutionException, InterruptedException {
    Program prog = Program.compile("a.url", "a");
    LOG.debug("Program = {}", prog);
    prog.execute((Object) null);
  }


  @Test
  public void test5() throws CompileException, ExecutionException, InterruptedException {
    Predicate<GenericRecord> p = Program.compilePredicate("a?.url==null", "a");
    Schema schema = SchemaBuilder.builder().record("test").fields()
            .optionalString("url").endRecord();
    GenericData.Record record = new GenericData.Record(schema);
    Assert.assertTrue(p.test(record));

  }
}
