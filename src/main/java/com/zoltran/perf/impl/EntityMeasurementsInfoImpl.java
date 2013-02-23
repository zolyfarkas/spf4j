 /*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
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
 */
package com.zoltran.perf.impl;

import com.zoltran.perf.EntityMeasurementsInfo;
import java.util.Arrays;

/**
 *
 * @author zoly
 */
public class EntityMeasurementsInfoImpl implements EntityMeasurementsInfo {
    private final Object measuredEntity;
    private final String unitOfMeasurement;
    private final String[] measurementNames;

    public EntityMeasurementsInfoImpl(Object measuredEntity, String unitOfMeasurement, String [] measurementNames) {
        this.measuredEntity = measuredEntity;
        this.unitOfMeasurement = unitOfMeasurement;
        this.measurementNames = measurementNames.clone();
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
    public String [] getMeasurementNames() {
        return measurementNames.clone();
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
        if (!Arrays.deepEquals(this.measurementNames, other.measurementNames)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "EntityMeasurementsInfoImpl{" + "measuredEntity=" + measuredEntity + ", unitOfMeasurement=" + unitOfMeasurement + ", measurementNames=" + Arrays.toString(measurementNames)+ '}';
    }




    @Override
    public int getNumberOfMeasurements() {
        return measurementNames.length;
    }
    
    
}
