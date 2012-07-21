/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.perf;


import java.util.Map;
import javax.annotation.Nonnull;


/**
 *
 * @author zoly
 */

public interface EntityMeasurements {
    
    @Nonnull
    Map<String, Number> getMeasurements(boolean reset);    
    
    @Nonnull
    EntityMeasurements  aggregate(@Nonnull EntityMeasurements mSource);
    
    @Nonnull
    Object getMeasuredEntity();
    
    @Nonnull
    String getUnitOfMeasurement();
    
    @Nonnull
    EntityMeasurements createClone(boolean reset);
    
    @Nonnull
    EntityMeasurements createLike(Object entity);
    
}
