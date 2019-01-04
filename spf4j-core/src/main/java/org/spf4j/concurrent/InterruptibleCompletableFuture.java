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
package org.spf4j.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * A completable Future that wraps a Future that will provide the results for this.
 * When the wrapped Future completes it will invoke complete to complete this task.
 * see CompletableFuture for more info.
 * @author Zoltan Farkas
 */
@SuppressWarnings("checkstyle:DesignForExtension")
public class InterruptibleCompletableFuture<A> extends CompletableFuture<A> {

    private Future<A> toCancel;

    private final Object sync = new Object();

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
      synchronized (sync) {
        Future<A> tc = toCancel;
        if (tc != null) {
          tc.cancel(mayInterruptIfRunning);
          toCancel = null;
        }
        return  super.cancel(mayInterruptIfRunning);
      }
    }

    public void setToCancel(final Future<A> toCancel) {
      synchronized (sync) {
        this.toCancel = toCancel;
      }
    }

    /**
     * Overwrite as needed.
     * @return
     */
    @Override
    public String toString() {
      synchronized (sync) {
        return "InterruptibleCompletableFuture{" + "toCancel=" + toCancel + ", super = " + super.toString() + '}';
      }
    }

  }

