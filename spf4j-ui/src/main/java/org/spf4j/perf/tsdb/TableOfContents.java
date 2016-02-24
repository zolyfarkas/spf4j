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
package org.spf4j.perf.tsdb;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @deprecated please use org.spf4j.tsdb2
 * @author zoly
 */
@Deprecated
final class TableOfContents {

    private final long location;
    private long lastTableInfo;
    private long firstTableInfo;

    TableOfContents(final long location) {
        this.location = location;
        firstTableInfo = 0;
        lastTableInfo = 0;
    }

    TableOfContents(final RandomAccessFile raf) throws IOException {
        this(raf, raf.getFilePointer());
    }

    TableOfContents(final RandomAccessFile raf, final long location) throws IOException {
        this.location = location;
        raf.seek(location);
        this.firstTableInfo = raf.readLong();
        this.lastTableInfo = raf.readLong();
    }


    public void writeTo(final RandomAccessFile raf) throws IOException {
        raf.seek(location);
        raf.writeLong(firstTableInfo);
        raf.writeLong(lastTableInfo);
    }

    public long getLastTableInfoPtr() {
        return lastTableInfo;
    }

    public long getFirstTableInfoPtr() {
        return firstTableInfo;
    }

    public void setLastTableInfo(final long plastColumnInfo, final RandomAccessFile raf) throws IOException {
        this.lastTableInfo = plastColumnInfo;
        long loc = location + 8;
        raf.seek(loc);
        raf.writeLong(lastTableInfo);
    }

    public void setFirstTableInfo(final long pfirstColumnInfo, final RandomAccessFile raf) throws IOException {
        this.firstTableInfo = pfirstColumnInfo;
        raf.seek(location);
        raf.writeLong(firstTableInfo);
    }

    public long getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "TableOfContents{" + "location=" + location + ", lastColumnInfo=" + lastTableInfo
                + ", firstColumnInfo=" + firstTableInfo + '}';
    }

}
