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

import gnu.trove.set.hash.THashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public final class ZExecutionException extends ExecutionException {

  private static final long serialVersionUID = 8823469923479284L;

  private List<ZelFrame> zelframes;

  private final Object payload;

  public ZExecutionException(final String message, final Exception e) {
    super(message, e);
    this.payload = null;
  }

  public ZExecutionException(final Exception e) {
    super(e);
    this.payload = null;
  }

  public ZExecutionException(final String msg) {
    super(msg);
    this.payload = null;
  }

  public ZExecutionException(final Object object) {
    super();
    this.payload = object;
  }

  public Object getPayload() {
    return payload;
  }

  public void addZelFrame(final ZelFrame frame) {
    if (zelframes == null) {
      zelframes = new ArrayList<>();
    }
    zelframes.add(frame);
  }

  public List<ZelFrame> getZelframes() {
    return zelframes == null ? Collections.EMPTY_LIST : zelframes;
  }

  @Override
  public String toString() {
    String msg = super.toString();
    StringBuilder result = new StringBuilder(1024);
    result.append(msg);
    result.append('\n');
    result.append("Zel trace:\n");
    List<ZelFrame> zf = getZelframes();
    Set<String> sourceIds = new THashSet<>(zf.size());
    for (ZelFrame frame : zf) {
      result.append(frame);
      result.append('\n');
      sourceIds.add(frame.getSource());
    }
    for (String sourceId : sourceIds) {
      result.append(sourceId).append(":\n");
      result.append(ZelFrame.getDetail(sourceId)).append('\n');
    }
    return result.toString();
  }

}
