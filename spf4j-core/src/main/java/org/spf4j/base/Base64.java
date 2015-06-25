package org.spf4j.base;

import java.io.IOException;

/**
 * improved implementation based on DataTypeConverterImpl
 * should be about 6-7% faster than the JDK equivalent.
 * But most importantly you can encode/decode parts of a String, which should reduce the need
 * of copying objects and reduce the amount of garbage created.
 *
 * @author zoly
 */
public final class Base64 {

    private Base64() {
    }

    private static final byte[] DECODE_MAP = initDecodeMap();

    private static final byte PADDING = 127;

    private static byte[] initDecodeMap() {
        byte[] map = new byte[128];
        int i;
        for (i = 0; i < 128; i++) {
            map[i] = -1;
        }

        for (i = 'A'; i <= 'Z'; i++) {
            map[i] = (byte) (i - 'A');
        }
        for (i = 'a'; i <= 'z'; i++) {
            map[i] = (byte) (i - 'a' + 26);
        }
        for (i = '0'; i <= '9'; i++) {
            map[i] = (byte) (i - '0' + 52);
        }
        map['+'] = 62;
        map['/'] = 63;
        map['='] = PADDING;

        return map;
    }

    /**
     * computes the length of binary data speculatively.
     *
     * <p>
     * Our requirement is to create byte[] of the exact length to store the binary data. If we do this in a
     * straight-forward way, it takes two passes over the data. Experiments show that this is a non-trivial overhead
     * (35% or so is spent on the first pass in calculating the length.)
     *
     * <p>
     * So the approach here is that we compute the length speculatively, without looking at the whole contents. The
     * obtained speculative value is never less than the actual length of the binary data, but it may be bigger. So if
     * the speculation goes wrong, we'll pay the cost of reallocation and buffer copying.
     *
     * <p>
     * If the base64 text is tightly packed with no indentation nor illegal char (like what most web services produce),
     * then the speculation of this method will be correct, so we get the performance benefit.
     */
    private static int guessLength(final CharSequence text, final int from, final int len) {
        final int to = from + len;

        // compute the tail '=' chars
        int j = to - 1;
        for (; j >= 0; j--) {
            byte code = DECODE_MAP[text.charAt(j)];
            if (code == PADDING) {
                continue;
            }
            if (code == -1) // most likely this base64 text is indented. go with the upper bound
            {
                return len / 4 * 3;
            }
            break;
        }

        j++;    // text.charAt(j) is now at some base64 char, so +1 to make it the size
        int padSize = to - j;
        if (padSize > 2) // something is wrong with base64. be safe and go with the upper bound
        {
            return len / 4 * 3;
        }

        // so far this base64 looks like it's unindented tightly packed base64.
        // take a chance and create an array with the expected size
        return len / 4 * 3 - padSize;
    }


    private static int guessLength(final char [] text, final int from, final int len) {
        final int to = from + len;

        // compute the tail '=' chars
        int j = to - 1;
        for (; j >= 0; j--) {
            byte code = DECODE_MAP[text[j]];
            if (code == PADDING) {
                continue;
            }
            if (code == -1) // most likely this base64 text is indented. go with the upper bound
            {
                return len / 4 * 3;
            }
            break;
        }

        j++;    // text.charAt(j) is now at some base64 char, so +1 to make it the size
        int padSize = to - j;
        if (padSize > 2) // something is wrong with base64. be safe and go with the upper bound
        {
            return len / 4 * 3;
        }

        // so far this base64 looks like it's unindented tightly packed base64.
        // take a chance and create an array with the expected size
        return len / 4 * 3 - padSize;
    }


    public static byte[] decodeBase64(final CharSequence text) {
        return Base64.decodeBase64(text, 0, text.length());
    }


    public static byte[] decodeBase64(final String text) {
        return Base64.decodeBase64(text, 0, text.length());
    }

   public static byte[] decodeBase64(final String text, final int from, final int length) {
        char[] steal = Strings.steal(text);
        return decodeBase64(steal, from, length);
    }


