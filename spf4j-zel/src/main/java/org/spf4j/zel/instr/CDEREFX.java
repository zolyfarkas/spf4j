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
package org.spf4j.zel.instr;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.apache.avro.generic.GenericRecord;
import org.spf4j.reflect.CachingTypeMapWrapper;
import org.spf4j.reflect.GraphTypeMap;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.JavaMethodCall;
import org.spf4j.zel.vm.SuspendedException;

public final class CDEREFX extends Instruction {

  private static final long serialVersionUID = 1L;

  private static final CachingTypeMapWrapper<DEREF.ReferenceHandler> TYPE_HANDLER
          = new CachingTypeMapWrapper<>(new GraphTypeMap());

  static {
    TYPE_HANDLER.safePut(Map.class,
            (Object relativeTo, Object ref, ExecutionContext context)
            -> context.push(((Map) relativeTo).get(ref)))
            .safePut(GenericRecord.class,
                    (Object relativeTo, Object ref, ExecutionContext context)
                    ->  {
                      GenericRecord gr = (GenericRecord) relativeTo;
                      String fName = ref.toString();
                      if (gr.getSchema().getField(fName) != null) {
                        context.push(gr.get(fName));
                      } else {
                        context.pushNull();
                      }
                    })
            .safePut(Object.class,
                    (Object relativeTo, Object ref, ExecutionContext context)
                    -> context.push(new JavaMethodCall(relativeTo, ref.toString())))
            .safePut(Object[].class,
                    (Object relativeTo, Object ref, ExecutionContext context)
                    -> {
              if ("length".equals(ref)) {
                context.push(((Object[]) relativeTo).length);
              } else {
                context.push(((Object[]) relativeTo)[((Number) ref).intValue()]);
              }
            })
            .safePut(int[].class,
                    (Object relativeTo, Object ref, ExecutionContext context)
                    -> {
              if ("length".equals(ref)) {
                context.push(((int[]) relativeTo).length);
              } else {
                context.push(((int[]) relativeTo)[((Number) ref).intValue()]);
              }
            })
            .safePut(byte[].class,
                    (Object relativeTo, Object ref, ExecutionContext context)
                    -> {
              if ("length".equals(ref)) {
                context.push(((byte[]) relativeTo).length);
              } else {
                context.push(((byte[]) relativeTo)[((Number) ref).intValue()]);
              }
            })
            .safePut(char[].class,
                    (Object relativeTo, Object ref, ExecutionContext context)
                    -> {
              if ("length".equals(ref)) {
                context.push(((char[]) relativeTo).length);
              } else {
                context.push(((char[]) relativeTo)[((Number) ref).intValue()]);
              }
            })
            .safePut(long[].class,
                    (Object relativeTo, Object ref, ExecutionContext context)
                    -> {
              if ("length".equals(ref)) {
                context.push(((long[]) relativeTo).length);
              } else {
                context.push(((long[]) relativeTo)[((Number) ref).intValue()]);
              }
            })
            .safePut(short[].class,
                    (Object relativeTo, Object ref, ExecutionContext context)
                    -> {
              if ("length".equals(ref)) {
                context.push(((short[]) relativeTo).length);
              } else {
                context.push(((short[]) relativeTo)[((Number) ref).intValue()]);
              }
            })
            .safePut(List.class,
                    (Object relativeTo, Object ref, ExecutionContext context)
                    -> {
              if ("length".equals(ref)) {
                context.push(((List) relativeTo).size());
              } else {
                context.push(((List) relativeTo).get(((Number) ref).intValue()));
              }
            });
  }


  private final Object ref;

  public CDEREFX(final Object ref) {
    this.ref = ref;
  }

  @Override
  public int execute(final ExecutionContext context)
          throws SuspendedException, ExecutionException {
    Object relativeTo = context.popSyncStackVal();
    if (relativeTo != null) {
      pushDeref(relativeTo, ref, context);
    } else {
      context.pushNull();
    }
    return 1;
  }

  private static void pushDeref(final Object relativeTo, final Object ref, final ExecutionContext context) {
    DEREF.ReferenceHandler rh = TYPE_HANDLER.get(relativeTo.getClass());
    rh.pushDeref(relativeTo, ref, context);
  }

  @Override
  public Object[] getParameters() {
    return org.spf4j.base.Arrays.EMPTY_OBJ_ARRAY;
  }
}
