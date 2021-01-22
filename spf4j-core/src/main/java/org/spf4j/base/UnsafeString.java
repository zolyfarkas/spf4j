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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lower lever unsafe hacks for performance improvement.
 * These optimizations will work only up to including JDK 8.
 * @author Zoltan Farkas
 */
public final class UnsafeString {

    /**
   * This field is byte[] in JDK11.
   */
  private static final MethodHandle CHARS_FIELD_GET;

  //String(char[] value, boolean share) {
  private static final MethodHandle PROTECTED_STR_CONSTR_HANDLE;
  private static final Class<?>[] PROTECTED_STR_CONSTR_PARAM_TYPES;

  static {
    Field charsField;
    charsField = AccessController.doPrivileged(new PrivilegedAction<Field>() {
      @Override
      public Field run() {
        Field charsField;
        try {
          charsField = String.class.getDeclaredField("value");
          Class<?> type = charsField.getType();
          if (type.isArray() && type.getComponentType() == char.class) {
            charsField.setAccessible(true);
          } else {
            Logger logger = Logger.getLogger(Strings.class.getName());
            logger.warning("char array stealing from String not supported");
            charsField = null;
          }
        } catch (NoSuchFieldException ex) {
          Logger logger = Logger.getLogger(Strings.class.getName());
          logger.warning("char array stealing from String not supported");
          logger.log(Level.FINEST, "Exception detail", ex);
          charsField = null;
        }
        return charsField;
      }
    });
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    if (charsField != null && char[].class == charsField.getType()) {
      try {
        CHARS_FIELD_GET = lookup.unreflectGetter(charsField);
      } catch (IllegalAccessException ex) {
        throw new ExceptionInInitializerError(ex);
      }
    } else {
      CHARS_FIELD_GET = null;
    }

    // up until u45 String(int offset, int count, char value[]) {
    // u45 reverted to: String(char[] value, boolean share) {
    Constructor<String> prConstr = AccessController.doPrivileged(
            new PrivilegedAction<Constructor<String>>() {
      @Override
      public Constructor<String> run() {
        Constructor<String> constr;
        try {
          constr = String.class.getDeclaredConstructor(char[].class, boolean.class);
          constr.setAccessible(true);
        } catch (NoSuchMethodException ex) {
          try {
            constr = String.class.getDeclaredConstructor(int.class, int.class, char[].class);
            constr.setAccessible(true);
          } catch (NoSuchMethodException ex2) {
            ex2.addSuppressed(ex);
            Logger logger = Logger.getLogger(Strings.class.getName());
            logger.log(Level.FINE, "Building String from char[] without copy not supported");
            logger.log(Level.FINEST, "Exception detail", ex2);
            constr = null;
          }
        }
        return constr;
      }
    });

    if (prConstr == null) {
      PROTECTED_STR_CONSTR_PARAM_TYPES = null;
      PROTECTED_STR_CONSTR_HANDLE = null;
    } else {
      PROTECTED_STR_CONSTR_PARAM_TYPES = prConstr.getParameterTypes();
      try {
        PROTECTED_STR_CONSTR_HANDLE = lookup.unreflectConstructor(prConstr);
      } catch (IllegalAccessException ex) {
        throw new ExceptionInInitializerError(ex);
      }
    }
  }



  private UnsafeString() { }

  /**
   * Steal the underlying character array of a String.
   *
   * @param str
   * @return
   */
  public static char[] steal(final String str) {
    if (CHARS_FIELD_GET == null) {
      return str.toCharArray();
    } else {
      try {
        return (char[]) CHARS_FIELD_GET.invokeExact(str);
      } catch (Throwable ex) {
        UnknownError err = new UnknownError("Error while stealing String char array");
        err.addSuppressed(ex);
        throw err;
      }
    }
  }

   /**
   * Create a String based on the provided character array. No copy of the array is made.
   *
   * @param chars
   * @return
   */
  public static String wrap(final char[] chars) {
    if (PROTECTED_STR_CONSTR_PARAM_TYPES != null) {
      try {
        if (PROTECTED_STR_CONSTR_PARAM_TYPES.length == 3) {
          return (String) PROTECTED_STR_CONSTR_HANDLE.invokeExact(0, chars.length, chars);
        } else {
          return (String) PROTECTED_STR_CONSTR_HANDLE.invokeExact(chars, true);
        }
      } catch (Throwable ex) {
        UnknownError err = new UnknownError("Error while wrapping String char array");
        err.addSuppressed(ex);
        throw err;
      }
    } else {
      return new String(chars);
    }
  }

}