    /**
     * @param text base64Binary data is likely to be long, and decoding requires each character to be accessed twice
     * (once for counting length, another for decoding.)
     *
     */
    public static byte[] decodeBase64(final CharSequence text, final int from, final int len) {
        final int buflen = guessLength(text, from, len);
        final byte[] out = new byte[buflen];
        int o = 0;

        int i;

        final byte[] quadruplet = new byte[4];
        int q = 0;

        // convert each quadruplet to three bytes.
        int to = from + len;
        for (i = from; i < to; i++) {
            char ch = text.charAt(i);
            byte v = DECODE_MAP[ch];

            if (v != -1) {
                quadruplet[q++] = v;
            }

            if (q == 4) {
                // quadruplet is now filled.
                out[o++] = (byte) ((quadruplet[0] << 2) | (quadruplet[1] >> 4));
                if (quadruplet[2] != PADDING) {
                    out[o++] = (byte) ((quadruplet[1] << 4) | (quadruplet[2] >> 2));
                }
                if (quadruplet[3] != PADDING) {
                    out[o++] = (byte) ((quadruplet[2] << 6) | (quadruplet[3]));
                }
                q = 0;
            }
        }

        if (buflen == o) { // speculation worked out to be OK
            return out;
        }

        // we overestimated, so need to create a new buffer
        byte[] nb = new byte[o];
        System.arraycopy(out, 0, nb, 0, o);
        return nb;
    }


    public static byte[] decodeBase64(final char[] text, final int from, final int len) {
        final int buflen = guessLength(text, from, len);
        final byte[] out = new byte[buflen];
        int o = 0;

        int i;

        final byte[] quadruplet = new byte[4];
        int q = 0;

        // convert each quadruplet to three bytes.
        int to = from + len;
        for (i = from; i < to; i++) {
            char ch = text[i];
            byte v = DECODE_MAP[ch];

            if (v != -1) {
                quadruplet[q++] = v;
            }

            if (q == 4) {
                // quadruplet is now filled.
                out[o++] = (byte) ((quadruplet[0] << 2) | (quadruplet[1] >> 4));
                if (quadruplet[2] != PADDING) {
                    out[o++] = (byte) ((quadruplet[1] << 4) | (quadruplet[2] >> 2));
                }
                if (quadruplet[3] != PADDING) {
                    out[o++] = (byte) ((quadruplet[2] << 6) | (quadruplet[3]));
                }
                q = 0;
            }
        }

        if (buflen == o) { // speculation worked out to be OK
            return out;
        }

        // we overestimated, so need to create a new buffer
        byte[] nb = new byte[o];
        System.arraycopy(out, 0, nb, 0, o);
        return nb;
    }





    private static final char[] ENCODE_MAP = initEncodeMap();

    private static char[] initEncodeMap() {
        char[] map = new char[64];
        int i;
        for (i = 0; i < 26; i++) {
            map[i] = (char) ('A' + i);
        }
        for (i = 26; i < 52; i++) {
            map[i] = (char) ('a' + (i - 26));
        }
        for (i = 52; i < 62; i++) {
            map[i] = (char) ('0' + (i - 52));
        }
        map[62] = '+';
        map[63] = '/';

        return map;
    }

    public static char encode(final int i) {
        return ENCODE_MAP[i & 0x3F];
    }

    public static byte encodeByte(final int i) {
        return (byte) ENCODE_MAP[i & 0x3F];
    }

    public static String encodeBase64(final byte[] input) {
        return Base64.encodeBase64(input, 0, input.length);
    }

    public static String encodeBase64(final byte[] input, final int offset, final int len) {
        char[] buf = new char [(((len + 2) / 3) * 4)];
        int ptr = Base64.encodeBase64(input, offset, len, buf, 0);
        assert ptr == buf.length;
        return Strings.wrap(buf);
    }

