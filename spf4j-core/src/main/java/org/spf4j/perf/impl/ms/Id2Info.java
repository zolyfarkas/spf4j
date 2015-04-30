
package org.spf4j.perf.impl.ms;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.spf4j.perf.EntityMeasurementsInfo;

/**
 *
 * @author zoly
 */
public final class Id2Info {

    private Id2Info() { }

    private static final TObjectLongMap<EntityMeasurementsInfo> INFO2ID = new TObjectLongHashMap<>();
    private static final TLongObjectMap<EntityMeasurementsInfo> ID2INFO = new TLongObjectHashMap<>();
    private static long idSeq = 1;

    public static synchronized long getId(final EntityMeasurementsInfo info) {
        long id = INFO2ID.get(info);
        if (id <= 0) {
            id = idSeq++;
            INFO2ID.put(info, id);
            ID2INFO.put(id, info);
        }
        return id;
    }

    public static synchronized EntityMeasurementsInfo getInfo(final long id) {
        return ID2INFO.get(id);
    }

}
