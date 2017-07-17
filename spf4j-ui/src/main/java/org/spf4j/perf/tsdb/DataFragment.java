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
package org.spf4j.perf.tsdb;

import gnu.trove.list.array.TIntArrayList;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import org.spf4j.io.ByteArrayBuilder;

/**
 * @deprecated please use org.spf4j.tsdb2
 * @author zoly
 */
@Deprecated
final class DataFragment {

    private long location;
    private long nextDataFragment;
    private final long startTimeMillis;
    private List<long[]> data;
    private TIntArrayList timestamps;

    DataFragment(final long startTimeMillis) {
        this.location = 0;
        this.nextDataFragment = 0;
        this.startTimeMillis = startTimeMillis;
        data = new ArrayList<>();
        timestamps = new TIntArrayList();
    }

    DataFragment(final RandomAccessFile raf) throws IOException {
        location = raf.getFilePointer();
        this.nextDataFragment = raf.readLong();
        this.startTimeMillis = raf.readLong();
        int nrSamples = raf.readInt();
        int samplesLength = raf.readInt();
        int bufferSize = nrSamples * (samplesLength * 8 + 4);
        byte[] buffer = new byte[bufferSize];
        raf.readFully(buffer);
        loadData(nrSamples, samplesLength, new DataInputStream(new ByteArrayInputStream(buffer)));
    }

    public void writeTo(final DataOutput dos) throws IOException {
        dos.writeLong(nextDataFragment);
        dos.writeLong(startTimeMillis);
        dos.writeInt(data.size());
        dos.writeInt(data.get(0).length);
        for (int i = 0; i < timestamps.size(); i++) {
            dos.writeInt(timestamps.get(i));
            for (long value : data.get(i)) {
                dos.writeLong(value);
            }
        }
    }

    public void writeTo(final RandomAccessFile raf) throws IOException {
        try (ByteArrayBuilder bos = new ByteArrayBuilder()) {
            DataOutput dos = new DataOutputStream(bos);
            writeTo(dos);
            raf.seek(location);
            raf.write(bos.getBuffer(), 0, bos.size());
        }
    }

    public void addData(final long timestamp, final long[] dataRow) {
        data.add(dataRow);
        timestamps.add((int) (timestamp - startTimeMillis));
    }

    public void setNextDataFragment(final long pnextDataFragment, final RandomAccessFile raf) throws IOException {
        this.nextDataFragment = pnextDataFragment;
        setNextDataFragment(location, nextDataFragment, raf);
    }

    public static void setNextDataFragment(final long dataFragmentPosition, final long nextDataFragment,
            final RandomAccessFile raf) throws IOException {
        raf.seek(dataFragmentPosition);
        raf.writeLong(nextDataFragment);
    }

    public long getNextDataFragment() {
        return nextDataFragment;
    }

    public long getLocation() {
        return location;
    }

    private void loadData(final int nrSamples, final int samplesLength, final DataInput raf) throws IOException {
        data = new ArrayList(nrSamples);
        timestamps = new TIntArrayList(nrSamples);
        for (int i = 0; i < nrSamples; i++) {
            timestamps.add(raf.readInt());
            long[] row = new long[samplesLength];
            for (int j = 0; j < samplesLength; j++) {
                row[j] = raf.readLong();
            }
            data.add(row);
        }
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public int getNrRows() {
        return data.size();
    }

    public List<long[]> getData() {
        return data;
    }

    public TIntArrayList getTimestamps() {
        return timestamps;
    }

    public void setLocation(final long location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "DataFragment{" + "location=" + location + ", nextDataFragment=" + nextDataFragment
                + ", startTimeMillis=" + startTimeMillis + ", data=" + data + ", timestamps=" + timestamps + '}';
    }

}
