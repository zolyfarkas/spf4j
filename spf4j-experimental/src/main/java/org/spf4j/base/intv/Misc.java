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

/**
 * @author Zoltan Farkas
 */
public class Misc {

  /**
   * Island rain and stuff...
   */

  public static  int waterVolume(int[] topology) {
    int result = 0;
    for (int i = 0; i < topology.length; i++) {
      result += waterLevel(topology, i);
    }
    return result;
  }


  public static int waterLevel(int[] topology, int pos) {
    int h = topology[pos];
    int lh = h;
    for (int i = 0; i < pos; i++) {
      int v = topology[i];
      if (v > lh) {
        lh = v;
      }
    }
    if (lh <= h) {
      return 0;
    }
    int rh = h;
    for (int i = pos + 1; i < topology.length; i++) {
      int v = topology[i];
      if (v > rh) {
        rh = v;
      }
    }
    if (rh <= h) {
      return 0;
    }
    return Math.min(lh, rh) - h;
  }


}
