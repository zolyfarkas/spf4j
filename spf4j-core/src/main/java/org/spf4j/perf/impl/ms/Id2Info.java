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
package org.spf4j.perf.impl.ms;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import javax.annotation.Nonnull;
import org.spf4j.perf.MeasurementsInfo;

/**
 * @author zoly
 */
public final class Id2Info {

    private static final TObjectLongMap<MeasurementsInfo> INFO2ID = new TObjectLongHashMap<>();
    private static final TLongObjectMap<MeasurementsInfo> ID2INFO = new TLongObjectHashMap<>();
    private static long idSeq = 1;

    private Id2Info() { }

    public static synchronized long getId(@Nonnull final MeasurementsInfo info) {
        long id = INFO2ID.get(info);
        if (id <= 0) {
            id = idSeq++;
            INFO2ID.put(info, id);
            ID2INFO.put(id, info);
        }
        return id;
    }

    @Nonnull
    public static synchronized MeasurementsInfo getInfo(final long id) {
        MeasurementsInfo result = ID2INFO.get(id);
        if (result == null) {
          throw new IllegalArgumentException("ID " + id + " not known");
        }
        return result;
    }

}