    public static void encodeBase64(final byte[] input, final int offset, final int len, final Appendable result)
            throws IOException {
        for (int i = offset; i < len; i += 3) {
            switch (len - i) {
                case 1:
                    result.append(encode(input[i] >> 2));
                    result.append(encode(((input[i]) & 0x3) << 4));
                    result.append("==");
                    break;
                case 2:
                    result.append(encode(input[i] >> 2));
                    result.append(encode(
                            ((input[i] & 0x3) << 4)
                            | ((input[i + 1] >> 4) & 0xF)));
                    result.append(encode((input[i + 1] & 0xF) << 2));
                    result.append('=');
                    break;
                default:
                    result.append(encode(input[i] >> 2));
                    result.append(encode(
                            ((input[i] & 0x3) << 4)
                            | ((input[i + 1] >> 4) & 0xF)));
                    result.append(encode(
                            ((input[i + 1] & 0xF) << 2)
                            | ((input[i + 2] >> 6) & 0x3)));
                    result.append(encode(input[i + 2] & 0x3F));
                    break;
            }
        }
    }

    /**
     * Encodes a byte array into a char array by doing base64 encoding.
     *
     * The caller must supply a big enough buffer.
     *
     * @return the value of {@code ptr+((len+2)/3)*4}, which is the new offset in the output buffer where the further
     * bytes should be placed.
     */
    public static int encodeBase64(final byte[] input, final int offset,
            final int len, final char[] buf, final int cptr) {
        int ptr = cptr;
        for (int i = offset; i < len; i += 3) {
            switch (len - i) {
                case 1:
                    buf[ptr++] = encode(input[i] >> 2);
                    buf[ptr++] = encode(((input[i]) & 0x3) << 4);
                    buf[ptr++] = '=';
                    buf[ptr++] = '=';
                    break;
                case 2:
                    buf[ptr++] = encode(input[i] >> 2);
                    buf[ptr++] = encode(
                            ((input[i] & 0x3) << 4)
                            | ((input[i + 1] >> 4) & 0xF));
                    buf[ptr++] = encode((input[i + 1] & 0xF) << 2);
                    buf[ptr++] = '=';
                    break;
                default:
                    buf[ptr++] = encode(input[i] >> 2);
                    buf[ptr++] = encode(
                            ((input[i] & 0x3) << 4)
                            | ((input[i + 1] >> 4) & 0xF));
                    buf[ptr++] = encode(
                            ((input[i + 1] & 0xF) << 2)
                            | ((input[i + 2] >> 6) & 0x3));
                    buf[ptr++] = encode(input[i + 2] & 0x3F);
                    break;
            }
        }
        return ptr;
    }

    /**
     * Encodes a byte array into another byte array by first doing base64 encoding then encoding the result in ASCII.
     *
     * The caller must supply a big enough buffer.
     *
     * @return the value of {@code ptr+((len+2)/3)*4}, which is the new offset in the output buffer where the further
     * bytes should be placed.
     */
    public static int encodeBase64(final byte[] input, final int offset, final int len,
            final byte[] out, final int cptr) {
        int ptr = cptr;
        byte[] buf = out;
        int max = len + offset;
        for (int i = offset; i < max; i += 3) {
            switch (max - i) {
                case 1:
                    buf[ptr++] = encodeByte(input[i] >> 2);
                    buf[ptr++] = encodeByte(((input[i]) & 0x3) << 4);
                    buf[ptr++] = '=';
                    buf[ptr++] = '=';
                    break;
                case 2:
                    buf[ptr++] = encodeByte(input[i] >> 2);
                    buf[ptr++] = encodeByte(
                            ((input[i] & 0x3) << 4)
                            | ((input[i + 1] >> 4) & 0xF));
                    buf[ptr++] = encodeByte((input[i + 1] & 0xF) << 2);
                    buf[ptr++] = '=';
                    break;
                default:
                    buf[ptr++] = encodeByte(input[i] >> 2);
                    buf[ptr++] = encodeByte(
                            ((input[i] & 0x3) << 4)
                            | ((input[i + 1] >> 4) & 0xF));
                    buf[ptr++] = encodeByte(
                            ((input[i + 1] & 0xF) << 2)
                            | ((input[i + 2] >> 6) & 0x3));
                    buf[ptr++] = encodeByte(input[i + 2] & 0x3F);
                    break;
            }
        }

        return ptr;
    }

}
