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
    boolean ab = true, bb = true;
    T an = null;
    T bn = null;
    while (true) {
      if (ab && ai.hasNext()) {
        an = ai.next();
        ab = false;
      }
      if (bb & bi.hasNext()) {
        bn = bi.next();
        bb = false;
      }
      if (!ab) {
        if (!bb) {
          int cmp = an.compareTo(bn);
          if (cmp > 0) {
            res.accept(bn);
            bb = true;
          } else if (cmp < 0){
            res.accept(an);
            ab = true;
          } else {
            res.accept(bn);
            res.accept(an);
            bb = true;
            ab = true;
          }
        } else {
           res.accept(an);
           ab = true;
        }
      } else {
        if (!bb) {
           res.accept(bn);
           bb = true;
        } else {
          break;
        }
      }
    }
  }

}
