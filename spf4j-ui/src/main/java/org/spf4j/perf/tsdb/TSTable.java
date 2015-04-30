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
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.spf4j.io.ByteArrayBuilder;

/**
 *
 * @author zoly
 */
public final class TSTable {

    private final long location;
    private long nextTableInfo;
    private long firstDataFragment;
    private long lastDataFragment;
    private final String tableName;
    private final int sampleTime;
    private final byte[] tableMetaData;
    private final String[] columnNames;
    private final byte[][] columnMetaData;
    private final Map<String, Integer> nameToIndex;

    TSTable(final String tableName, final byte[] tableMetaData,
            final String[] columnNames, final byte[][] columnMetaData,
            final int sampleTime, final long location) {
        this.location = location;
        this.nextTableInfo = 0;
        this.firstDataFragment = 0;
        this.lastDataFragment = 0;
        this.tableName = tableName;
        this.sampleTime = sampleTime;
        this.tableMetaData = tableMetaData;
        this.columnNames = columnNames;
        this.columnMetaData = columnMetaData;
        this.nameToIndex = new HashMap<>(columnNames.length + columnNames.length / 3);
        for (int i = 0; i < columnNames.length; i++) {
            this.nameToIndex.put(columnNames[i], i);
        }
    }

    /**
     * This is a shalow copy.
     *
     * @param copyOf
     */

    TSTable(final TSTable copyOf) {
        this.location = copyOf.location;
        this.nextTableInfo = copyOf.nextTableInfo;
        this.firstDataFragment = copyOf.firstDataFragment;
        this.lastDataFragment = copyOf.lastDataFragment;
        this.tableName = copyOf.tableName;
        this.sampleTime = copyOf.sampleTime;
        this.tableMetaData = copyOf.tableMetaData;
        this.columnNames = copyOf.columnNames;
        this.columnMetaData = copyOf.columnMetaData;
        this.nameToIndex = copyOf.nameToIndex;
    }


    /**
     * Read table structure from file.
     *
     * @param raf
     * @throws IOException
     */
    TSTable(final RandomAccessFile raf) throws IOException {
        this(raf, raf.getFilePointer());
    }

    TSTable(final RandomAccessFile raf, final long location) throws IOException {
        this.location = location;
        raf.seek(location);
        this.nextTableInfo = raf.readLong();
        this.firstDataFragment = raf.readLong();
        this.lastDataFragment = raf.readLong();
        this.tableName = raf.readUTF();
        this.sampleTime = raf.readInt();
        int grMetaSize = raf.readInt();
        this.tableMetaData = new byte[grMetaSize];
        raf.readFully(tableMetaData);
        int nrColumns = raf.readShort();
        columnNames = new String[nrColumns];
        this.nameToIndex = new HashMap<>(nrColumns + nrColumns / 3);
        for (int i = 0; i < columnNames.length; i++) {
            String colName = raf.readUTF();
            columnNames[i] = colName;
            this.nameToIndex.put(colName, i);
        }
        columnMetaData = new byte[raf.readInt()][];
        for (int i = 0; i < columnMetaData.length; i++) {
            int metaLength = raf.readInt();
            byte[] colMetaData = new byte[metaLength];
            raf.readFully(colMetaData);
            columnMetaData[i] = colMetaData;
        }
    }



    void writeTo(final DataOutput dos) throws IOException {
        dos.writeLong(nextTableInfo);
        dos.writeLong(firstDataFragment);
        dos.writeLong(lastDataFragment);
        dos.writeUTF(tableName);
        dos.writeInt(sampleTime);
        dos.writeInt(tableMetaData.length);
        dos.write(tableMetaData);
        dos.writeShort(columnNames.length);
        for (String columnName : columnNames) {
            dos.writeUTF(columnName);
        }
        dos.writeInt(columnMetaData.length);
        for (byte[] colMeta : columnMetaData) {
            dos.writeInt(colMeta.length);
            dos.write(colMeta);
        }

    }

    void writeTo(final RandomAccessFile raf) throws IOException {
        try (ByteArrayBuilder bos = new ByteArrayBuilder()) {
            DataOutput dos = new DataOutputStream(bos);
            writeTo(dos);

            raf.seek(location);
            raf.write(bos.getBuffer(), 0, bos.size());
        }
    }

    public String[] getColumnNames() {
        return columnNames.clone();
    }

    void setNextColumnInfo(final long pnextColumnInfo, final RandomAccessFile raf) throws IOException {
        this.nextTableInfo = pnextColumnInfo;

        raf.seek(location);
        raf.writeLong(nextTableInfo);
    }

    void setFirstDataFragment(final long pfirstDataFragment, final RandomAccessFile raf) throws IOException {
        this.firstDataFragment = pfirstDataFragment;
        raf.seek(location + 8);
        raf.writeLong(firstDataFragment);
    }

    void setLastDataFragment(final long plastDataFragment, final RandomAccessFile raf) throws IOException {
        this.lastDataFragment = plastDataFragment;
        raf.seek(location + 16);
        raf.writeLong(lastDataFragment);
    }

    public long getNextTSTable() {
        return nextTableInfo;
    }

    public long getLocation() {
        return location;
    }

    public String getTableName() {
        return tableName;
    }

    public long getFirstDataFragment() {
        return firstDataFragment;
    }

    public long getLastDataFragment() {
        return lastDataFragment;
    }

    public byte[][] getColumnMetaData() {
        return columnMetaData.clone();
    }

    public String[] getColumnMetaDataAsStrings() {
        String[] result = new String[columnMetaData.length];
        for (int i = 0; i < columnMetaData.length; i++) {
            result[i] = new String(columnMetaData[i], Charsets.UTF_8);
        }
        return result;
    }

    public int getColumnIndex(final String columnName) {
        Integer result = this.nameToIndex.get(columnName);
        if (result == null) {
            return -1;
        } else {
            return result;
        }
    }

    public int getSampleTime() {
        return sampleTime;
    }

    public byte[] getTableMetaData() {
        return tableMetaData.clone();
    }

    public int getColumnNumber() {
        return columnNames.length;
    }

    public String getColumnName(final int index) {
        return columnNames[index];
    }

    @Override
    public String toString() {
        return "TSTable{" + "location=" + location + ", nextColumnInfo=" + nextTableInfo
                + ", firstDataFragment=" + firstDataFragment + ", lastDataFragment="
                + lastDataFragment + ", groupName=" + tableName + ", sampleTime=" + sampleTime
                + ", columnNames=" + Arrays.toString(columnNames) + ", nameToIndex=" + nameToIndex + '}';
    }

}
