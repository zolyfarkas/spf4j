/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.perf.impl;

import com.zoltran.perf.EntityMeasurementsInfo;
import java.util.Set;

/**
 *
 * @author zoly
 */
public class EntityMeasurementsInfoImpl implements EntityMeasurementsInfo {
    private final Object measuredEntity;
    private final String unitOfMeasurement;
    private final Set<String> measurementNames;

    public EntityMeasurementsInfoImpl(Object measuredEntity, String unitOfMeasurement, Set<String> measurementNames) {
        this.measuredEntity = measuredEntity;
        this.unitOfMeasurement = unitOfMeasurement;
        this.measurementNames = measurementNames;
    }

    @Override
    public Object getMeasuredEntity() {
        return measuredEntity;
    }

    @Override
    public String getUnitOfMeasurement() {
        return unitOfMeasurement;
    }

    @Override
    public Set<String> getMeasurementNames() {
        return measurementNames;
    }

    @Override
    public int hashCode() {
        return measuredEntity.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EntityMeasurementsInfoImpl other = (EntityMeasurementsInfoImpl) obj;
        if (this.measuredEntity != other.measuredEntity && (this.measuredEntity == null || !this.measuredEntity.equals(other.measuredEntity))) {
            return false;
        }
        if ((this.unitOfMeasurement == null) ? (other.unitOfMeasurement != null) : !this.unitOfMeasurement.equals(other.unitOfMeasurement)) {
            return false;
        }
        if (this.measurementNames != other.measurementNames && (this.measurementNames == null || !this.measurementNames.equals(other.measurementNames))) {
            return false;
        }
        return true;
    }
    
    
    
    
}
