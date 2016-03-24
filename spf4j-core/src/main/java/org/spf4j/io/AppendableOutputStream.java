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
package org.spf4j.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import org.spf4j.recyclable.impl.ArraySuppliers;

/**
 * {@link OutputStream} implementation that transforms a byte stream to a
 * character stream using a specified character set encoding and writes the resulting
 * stream to a {@link Writer}. The stream is transformed using a
 * {@link CharsetDecoder} object, guaranteeing that all character set
 * encodings supported by the JRE are handled correctly.
 * <p>
 * The output of the {@link CharsetDecoder} is buffered using a fixed size buffer.
 * This implies that the data is written to the underlying {@link Writer} in chunks
 * that are no larger than the size of this buffer. By default, the buffer is
 * flushed only when it overflows or when {@link #flush()} or {@link #close()}
 * is called. In general there is therefore no need to wrap the underlying {@link Writer}
 * in a {@link java.io.BufferedWriter}. {@link WriterOutputStream} can also
 * be instructed to flush the buffer after each write operation. In this case, all
 * available data is written immediately to the underlying {@link Writer}, implying that
 * the current position of the {@link Writer} is correlated to the current position
 * of the {@link AppendableOutputStream}.
 * <p>
 * {@link AppendableOutputStream} implements the inverse transformation of {@link java.io.OutputStreamWriter};
 * in the following example, writing to {@code out2} would have the same result as writing to
 * {@code out} directly (provided that the byte sequence is legal with respect to the
 * character set encoding):
 * <pre>
 * OutputStream out = ...
 * Charset cs = ...
 * OutputStreamWriter writer = new OutputStreamWriter(out, cs);
 * AppendableOutputStream out2 = new AppendableOutputStream(writer, cs);</pre>
 * {@link WriterOutputStream} implements the same transformation as {@link java.io.InputStreamReader},
 * except that the control flow is reversed: both classes transform a byte stream
 * into a character stream, but {@link java.io.InputStreamReader} pulls data from the underlying stream,
 * while {@link AppendableOutputStream} pushes it to the underlying stream.
 * <p>
 * Note that while there are use cases where there is no alternative to using
 * this class, very often the need to use this class is an indication of a flaw
 * in the design of the code. This class is typically used in situations where an existing
 * API only accepts an {@link OutputStream} object, but where the stream is known to represent
 * character data that must be decoded for further use.
 * <p>
 * Instances of {@link AppendableOutputStream} are not thread safe.
 * 
 * @see org.apache.commons.io.input.ReaderInputStream
 * 
 * @since 7.2.25
 */
//CHECKSTYLE IGNORE DesignForExtension FOR NEXT 2000 LINES
//CHECKSTYLE IGNORE VisibilityModifier FOR NEXT 2000 LINES
public class AppendableOutputStream extends OutputStream {
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private final Appendable writer;
    private final CharsetDecoder decoder;

    /**
     * ByteBuffer used as input for the decoder. This buffer can be small
     * as it is used only to transfer the received data to the
     * decoder.
     */
    private ByteBuffer decoderIn;

    /**
     * CharBuffer used as output for the decoder. It should be
     * somewhat larger as we write from this buffer to the
     * underlying Writer.
     */
    protected CharBuffer decoderOut;

