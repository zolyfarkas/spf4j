package org.spf4j.concurrent;

import com.google.common.annotations.Beta;
import com.google.common.base.Supplier;
import com.google.common.util.concurrent.Service;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.jmx.JmxExport;
import org.spf4j.jmx.Registry;

/**
 *
 * @author zoly
 */
@Beta
public abstract class RestartableServiceImpl implements RestartableService {

    private static final Logger LOG = LoggerFactory.getLogger(RestartableServiceImpl.class);

    private final Supplier<Service> supplier;

    private volatile Service guavaService;

    public RestartableServiceImpl(final Supplier<Service> supplier) {
        this.supplier = supplier;
        this.guavaService = supplier.get();
    }


    public final void registerToJmx() {
        Registry.export(RestartableService.class.getName(), getServiceName(), this);
    }


    @Override
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH") // this is on purpose.
    @JmxExport
    public final synchronized Service startAsync() {
        final Service svc = guavaService;
        final State state = svc.state();
        switch (state) {
            case NEW:
                svc.startAsync();
                break;
            case FAILED:
                LOG.warn("Restarting a failed service", svc.failureCause());
            case TERMINATED:
                Service newSvc = supplier.get();
                guavaService = newSvc;
                newSvc.startAsync();
                break;
            default:
                throw new IllegalStateException("Service is in invalid state " + state);
        }
        return this;
    }

    @Override
    @JmxExport
    public final boolean isRunning() {
        return guavaService.isRunning();
    }

    @Override
    @JmxExport
    public final State state() {
        return guavaService.state();
    }

    @Override
    @JmxExport
    public final Service stopAsync() {
        guavaService.stopAsync();
        return this;
    }

    @Override
    @JmxExport
    public final void awaitRunning() {
        guavaService.awaitRunning();
    }

    @Override
    public final void awaitRunning(final long timeout, final TimeUnit unit) throws TimeoutException {
        guavaService.awaitRunning(timeout, unit);
    }

    @Override
    @JmxExport
    public final void awaitTerminated() {
        guavaService.awaitTerminated();
    }

    @Override
    public final void awaitTerminated(final long timeout, final TimeUnit unit) throws TimeoutException {
        guavaService.awaitTerminated(timeout, unit);
    }

    @Override
    @JmxExport
    public final Throwable failureCause() {
        return guavaService.failureCause();
    }

    @Override
    public final void addListener(final Listener listener, final Executor executor) {
        guavaService.addListener(listener, executor);
    }

    @Override
    public final void close() {
        guavaService.stopAsync().awaitTerminated();
        Registry.unregister(RestartableService.class.getName(), getServiceName());
    }

    @Override
    public final String toString() {
        return getServiceName();
    }

}
