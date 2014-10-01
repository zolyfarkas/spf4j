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
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 *
 * @author zoly
 */
final class TableOfContents {
    private final long location;
    private long lastColumnInfo;
    private long firstColumnInfo;

    public TableOfContents(final long location) {
        this.location = location;
        firstColumnInfo = 0;
        lastColumnInfo = 0;
    }
    
    public TableOfContents(final RandomAccessFile raf) throws IOException {
        this.location = raf.getFilePointer();
        FileChannel ch = raf.getChannel();
        FileLock lock = ch.lock(this.location, 16, true);
        try {
            this.firstColumnInfo = raf.readLong();
            this.lastColumnInfo = raf.readLong();
        } catch (IOException | RuntimeException e) {
            try {
                lock.release();
                throw e;
            } catch (IOException ex) {
                ex.addSuppressed(e);
                throw ex;
            }
        }
        lock.release();
    }

    public void writeTo(final RandomAccessFile raf) throws IOException {
        FileChannel ch = raf.getChannel();
        FileLock lock = ch.lock(this.location, 16, false);
        try {
            raf.seek(location);
            raf.writeLong(firstColumnInfo);
            raf.writeLong(lastColumnInfo);
        } catch (IOException | RuntimeException e) {
            try {
                lock.release();
                throw e;
            } catch (IOException ex) {
                ex.addSuppressed(e);
                throw ex;
            }
        }
        lock.release();
    }

    public long getLastColumnInfo() {
        return lastColumnInfo;
    }


    public long getFirstColumnInfo() {
        return firstColumnInfo;
    }

    
    public void setLastColumnInfo(final long plastColumnInfo, final RandomAccessFile raf) throws IOException {
        this.lastColumnInfo = plastColumnInfo;
        FileChannel ch = raf.getChannel();
        long loc = location + 8;
        FileLock lock = ch.lock(loc, 8, false);
        try {
            raf.seek(loc);
            raf.writeLong(lastColumnInfo);
        } catch (IOException | RuntimeException e) {
            try {
                lock.release();
                throw e;
            } catch (IOException ex) {
                ex.addSuppressed(e);
                throw ex;
            }
        }
        lock.release();
    }
    
    public void setFirstColumnInfo(final long pfirstColumnInfo, final RandomAccessFile raf) throws IOException {
        this.firstColumnInfo = pfirstColumnInfo;
        FileChannel ch = raf.getChannel();
        FileLock lock = ch.lock(location, 8, false);
        try {
            raf.seek(location);
            raf.writeLong(firstColumnInfo);
        } catch (IOException | RuntimeException e) {
            try {
                lock.release();
                throw e;
            } catch (IOException ex) {
                ex.addSuppressed(e);
                throw ex;
            }
        }
        lock.release();
    }

    @Override
    public String toString() {
        return "TableOfContents{" + "location=" + location + ", lastColumnInfo=" + lastColumnInfo
                + ", firstColumnInfo=" + firstColumnInfo + '}';
    }

}
