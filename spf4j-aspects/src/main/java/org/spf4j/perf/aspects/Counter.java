
/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
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
 */
package org.spf4j.perf.aspects;

import org.spf4j.base.MutableInteger;

/**
 * This class should not be weaved.
 *
 * @author zoly
 */
public final class Counter {

  static final ThreadLocal<MutableInteger> SAMPLING_COUNTER = new ThreadLocal<MutableInteger>() {

    @Override
    protected MutableInteger initialValue() {
      return new MutableInteger(0);
    }

  };

  private Counter() {
  }

}
