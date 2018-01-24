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

    @JmxExport
    public final void jmxStart() {
      startAsync().awaitRunning();
    }

    @Override
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
    public final synchronized Service startAsync() {
        final Service svc = guavaService;
        final State state = svc.state();
        switch (state) {
            case NEW:
                svc.startAsync();
                break;
            case FAILED:
                LOG.warn("Restarting a failed service", svc.failureCause());
                restart();
                break;
            case TERMINATED:
                LOG.info("Restarting a terminated service");
                restart();
                break;
            default:
                throw new IllegalStateException("Service is in invalid state " + state);
        }
        return this;
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
    private void restart() {
        Service newSvc = supplier.get();
        guavaService = newSvc;
        newSvc.startAsync();
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

    @JmxExport
    public final void jmxStop() {
      stopAsync().awaitTerminated();
    }

    @Override
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
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
