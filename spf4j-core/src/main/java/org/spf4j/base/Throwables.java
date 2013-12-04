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
package org.spf4j.base;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * utility class for throwables.
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class Throwables {

    private Throwables() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(Throwables.class);

    private static final int MAX_THROWABLE_CHAIN
            = Integer.parseInt(System.getProperty("throwables.max.chain", "200"));

    private static final Field CAUSE_FIELD;

    private static final Method ADD_SUPPRESSED;

    static {
        try {
            CAUSE_FIELD = Throwable.class.getDeclaredField("cause");
        } catch (NoSuchFieldException ex) {
            throw new RuntimeException(ex);
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        }
        AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                CAUSE_FIELD.setAccessible(true);
                return null; // nothing to return
            }
        });
    }
    static {
        Method m;
        try {
            m = Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class);
        } catch (NoSuchMethodException ex) {
            m = null;
        } catch (SecurityException ex) {
            m = null;
        }
        ADD_SUPPRESSED = m;
        if (ADD_SUPPRESSED != null) {
            AccessController.doPrivileged(new PrivilegedAction() {
                @Override
                public Object run() {
                    ADD_SUPPRESSED.setAccessible(true);
                    return null; // nothing to return
                }
            });
        }

    }

    private static Throwable chain0(final Throwable t, final Throwable cause) {
        final Throwable rc = com.google.common.base.Throwables.getRootCause(t);
        try {
            AccessController.doPrivileged(new PrivilegedAction() {
                @Override
                public Object run() {
                    try {
                        CAUSE_FIELD.set(rc, cause);
                    } catch (IllegalArgumentException ex) {
                        throw new RuntimeException(ex);
                    } catch (IllegalAccessException ex) {
                        throw new RuntimeException(ex);
                    }
                    return null; // nothing to return
                }
            });

        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        }
        return t;
    }

    /**
     * This method will clone the exception t and will set a new root cause.
     *
     * @param <T>
     * @param t
     * @param newRootCause
     * @return
     */
    public static <T extends Throwable> T chain(final T t, final Throwable newRootCause) {
        int chainedExNr = com.google.common.base.Throwables.getCausalChain(t).size();
        if (chainedExNr >= MAX_THROWABLE_CHAIN) {
            LOG.warn("Trimming exception", newRootCause);
            return t;
        }
        List<Throwable> newRootCauseChain = com.google.common.base.Throwables.getCausalChain(newRootCause);
        int newChainIdx = 0;
        if (chainedExNr + newRootCauseChain.size() > MAX_THROWABLE_CHAIN) {
            newChainIdx = newRootCauseChain.size() - (MAX_THROWABLE_CHAIN - chainedExNr);
            LOG.warn("Trimming exception at {} ", newChainIdx, newRootCause);
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            try {
                out.writeObject(t);
            } finally {
                out.close();
            }

            T result;
            ObjectInputStream in = new ObjectInputStream(
                    new ByteArrayInputStream(bos.toByteArray()));
            try {
                result = (T) in.readObject();
            } finally {
                in.close();
            }
            chain0(result, newRootCauseChain.get(newChainIdx));
            return result;
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Functionality equivalent for java 1.7 Throwable.addSuppressed.
     *
     * @param <T>
     * @param t
     * @param suppressed
     * @return
     */
    public static <T extends Throwable> T suppress(final T t, final Throwable suppressed) {
        if (ADD_SUPPRESSED != null) {
            try {
                ADD_SUPPRESSED.invoke(t, suppressed);
                return t;
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException(ex);
            } catch (InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            if (suppressed == null) {
                throw new IllegalArgumentException("Cannot suppress null exception");
            }
            if (t == suppressed) {
                throw new IllegalArgumentException("Self suppression not permitted");
            }
            return chain(t, new SuppressedThrowable(suppressed));
        }
    }

}
