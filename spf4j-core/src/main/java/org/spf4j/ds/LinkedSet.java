
package org.spf4j.ds;

import java.util.Set;
import javax.annotation.Nullable;

/**
 *
 * @author zoly
 */
public interface LinkedSet<V> extends Set<V> {

    @Nullable
    V getLastValue();

    @Nullable
    V pollLastValue();

}
