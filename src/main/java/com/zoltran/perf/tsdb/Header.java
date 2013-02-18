/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.perf.tsdb;

import com.google.common.base.Charsets;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * TSDB header detail
 * @author zoly
 */
public class Header {
    
    public static final String TYPE="TSDB"; 
    
    private final String type;
    private final int version;
    private final int sampleIntervalMillis;    
    private final byte[] metaData;

    public Header(int version, int sampleIntervalMillis, byte[] metaData) {
        this.type = TYPE;
        this.version = version;
        this.sampleIntervalMillis = sampleIntervalMillis;
        this.metaData = metaData;
    }

    public Header(RandomAccessFile raf) throws IOException {
        FileChannel ch = raf.getChannel();
        FileLock lock = ch.lock(ch.position(), 16, true);
        try {
            byte[] bType = new byte[4];
            raf.readFully(bType);
            this.type = new String(bType, Charsets.US_ASCII);
            if (!this.type.equals(TYPE)) {
                throw new IOException("Invalid File Type " + this.type);
            }
            this.version = raf.readInt();
            this.sampleIntervalMillis = raf.readInt();
            int metaDataSize = raf.readInt();
            if (metaDataSize > 0) {
                FileLock metaLock = ch.lock(ch.position(), metaDataSize, true);
                try {
                    this.metaData = new byte[metaDataSize];
                    raf.readFully(this.metaData);
                } finally {
                    metaLock.release();
                }
            } else {
                this.metaData = new byte[]{};
            }
        } finally {
            lock.release();
        }
    }

    public void writeTo(RandomAccessFile raf) throws IOException {
        FileChannel ch = raf.getChannel();
        FileLock lock = ch.lock();
        try {
            raf.write(type.getBytes(Charsets.US_ASCII));
            raf.writeInt(version);
            raf.writeInt(sampleIntervalMillis);
            raf.writeInt(metaData.length);
            if (metaData.length > 0) {
                raf.write(metaData);
            }
        } finally {
            lock.release();
        }
    }


    public String getType() {
        return type;
    }

    public int getVersion() {
        return version;
    }

    public int getSampleIntervalMillis() {
        return sampleIntervalMillis;
    }

    public byte[] getMetaData() {
        return metaData;
    }
    
    
}
