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
package org.spf4j.base;

/**
 * Logging utilities for situations where a logging framework is inapropriate.
 * Use cases:
 * 1) logging issues before logging framework is initialized or after it is closed.
 * 2) Issues happening during the logging process itself.
 * 3) Fatal situations where the logging framework is too heavy.
 * @author Zoltan Farkas
 */
public final class ErrLog {

  private ErrLog() { }

  @SuppressWarnings("checkstyle:regexp")
  public static void error(final String message) {
    System.err.println(message);
  }

  @SuppressWarnings("checkstyle:regexp")
  public static void error(final String message, final Throwable t) {
    System.err.println(message);
    Throwables.writeTo(t, System.err, Throwables.PackageDetail.SHORT);
  }

  @SuppressWarnings("checkstyle:regexp")
  public static void errorNoPackageDetail(final String message, final Throwable t) {
    System.err.println(message);
    Throwables.writeTo(t, System.err, Throwables.PackageDetail.NONE);
  }

}
