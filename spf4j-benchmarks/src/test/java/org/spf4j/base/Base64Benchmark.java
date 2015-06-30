
package org.spf4j.base;

import com.google.common.io.BaseEncoding;
import javax.xml.bind.DatatypeConverter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

/**
 *
 * @author zoly
 */
@State(Scope.Benchmark)
@Fork(2)
@Threads(value = 8)
public class Base64Benchmark {




    private static final byte [] TEST_ARRAY = Strings.toUtf8("ajkfhskhfkdsjhfkshdkfgj"
            + "$%^&IOJHBVCDSERT%TYUIO)(*&^%$#@!@#$%^&*()(*&^%$#WSDVBNJKIUYFEWERTYUI)(*"
            + " XCVBNM<>LKJHG   POIY TRYYT OUPOJ^%$#WSedtfgh itrcsdxcsvko1poisuwcytr542"
            + "gfdgykio9876redfghjkiugfrtghjkjhgtrghjiduygfghjk167890-vokcnsacghd&^%$h"
            + "$%^&IOJHBVCDSERT%TYUIO)(*&^%$#@!@#$%^&*()(*&^%$#WSDVBNJKIUYFEWERTYUI)(*"
            + " XCVBNM<>LKJHG   POIY TRYYT OUPOJ^%$#WSedtfgh itrcsdxcsvko1poisuwcytr542"
            + "gfdgykio9876redfghjkiugfrtghjkjhgtrghjiduygfghjk167890-vokcnsacghd&^%$h"
            + "$%^&IOJHBVCDSERT%TYUIO)(*&^%$#@!@#$%^&*()(*&^%$#WSDVBNJKIUYFEWERTYUI)(*"
            + " XCVBNM<>LKJHG   POIY TRYYT OUPOJ^%$#WSedtfgh itrcsdxcsvko1poisuwcytr542"
            + "gfdgykio9876redfghjkiugfrtghjkjhgtrghjiduygfghjk167890-vokcnsacghd&^%$h"
            + "$%^&IOJHBVCDSERT%TYUIO)(*&^%$#@!@#$%^&*()(*&^%$#WSDVBNJKIUYFEWERTYUI)(*"
            + " XCVBNM<>LKJHG   POIY TRYYT OUPOJ^%$#WSedtfgh itrcsdxcsvko1poisuwcytr542"
            + "gfdgykio9876redfghjkiugfrtghjkjhgtrghjiduygfghjk167890-vokcnsacghd&^%$h"
            + "$%^&IOJHBVCDSERT%TYUIO)(*&^%$#@!@#$%^&*()(*&^%$#WSDVBNJKIUYFEWERTYUI)(*"
            + " XCVBNM<>LKJHG   POIY TRYYT OUPOJ^%$#WSedtfgh itrcsdxcsvko1poisuwcytr542"
            + "gfdgykio9876redfghjkiugfrtghjkjhgtrghjiduygfghjk167890-vokcnsacghd&^%$h"
            + "$%^&IOJHBVCDSERT%TYUIO)(*&^%$#@!@#$%^&*()(*&^%$#WSDVBNJKIUYFEWERTYUI)(*"
            + " XCVBNM<>LKJHG   POIY TRYYT OUPOJ^%$#WSedtfgh itrcsdxcsvko1poisuwcytr542"
            + "gfdgykio9876redfghjkiugfrtghjkjhgtrghjiduygfghjk167890-vokcnsacghd&^%$h"
            + "$%^&IOJHBVCDSERT%TYUIO)(*&^%$#@!@#$%^&*()(*&^%$#WSDVBNJKIUYFEWERTYUI)(*"
            + " XCVBNM<>LKJHG   POIY TRYYT OUPOJ^%$#WSedtfgh itrcsdxcsvko1poisuwcytr542"
            + "gfdgykio9876redfghjkiugfrtghjkjhgtrghjiduygfghjk167890-vokcnsacghd&^%$h"
            + "$%^&IOJHBVCDSERT%TYUIO)(*&^%$#@!@#$%^&*()(*&^%$#WSDVBNJKIUYFEWERTYUI)(*"
            + " XCVBNM<>LKJHG   POIY TRYYT OUPOJ^%$#WSedtfgh itrcsdxcsvko1poisuwcytr542"
            + "gfdgykio9876redfghjkiugfrtghjkjhgtrghjiduygfghjk167890-vokcnsacghd&^%$h"
            + "$%^&IOJHBVCDSERT%TYUIO)(*&^%$#@!@#$%^&*()(*&^%$#WSDVBNJKIUYFEWERTYUI)(*"
            + " XCVBNM<>LKJHG   POIY TRYYT OUPOJ^%$#WSedtfgh itrcsdxcsvko1poisuwcytr542"
            + "gfdgykio9876redfghjkiugfrtghjkjhgtrghjiduygfghjk167890-vokcnsacghd&^%$h");


    private static final byte [] TEST_ARRAY_LARGE;

    static {
        TEST_ARRAY_LARGE = new byte[TEST_ARRAY.length * 10];
        int k = 0;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < TEST_ARRAY.length; j++) {
                TEST_ARRAY_LARGE[k++] = TEST_ARRAY[j];
            }
        }
    }

    @Benchmark
    public byte [] testSpf4jBase64() {
        String encodeBase64 = Base64.encodeBase64(TEST_ARRAY);
        return Base64.decodeBase64(encodeBase64);
    }


    @Benchmark
    public byte [] testJdkBase64() {
        String encodeBase64 = DatatypeConverter.printBase64Binary(TEST_ARRAY);
        return DatatypeConverter.parseBase64Binary(encodeBase64);
    }

    private static final BaseEncoding G_BASE64 = BaseEncoding.base64();


    @Benchmark
    public byte [] testGuavaBase64() {
        String encodeBase64 = G_BASE64.encode(TEST_ARRAY);
        return G_BASE64.decode(encodeBase64);
    }

    @Benchmark
    public byte [] testSpf4jBase64Large() {
        String encodeBase64 = Base64.encodeBase64(TEST_ARRAY_LARGE);
        return Base64.decodeBase64(encodeBase64);
    }


    @Benchmark
    public byte [] testJdkBase64Large() {
        String encodeBase64 = DatatypeConverter.printBase64Binary(TEST_ARRAY_LARGE);
        return DatatypeConverter.parseBase64Binary(encodeBase64);
    }

    @Benchmark
    public byte [] testGuavaBase64Large() {
       String encodeBase64 = G_BASE64.encode(TEST_ARRAY_LARGE);
       return G_BASE64.decode(encodeBase64);
    }


}
