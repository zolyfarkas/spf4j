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

import com.google.common.primitives.Ints;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class Version implements Comparable<Version>, Serializable {

  private static final long serialVersionUID = 1L;

  private transient Comparable[] components;

  private final String image;

  public Version(final String version) {
    this((CharSequence) version);
  }

  public Version(final CharSequence version) {
    this.image = version.toString();
    parse(version);
  }

  private void parse(final CharSequence version) {
    List<Comparable<?>> comps = new ArrayList<>(4);
    StringBuilder sb = new StringBuilder();
    for (int i = 0, l = version.length(); i < l; i++) {
      char c = version.charAt(i);
      final int length = sb.length();
      if (c == '.') {
        addPart(sb, comps);
        sb.setLength(0);
      } else if (Character.isDigit(c)) {
        if (length > 0) {
          char prev = sb.charAt(length - 1);
          if (!Character.isDigit(prev)) {
            comps.add(sb.toString());
            sb.setLength(0);
          }
        }
        sb.append(c);
      } else {
        if (length > 0) {
          char prev = sb.charAt(length - 1);
          if (Character.isDigit(prev)) {
            comps.add(Integer.valueOf(sb.toString()));
            sb.setLength(0);
          }
        }
        sb.append(c);
      }
    }
    if (sb.length() > 0) {
      addPart(sb, comps);
    }
    components = comps.toArray(new Comparable[comps.size()]);
  }

  private static void addPart(final StringBuilder sb, final List<Comparable<?>> comps) {
    final String strPart = sb.toString();
    Integer nr = Ints.tryParse(strPart);
    if (nr == null) {
      comps.add(strPart);
    } else {
      comps.add(nr);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public int compareTo(final Version o) {
    if (components.length == o.components.length) {
      return Comparables.compareArrays(components, o.components);
    } else if (components.length < o.components.length) {
      int res = Comparables.compareArrays(components, o.components, 0, components.length);
      if (res == 0) {
        Comparable component = o.components[components.length];
        if (component instanceof String && ((String) component).contains("SNAPSHOT")) {
          return 1;
        } else {
          return -1;
        }
      }
      return res;
    } else {
      int res = Comparables.compareArrays(components, o.components, 0, o.components.length);
      if (res == 0) {
        Comparable component = components[o.components.length];
        if (component instanceof String && ((String) component).contains("SNAPSHOT")) {
          return -1;
        } else {
          return 1;
        }
      }
      return res;
    }
  }

  @Override
  public int hashCode() {
    return image.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    return this.compareTo((Version) obj) == 0;
  }

  public String getImage() {
    return image;
  }

  public Comparable[] getComponents() {
    return components.clone();
  }

  public Comparable getComponent(final int pos) {
    return components[pos];
  }

  @SuppressFBWarnings("CLI_CONSTANT_LIST_INDEX")
  public int getMajor() {
    return (int) components[0];
  }

  @SuppressFBWarnings("CLI_CONSTANT_LIST_INDEX")
  public int getMinor() {
    return (int) components[1];
  }

  @SuppressFBWarnings("CLI_CONSTANT_LIST_INDEX")
  public int getPatch() {
    return (int) components[2];
  }

  public int getNrComponents() {
    return components.length;
  }

  private void readObject(final java.io.ObjectInputStream s)
          throws java.io.IOException, ClassNotFoundException {
    s.defaultReadObject();
    parse(this.image);
  }

  @Override
  public String toString() {
    return image;
  }

}
