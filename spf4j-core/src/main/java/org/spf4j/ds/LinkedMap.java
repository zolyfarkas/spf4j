
package org.spf4j.ds;

import java.util.Map;
import javax.annotation.Nullable;

/**
 *
 * @author zoly
 */
public interface LinkedMap<K, V> extends Map<K, V> {

    @Nullable
    Map.Entry<K, V> getLastEntry();

    @Nullable
    Map.Entry<K, V> pollLastEntry();

}
