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
package org.spf4j.perf.memory;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import sun.jvm.hotspot.HotSpotAgent;
import sun.jvm.hotspot.oops.DefaultHeapVisitor;
import sun.jvm.hotspot.oops.Klass;
import sun.jvm.hotspot.oops.Oop;
import sun.jvm.hotspot.runtime.VM;



/**
 *
 * @author zoly
 */
public class VMHistograms {

    static {
         HotSpotAgent agent = new HotSpotAgent();
         agent.attach(org.spf4j.base.Runtime.PID);
         sun.jvm.hotspot.runtime.VM.initialize(agent.getTypeDataBase(), false);
    }

    public static TObjectIntMap<Klass> getHeapInstanceCountsHistogram() {
        final TObjectIntMap<Klass> counts = new TObjectIntHashMap(10240);
        VM vm = sun.jvm.hotspot.runtime.VM.getVM();
        vm.getObjectHeap().iterate(new DefaultHeapVisitor() {
            @Override
            public boolean doObj(final Oop oop) {
                counts.increment(oop.getKlass());
                oop.getKlass();
                return false;
            }
       });
        return counts;
    }

}
