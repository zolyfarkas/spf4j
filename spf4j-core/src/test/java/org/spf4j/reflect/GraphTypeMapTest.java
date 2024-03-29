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
package org.spf4j.reflect;

import com.google.common.net.HostAndPort;
import com.google.common.reflect.TypeToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.tsdb2.avro.ColumnDef;

/**
 *
 * @author Zoltan Farkas
 */
public class GraphTypeMapTest {

  @Test
  public void testSomeMethod() {
    TypeMap<String> registry = new GraphTypeMap<>();
    registry.safePut(List.class, "LIST");
    registry.safePut(Object.class, "OBJECT");
    Assert.assertEquals("LIST", registry.get(ArrayList.class));
    Assert.assertEquals("OBJECT", registry.get(Map.class));
    Assert.assertEquals("OBJECT", registry.get(Object.class));
  }

  public static final class OneStrSupplier implements Supplier<String> {

    @Override
    public String get() {
      return "1";
    }

  }

  @Test
  @SuppressFBWarnings("SE_BAD_FIELD_INNER_CLASS")
  public void testBehavior() {
    TypeMap<String> registry = new GraphTypeMap<>();
    registry.safePut(Object.class, "OBJECT");
    registry.safePut(GraphTypeMapTest.class, "TEST");
    registry.safePut(CharSequence.class, "CHARSEQUENCE");
    registry.safePut(Serializable.class, "SERIALIZABLE");
    registry.safePut(Deque.class, "DEQUE");
    registry.safePut(Collection.class, "COLLECTION");

    registry.safePut((new TypeToken<Supplier<? extends CharSequence>>() {
    }).getType(), "CHAR_SUPPLIER");
    Assert.assertEquals("TEST", registry.get(GraphTypeMapTest.class));
    Assert.assertEquals("OBJECT", registry.get(Object.class));
    try {
      registry.get(String.class);
      Assert.fail();
    } catch (IllegalArgumentException ex) {
      // expected
    }
    Assert.assertThat(registry.getAll(String.class), Matchers.hasItems("SERIALIZABLE", "CHARSEQUENCE"));
    Assert.assertEquals("SERIALIZABLE", registry.get(HostAndPort.class));
    Assert.assertEquals("SERIALIZABLE", registry.get(ColumnDef.class));
    Assert.assertThat(registry.getAll(ArrayDeque.class), Matchers.hasItems("SERIALIZABLE", "DEQUE"));
    Assert.assertEquals("SERIALIZABLE", registry.get(Serializable.class));
    Assert.assertEquals("CHAR_SUPPLIER", registry.get(OneStrSupplier.class));
  }

}
