/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.perf;

import java.util.Set;
import javax.annotation.Nonnull;

/**
 *
 * @author zoly
 */
public interface EntityMeasurementsInfo {
    
    @Nonnull
    Object getMeasuredEntity();
    
    @Nonnull
    String getUnitOfMeasurement();
    
    Set<String> getMeasurementNames();
    
}
