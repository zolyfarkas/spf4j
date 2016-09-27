
package org.spf4j.io.csv;

import java.util.Map;

/**
 *
 * @author zoly
 */
public interface CsvMapHandler<T> {

    void row(Map<String, String> row);

    T eof();
}