    /**
     * Constructs a new {@link WriterOutputStream} with a default output buffer size of
     * 1024 characters. The output buffer will only be flushed when it overflows or when
     * {@link #flush()} or {@link #close()} is called.
     * 
     * @param writer the target {@link Writer}
     * @param decoder the charset decoder
     * @since 2.1
     */
    public AppendableOutputStream(final Appendable writer, final CharsetDecoder decoder) {
        this(writer, decoder, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Constructs a new {@link WriterOutputStream}.
     * 
     * @param writer the target {@link Writer}
     * @param decoder the charset decoder
     * @param bufferSize the size of the output buffer in number of characters
     * @since 2.1
     */
    public AppendableOutputStream(final Appendable writer, final CharsetDecoder decoder, final int bufferSize) {
        checkIbmJdkWithBrokenUTF16(decoder.charset());
        this.writer = writer;
        this.decoder = decoder;
        decoderIn = ByteBuffer.wrap(ArraySuppliers.Bytes.TL_SUPPLIER.get(256));
        decoderOut = CharBuffer.wrap(ArraySuppliers.Chars.TL_SUPPLIER.get(bufferSize));
    }

    /**
     * Constructs a new {@link WriterOutputStream}.
     * 
     * @param writer the target {@link Writer}
     * @param charset the charset encoding
     * @param bufferSize the size of the output buffer in number of characters
     */
    public AppendableOutputStream(final Appendable writer, final Charset charset, final int bufferSize) {
        this(writer,
             charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE)
                    .replaceWith("?"),
             bufferSize);
    }

    /**
     * Constructs a new {@link WriterOutputStream} with a default output buffer size of
     * 1024 characters. The output buffer will only be flushed when it overflows or when
     * {@link #flush()} or {@link #close()} is called.
     * 
     * @param writer the target {@link Writer}
     * @param charset the charset encoding
     */
    public AppendableOutputStream(final Appendable writer, final Charset charset) {
        this(writer, charset, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Constructs a new {@link WriterOutputStream}.
     * 
     * @param writer the target {@link Writer}
     * @param charsetName the name of the charset encoding
     * @param bufferSize the size of the output buffer in number of characters
     */
    public AppendableOutputStream(final Appendable writer, final String charsetName, final int bufferSize) {
        this(writer, Charset.forName(charsetName), bufferSize);
    }

    /**
     * Constructs a new {@link WriterOutputStream} with a default output buffer size of
     * 1024 characters. The output buffer will only be flushed when it overflows or when
     * {@link #flush()} or {@link #close()} is called.
     * 
     * @param writer the target {@link Writer}
     * @param charsetName the name of the charset encoding
     */
    public AppendableOutputStream(final Appendable writer, final String charsetName) {
        this(writer, charsetName, DEFAULT_BUFFER_SIZE);
    }


    /**
     * Write bytes from the specified byte array to the stream.
     * 
     * @param b the byte array containing the bytes to write
     * @param poff the start offset in the byte array
     * @param plen the number of bytes to write
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(final byte[] b, final int poff, final int plen) throws IOException {
        int len = plen;
        int off = poff;
        while (len > 0) {
            final int c = Math.min(len, decoderIn.remaining());
            decoderIn.put(b, off, c);
            processInput(false);
            len -= c;
            off += c;
        }
    }

    /**
     * Write bytes from the specified byte array to the stream.
     * 
     * @param b the byte array containing the bytes to write
     * @throws IOException if an I/O error occurs
     */
    @Override
    public final void write(final byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /**
     * Write a single byte to the stream.
     * 
     * @param b the byte to write
     * @throws IOException if an I/O error occurs
     */
    @Override
    public final void write(final int b) throws IOException {

        final int c = Math.min(1, decoderIn.remaining());
        if (c > 0) {
            decoderIn.put((byte) b);
            processInput(false);
        } else {
            processInput(false);
            decoderIn.put((byte) b);
            processInput(false);
        }
    }

    /**
     * Flush the stream. Any remaining content accumulated in the output buffer
     * will be written to the underlying {@link Writer}. After that
     * {@link Writer#flush()} will be called.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void flush() throws IOException {
        flushOutput();
    }

    /**
     * Close the stream. Any remaining content accumulated in the output buffer
     * will be written to the underlying {@link Writer}. After that
     * {@link Writer#close()} will be called.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (decoderIn != null) {
            processInput(true);
            flushOutput();
            ArraySuppliers.Bytes.TL_SUPPLIER.recycle(decoderIn.array());
            ArraySuppliers.Chars.TL_SUPPLIER.recycle(decoderOut.array());
            decoderIn = null;
            decoderOut = null;
        }
    }

    /**
     * Decode the contents of the input ByteBuffer into a CharBuffer.
     * 
     * @param endOfInput indicates end of input
     * @throws IOException if an I/O error occurs
     */
    protected void processInput(final boolean endOfInput) throws IOException {
        // Prepare decoderIn for reading
        decoderIn.flip();
        CoderResult coderResult;
        while (true) {
            coderResult = decoder.decode(decoderIn, decoderOut, endOfInput);
            if (coderResult.isOverflow()) {
                flushOutput();
            } else if (coderResult.isUnderflow()) {
                break;
            } else {
                // The decoder is configured to replace malformed input and unmappable characters,
                // so we should not get here.
                throw new IOException("Unexpected coder result " + coderResult);
            }
        }
        // Discard the bytes that have been read
        decoderIn.compact();
    }

    /**
     * Flush the output.
     * 
     * @throws IOException if an I/O error occurs
     */
    protected void flushOutput() throws IOException {
        final int position = decoderOut.position();
        if (position > 0) {
            decoderOut.position(0);
            int limit = decoderOut.limit();
            decoderOut.limit(position);
            writer.append(decoderOut);
            decoderOut.rewind();
            decoderOut.limit(limit);
        }
    }
    
    
    static void checkIbmJdkWithBrokenUTF16(final Charset charset) {
        if (!"UTF-16".equals(charset.name())) {
            return;
        }
        final String testString2 = "v\u00e9s";
        byte[] bytes = testString2.getBytes(charset);

        final CharsetDecoder charsetDecoder2 = charset.newDecoder();
        ByteBuffer bb2 = ByteBuffer.allocate(16);
        CharBuffer cb2 = CharBuffer.allocate(testString2.length());
        final int len = bytes.length;
        for (int i = 0; i < len; i++) {
            bb2.put(bytes[i]);
            bb2.flip();
            try {
                charsetDecoder2.decode(bb2, cb2, i == (len - 1));
            } catch (IllegalArgumentException e) {
                throw new UnsupportedOperationException(
                        "UTF-16 requested when runninng on an IBM JDK with broken UTF-16 support. "
                        + "Please find a JDK that supports UTF-16 if you intend to use UF-16 with WriterOutputStream",
                        e);
            }
            bb2.compact();
        }
        cb2.rewind();
        if (!testString2.equals(cb2.toString())) {
            throw new UnsupportedOperationException(
                    "UTF-16 requested when runninng on an IBM JDK with broken UTF-16 support. "
                    + "Please find a JDK that supports UTF-16 if you intend to use UF-16 with WriterOutputStream");
        }

    }

    @Override
    public String toString() {
        return "AppendableOutputStream{" + "writer=" + writer + '}';
    }
    
    
    
}
