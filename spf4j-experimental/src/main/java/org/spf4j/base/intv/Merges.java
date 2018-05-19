/*
 * Copyright 2018 SPF4J.
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
package org.spf4j.base.intv;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 *
 * @author Zoltan Farkas
 */
public class Merges {

  public static <T extends Comparable<T>> void merge(Iterable<T> a, Iterable<T> b, Consumer<T> res) {
    Iterator<T> ai = a.iterator();
    Iterator<T> bi = b.iterator();
    T an = null;
    T bn = null;
    do {
      if (an == null) {
        if (ai.hasNext()) {
          an = ai.next();
        } else {
          if (bn != null) {
            res.accept(bn);
          }
          while (bi.hasNext()) {
            res.accept(bi.next());
          }
          break;
        }
      }
      if (bn == null) {
        if (bi.hasNext()) {
          bn = bi.next();
        } else {
          if (an != null) {
            res.accept(an);
          }
          while (ai.hasNext()) {
            res.accept(ai.next());
          }
          break;
        }
      }
      int cmp = an.compareTo(bn);
      if (cmp > 0) {
        res.accept(bn);
        bn = null;
      } else if (cmp < 0) {
        res.accept(an);
        an = null;
      } else {
        res.accept(an);
        res.accept(bn);
        an = null;
        bn = null;
      }
    } while (true);

  }

}
