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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedList;
import java.util.Queue;

/**
 * TODO: there is some dumb sync in this class that needs to be reviewed.
 *
 * @author zoly
 */
@SuppressFBWarnings("NOS_NON_OWNED_SYNCHRONIZATION")
public final class Channel {

  public static final Object EOF = new Object();

  private final Queue<Object> queue;

  private final Queue<VMFuture<Object>> readers;

  private final VMExecutor exec;

  private boolean closed;

  public Channel(final VMExecutor exec) {
    this.queue = new LinkedList<>();
    this.readers = new LinkedList<>();
    this.exec = exec;
    this.closed = false;
  }

  @SuppressFBWarnings("URV_UNRELATED_RETURN_VALUES")
  public Object read() {
    synchronized (this) {
      Object obj = queue.poll();
      if (obj == null) {
        VMASyncFuture<Object> fut = new VMASyncFuture<>();
        readers.add(fut);
        return fut;
      } else {
        if (obj == EOF) {
          queue.add(EOF);
        }
        return obj;
      }
    }
  }

  public void write(final Object obj) {
    synchronized (this) {
      if (closed) {
        throw new IllegalStateException("Channel is closed, cannot write " + obj + " into it");
      }
      VMFuture<Object> reader = readers.poll();
      if (reader != null) {
        reader.setResult(obj);
        exec.resumeSuspendables(reader);
      } else {
        queue.add(obj);
      }
    }
  }

  public void close() {
    synchronized (this) {
      VMFuture<Object> reader;
      while ((reader = readers.poll()) != null) {
        reader.setResult(EOF);
        exec.resumeSuspendables(reader);
      }
      queue.add(EOF);
      closed = true;
    }
  }

  public static final class Factory implements Method {

    public static final Factory INSTANCE = new Factory();

    private Factory() {
    }

    @Override
    public Object invoke(final ExecutionContext context, final Object[] parameters) {
      return new Channel(context.getExecService());
    }

  }

  @Override
  public String toString() {
    return "Channel{" + "queue=" + queue + ", readers=" + readers + ", exec=" + exec + ", closed=" + closed + '}';
  }

}
