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

import com.google.common.base.Charsets;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * TSDB header detail
 *
 * @author zoly
 */
final class Header {

    public static final String TYPE = "TSDB";

    private final String type;
    private final int version;
    private final byte[] metaData;

    public Header(final int version, final byte[] metaData) {
        this.type = TYPE;
        this.version = version;
        this.metaData = metaData;
    }

    public Header(final RandomAccessFile raf) throws IOException {
        FileChannel ch = raf.getChannel();
        FileLock lock = ch.lock(ch.position(), 16, true);
        try {
            byte[] bType = new byte[4];
            raf.readFully(bType);
            this.type = new String(bType, Charsets.US_ASCII);
            if (!TYPE.equals(this.type)) {
                throw new IOException("Invalid File Type " + this.type);
            }
            this.version = raf.readInt();
            int metaDataSize = raf.readInt();
            if (metaDataSize > 0) {
                FileLock metaLock = ch.lock(ch.position(), metaDataSize, true);
                try {
                    this.metaData = new byte[metaDataSize];
                    raf.readFully(this.metaData);
                } catch (IOException | RuntimeException e) {
                    try {
                        metaLock.release();
                        throw e;
                    } catch (IOException ex) {
                        ex.addSuppressed(e);
                        throw ex;
                    }
                }
                metaLock.release();
            } else {
                this.metaData = new byte[]{};
            }
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
        FileLock lock = ch.lock();
        try {
            raf.write(type.getBytes(Charsets.US_ASCII));
            raf.writeInt(version);
            raf.writeInt(metaData.length);
            if (metaData.length > 0) {
                raf.write(metaData);
            }
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

    public String getType() {
        return type;
    }

    public int getVersion() {
        return version;
    }

    public byte[] getMetaData() {
        return metaData;
    }

    @Override
    public String toString() {
        return "Header{" + "type=" + type + ", version=" + version + '}';
    }

}
