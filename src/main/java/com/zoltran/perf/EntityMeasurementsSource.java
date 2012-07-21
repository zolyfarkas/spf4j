/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.perf;

import java.util.Map;

/**
 *
 * @author zoly
 */
public interface EntityMeasurementsSource {
 
    Map<Object, EntityMeasurements> getEntitiesMeasurements(boolean reset);

}
