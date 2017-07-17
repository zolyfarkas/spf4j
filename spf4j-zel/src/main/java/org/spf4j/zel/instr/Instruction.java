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

import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import org.spf4j.zel.vm.ExecutionContext;
import org.spf4j.zel.vm.SuspendedException;

/**
 * <p>
 * Title: VM Instruction</p>
 * <p>
 * Description: Abstract Instruction Implementation</p>
 *
 * @author zoly
 */
public abstract class Instruction implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * Instruction execution
   *
   * @param context ExecutionContext
   * @throws java.lang.InterruptedException
   * @returns relative instruction pointer for next instruction.
   */
  public abstract int execute(ExecutionContext context)
          throws ExecutionException, InterruptedException, SuspendedException;

  public abstract Object[] getParameters();

  /**
   * Outputs Instruction Name - use for debug purposes ...
   *
   * @return String
   */
  @Override
  public final String toString() {
    String instrName = this.getClass().getSimpleName();
    Object[] parameters = getParameters();
    if (parameters.length > 0) {
      StringBuilder result = new StringBuilder(instrName.length() + parameters.length * 8);
      result.append(instrName).append('(');
      result.append(parameters[0]);
      for (int i = 1; i < parameters.length; i++) {
        result.append(',');
        result.append(parameters[i]);
      }
      result.append(')');
      return result.toString();
    } else {
      return instrName;
    }
  }

}
