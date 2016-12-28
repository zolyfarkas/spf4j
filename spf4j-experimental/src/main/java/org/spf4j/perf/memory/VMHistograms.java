
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
