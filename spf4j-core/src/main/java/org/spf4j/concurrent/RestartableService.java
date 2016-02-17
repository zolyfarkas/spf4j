
package org.spf4j.concurrent;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.Service;

/**
 * THis is only a marker interface that will let you know that this Guava service is restartable.
 * @author zoly
 */
@Beta
public interface RestartableService extends Service, AutoCloseable {
    String getServiceName();
}
