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
package org.spf4j.io.avro;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.IntFunction;
import org.iq80.snappy.Snappy;
import org.spf4j.recyclable.impl.ArraySuppliers;

/**
 * Utility methods to read/write snappy frames.
 *
 * Snappy frame detail: https://github.com/google/snappy/blob/master/framing_format.txt
 *
 *
 * @author zoly
 */
public final class SnappyFrameUtils {


  public static final int COMPRESSED_DATA_FLAG = 0x00;

  public static final int UNCOMPRESSED_DATA_FLAG = 0x01;

  public static final int STREAM_IDENTIFIER_FLAG = 0xff;

  public static final int PADDING_FLAG = 0xfe;

  /**
   * The header consists of the stream identifier flag, 3 bytes indicating a length of 6, and "sNaPpY" in ASCII.
   */
  public static final byte[] HEADER_BYTES = new byte[]{(byte) STREAM_IDENTIFIER_FLAG,
    0x06, 0x00, 0x00, 0x73, 0x4e, 0x61, 0x50, 0x70, 0x59};

  private SnappyFrameUtils() {
  }

  public static void writeFrame(final OutputStream out, final byte[] pdata,
          final int poffset, final int plength, final float compressionThreshold)
          throws IOException {
    int crc32c = Crc32C.maskedCrc32c(pdata, poffset, plength);
    int maxCompressedLength = Snappy.maxCompressedLength(plength);
    byte[] data = ArraySuppliers.Bytes.TL_SUPPLIER.get(maxCompressedLength);
    try {
      int length = Snappy.compress(pdata, poffset, plength, data, 0);

      if (((float) length) / plength < compressionThreshold) {
        out.write(COMPRESSED_DATA_FLAG);
        // the length written out to the header is both the checksum and the
        // frame
        int headerLength = length + 4;

        // write length
        out.write(headerLength);
        out.write(headerLength >>> 8);
        out.write(headerLength >>> 16);

        // write crc32c of user input data
        out.write(crc32c);
        out.write(crc32c >>> 8);
        out.write(crc32c >>> 16);
        out.write(crc32c >>> 24);

        // write data
        out.write(data, 0, length);
      } else {
        out.write(UNCOMPRESSED_DATA_FLAG);
        // the length written out to the header is both the checksum and the
        // frame
        int headerLength = plength + 4;

        // write length
        out.write(headerLength);
        out.write(headerLength >>> 8);
        out.write(headerLength >>> 16);

        // write crc32c of user input data
        out.write(crc32c);
        out.write(crc32c >>> 8);
        out.write(crc32c >>> 16);
        out.write(crc32c >>> 24);

        // write data
        out.write(pdata, poffset, plength);

      }
    } finally {
      ArraySuppliers.Bytes.TL_SUPPLIER.recycle(data);
    }
  }

  public static void writeUncompressedFrame(final OutputStream out, final byte[] data,
          final int offset, final int length)
          throws IOException {
    out.write(UNCOMPRESSED_DATA_FLAG);
    int crc32c = Crc32C.maskedCrc32c(data, offset, length);
    // the length written out to the header is both the checksum and the
    // frame
    int headerLength = length + 4;

    // write length
    out.write(headerLength);
    out.write(headerLength >>> 8);
    out.write(headerLength >>> 16);

    // write crc32c of user input data
    out.write(crc32c);
    out.write(crc32c >>> 8);
    out.write(crc32c >>> 16);
    out.write(crc32c >>> 24);

    // write data
    out.write(data, offset, length);
  }


  enum FrameType {
    DATA, IDENTIFIER, SKIPPABLE
  }

  public static final class Frame {

    private final FrameType frameType;

    private final int length;

    private final byte [] data;

    Frame(final FrameType frameType, final int length, final byte[] data) {
      this.frameType = frameType;
      this.length = length;
      this.data = data;
    }

    public FrameType getFrameType() {
      return frameType;
    }

    public int getLength() {
      return length;
    }

    public byte[] getData() {
      return data;
    }

    @Override
    public String toString() {
      return "Frame{" + "frameType=" + frameType + ", length=" + length + '}';
    }

  }

  public static  Frame readFrame(final InputStream in, final IntFunction<byte []> arrayProvider)
          throws IOException {

    int flag = in.read();
    int length = in.read();
    length |= in.read() << 8;
    length |= in.read() << 16;
    switch (flag) {
      case COMPRESSED_DATA_FLAG:
        if (length < 5) {
          throw new IOException("invalid length: " + length + " for chunk flag: " + Integer.toHexString(flag));
        }
        length -= 4;
        int crc = readInt(in);
        byte[] data = ArraySuppliers.Bytes.TL_SUPPLIER.get(length);
        ByteStreams.readFully(in, data, 0, length);
        int uncompressedLength = Snappy.getUncompressedLength(data, 0);
        byte [] result = arrayProvider.apply(uncompressedLength);
        Snappy.uncompress(data, 0, length, result, 0);
        int maskedCrc32c = Crc32C.maskedCrc32c(result, 0, uncompressedLength);
        if (crc != maskedCrc32c) {
          throw new IOException("CRC failure " + crc + " != " + maskedCrc32c);
        }
        return new Frame(FrameType.DATA, uncompressedLength, result);
      case UNCOMPRESSED_DATA_FLAG:
        if (length < 5) {
          throw new IOException("invalid length: " + length + " for chunk flag: " + Integer.toHexString(flag));
        }
        length -= 4;
        crc = readInt(in);
        data = arrayProvider.apply(length);
        ByteStreams.readFully(in, data, 0, length);
        maskedCrc32c = Crc32C.maskedCrc32c(data, 0, length);
        if (crc != maskedCrc32c) {
          throw new IOException("CRC failure " + crc + " != " + maskedCrc32c);
        }
        return new Frame(FrameType.DATA, length, data);
      case STREAM_IDENTIFIER_FLAG:
        if (length != 6) {
          throw new IOException("stream identifier chunk with invalid length: " + length);
        }
        return new Frame(FrameType.IDENTIFIER, 6, null);
      default:
        // Reserved unskippable chunks (chunk types 0x02-0x7f)
        if (flag <= 0x7f) {
          throw new IOException("unsupported unskippable chunk: " + Integer.toHexString(flag));
        }
        // all that is left is Reserved skippable chunks (chunk types 0x80-0xfe)
        if (length < 0) {
          throw new IOException("invalid length: " + length + " for chunk flag: " + Integer.toHexString(flag));
        }
        long skipped = in.skip(length);
        if (skipped != length) {
          throw new IOException("Unable to skip " + length + " bytes, managed only " + skipped);
        }
        return new Frame(FrameType.SKIPPABLE, length, null);
    }
  }

  public static int readInt(final InputStream is) throws IOException {
    return is.read() | is.read() << 8 | is.read() << 16 | is.read() << 24;
  }

}
