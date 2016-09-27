
package org.spf4j.io.csv;

/**
 *
 * @author zoly
 */
public interface CsvRowHandler<T> {

    void element(CharSequence elem);

    T eof();

}
