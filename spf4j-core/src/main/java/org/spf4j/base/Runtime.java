
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.spf4j.concurrent.DefaultExecutor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.spf4j.base.Runtime.Lsof.LSOF;
import static org.spf4j.base.Runtime.Lsof.LSOF_CMD;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.io.ByteArrayBuilder;
import org.spf4j.recyclable.impl.ArraySuppliers;

/**
 *
 * @author zoly
 */
public final class Runtime {

    private Runtime() {
    }

    public enum Version {

        V1_0, V1_1, V1_2, V1_3, V1_4, V1_5, V1_6, V1_7, V1_8, V1_9_PLUSZ;

        public static Version fromSpecVersion(final String specVersion) {
            return Version.values()[Integer.parseInt(specVersion.split("\\.")[1])];
        }
    }


    public static final Version JAVA_PLATFORM;


    private static class Lazy {
        private static final Logger LOGGER = LoggerFactory.getLogger(Lazy.class);
    }

    // Calling Halt is the only sensible thing to do when the JVM is hosed.
    @edu.umd.cs.findbugs.annotations.SuppressWarnings
    public static void goDownWithError(final Throwable t, final int exitCode) {
        try {
            Lazy.LOGGER.error("Unrecoverable Error, going down", t);
        } finally {
            try {
                if (t != null) {
                    t.printStackTrace();
                }
            } finally {
                java.lang.Runtime.getRuntime().halt(exitCode);
            }
        }
    }
    public static final String TMP_FOLDER = System.getProperty("java.io.tmpdir");
    public static final int PID;
    public static final String OS_NAME;
    public static final String PROCESS_NAME;
    public static final int NR_PROCESSORS;
    public static final String JAVA_VERSION = System.getProperty("java.version");
    public static final String USER_NAME = System.getProperty("user.name");
    public static final String USER_DIR = System.getProperty("user.dir");

    private static final SortedMap<Integer, Set<Runnable>> SHUTDOWN_HOOKS = new TreeMap<>();

    static {
        final java.lang.Runtime runtime = java.lang.Runtime.getRuntime();
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        final int availableProcessors = runtime.availableProcessors();
        if (availableProcessors <= 0) {
            System.err.println("Invalid number of processors " + availableProcessors
                + " defaulting to 1");
            NR_PROCESSORS = 1;
        } else {
            NR_PROCESSORS = availableProcessors;
        }
        PROCESS_NAME = runtimeMxBean.getName();
        int atIdx = PROCESS_NAME.indexOf('@');
        if (atIdx < 0) {
            PID = -1;
        } else {
            PID = Integer.parseInt(PROCESS_NAME.substring(0, atIdx));
        }
        OS_NAME = System.getProperty("os.name");

        runtime.addShutdownHook(new Thread(new AbstractRunnable(false) {
            @Override
            public void doRun() throws Exception {
                synchronized (SHUTDOWN_HOOKS) {
                    for (Map.Entry<Integer, Set<Runnable>> runnables : SHUTDOWN_HOOKS.entrySet()) {
                            final Set<Runnable> values = runnables.getValue();
                            List<Future<?>> futures = new ArrayList<>(values.size());
                            for (Runnable runnable : values) {
                                futures.add(DefaultExecutor.INSTANCE.submit(runnable));
                            }
                            for (Future<?> future : futures) {
                                try {
                                    future.get();
                                } catch (InterruptedException | ExecutionException | RuntimeException e) {
                                    e.printStackTrace();
                                }
                            }
                    }
                }
            }
        }, "spf4j queued shutdown"));
        //JAVA_PLATFORM = Version.fromSpecVersion(runtimeMxBean.getSpecVersion());
        JAVA_PLATFORM = Version.fromSpecVersion(JAVA_VERSION);
    }
    public static final String MAC_OS_X_OS_NAME = "Mac OS X";
    private static final File FD_FOLDER = new File("/proc/" + PID + "/fd");

    public static boolean isMacOsx() {
        return MAC_OS_X_OS_NAME.equals(OS_NAME);
    }

    public static boolean isWindows() {
        return OS_NAME.startsWith("Windows");
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    public static final class Lsof {

        public static final File LSOF;

        static {
            File lsofFile = new File("/usr/sbin/lsof");
            if (!lsofFile.exists() || !lsofFile.canExecute()) {
                lsofFile = new File("/usr/bin/lsof");
                if (!lsofFile.exists() || !lsofFile.canExecute()) {
                    lsofFile = new File("/usr/local/bin/lsof");
                    if (!lsofFile.exists() || !lsofFile.canExecute()) {
                        lsofFile = null;
                    }
                }
            }
            LSOF = lsofFile;
        }

        public static final String [] LSOF_CMD = (LSOF == null) ? null
                : new String [] {LSOF.getAbsolutePath(), "-p", Integer.toString(PID) };

    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    public static final class Ulimit {

        private static final File ULIMIT = new File("/usr/bin/ulimit");

        private static final String [] MFILES_CMD = new String [] {ULIMIT.getPath(), "-Sn" };

        public static final int MAX_NR_OPENFILES;

        static {
            int mfiles;
            if (ULIMIT.exists() && ULIMIT.canExecute()) {
                try {
                    String result = Runtime.run(MFILES_CMD, 10000);
                    if (result.contains("unlimited")) {
                        mfiles = Integer.MAX_VALUE;
                    } else {
                        mfiles = Integer.parseInt(result.trim());
                    }
                } catch (IOException | InterruptedException | ExecutionException ex) {
                    Lazy.LOGGER.error("Error while running ulimit, assuming no limit", ex);
                    mfiles = Integer.MAX_VALUE;
                }
            } else {
                Lazy.LOGGER.warn("No ulimit, assuming no limit");
                mfiles = Integer.MAX_VALUE;
            }
            MAX_NR_OPENFILES = mfiles;
        }

    }


    /**
     *
     * @return -1 if cannot get nr of open files
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     */

    public static int getNrOpenFiles() throws IOException, InterruptedException, ExecutionException {
        if (isMacOsx()) {
            if (Lsof.LSOF == null) {
                return -1;
            }
            LineCountCharHandler handler = new LineCountCharHandler();
            run(LSOF_CMD, handler, 60000);
            return handler.getLineCount() - 1;
        } else {
            if (FD_FOLDER.exists()) {
                final String[] list = FD_FOLDER.list();
                if (list == null) {
                    return -1;
                }
                return list.length;
            } else {
                return -1;
            }
        }
    }

    @Nullable
    public static String getLsofOutput() throws IOException, InterruptedException, ExecutionException {
        if (LSOF == null) {
            return null;
        }
        return run(LSOF_CMD, 60000);
    }

    public interface ProcOutputHandler {

        void handleStdOut(byte[] bytes, int length);

        void stdOutDone();

        void handleStdErr(byte[] bytes, int length);

        void stdErrDone();
    }

    public static String run(final String [] command,
            final long timeoutMillis) throws IOException, InterruptedException, ExecutionException {
        StringBuilderCharHandler handler = new StringBuilderCharHandler();
        int result = run(command, handler, timeoutMillis);
        if (result != 0) {
            throw new ExecutionException("Error While Executing: " + java.util.Arrays.toString(command)
                                           + "; returned " + result + "; stdErr = " + handler.getStdErr(), null);
        } else {
            return handler.getStdOut();
        }
    }


    public static int run(final String [] command, final ProcOutputHandler handler,
            final long timeoutMillis)
            throws IOException, InterruptedException, ExecutionException {
        final Process proc = java.lang.Runtime.getRuntime().exec(command);
        ScheduledFuture<?> schedule = DefaultScheduler.INSTANCE.schedule(new AbstractRunnable(false) {

            @Override
            public void doRun() throws Exception {
                if (JAVA_PLATFORM.ordinal() >= Version.V1_8.ordinal()) {
                    final Class<? extends Process> aClass = proc.getClass();
                    if ((Boolean) aClass.getMethod("isAlive").invoke(proc)) {
                        aClass.getMethod("destroyForcibly").invoke(proc);
                    }
                } else {
                    proc.destroy();
                }
            }
        }, timeoutMillis, TimeUnit.MILLISECONDS);
        try (InputStream pos = proc.getInputStream();
                InputStream pes = proc.getErrorStream();
                OutputStream pis = proc.getOutputStream()) {
            Future<?> esh = DefaultExecutor.INSTANCE.submit(new AbstractRunnable() {
                @Override
                public void doRun() throws Exception {
                    int eos;
                    byte [] buffer = ArraySuppliers.Bytes.TL_SUPPLIER.get(8192);
                    try {
                        while ((eos = pes.read(buffer)) >= 0) {
                            handler.handleStdErr(buffer, eos);
                        }
                    } finally {
                        ArraySuppliers.Bytes.TL_SUPPLIER.recycle(buffer);
                        handler.stdErrDone();
                    }
                }
            });
            int cos;
            byte [] buffer = ArraySuppliers.Bytes.TL_SUPPLIER.get(8192);
            try {
                while ((cos = pos.read(buffer)) >= 0) {
                    handler.handleStdOut(buffer, cos);
                }
            } finally {
                ArraySuppliers.Bytes.TL_SUPPLIER.recycle(buffer);
                handler.stdOutDone();
            }
            esh.get();
            return proc.waitFor();
        } finally {
            schedule.cancel(false);
        }
    }

    /**
     * todo: character enconding is not really don eproperly...
     */
    public static final class LineCountCharHandler implements ProcOutputHandler {

        public LineCountCharHandler() {
            lineCount = 0;
        }
        private int lineCount;

        @Override
        public void handleStdOut(final byte [] buffer, final int length) {
            for (int i = 0; i < length; i++) {
                byte c = buffer[i];
                if (c ==  (byte) '\n') {
                    lineCount++;
                }
            }
        }

        public int getLineCount() {
            return lineCount;
        }

        @Override
        public void handleStdErr(final byte [] buffer, final int length) {
            handleStdOut(buffer, length);
        }

        @Override
        public void stdOutDone() {
        }

        @Override
        public void stdErrDone() {
        }
    }

    public static final class StringBuilderCharHandler implements ProcOutputHandler {

        private final Charset charset;

        private final ByteArrayBuilder stdout;

        private final ByteArrayBuilder stderr;


        public StringBuilderCharHandler(final Charset charset) {
            stdout = new ByteArrayBuilder(128, ArraySuppliers.Bytes.JAVA_NEW);
            stderr = new ByteArrayBuilder(0, ArraySuppliers.Bytes.JAVA_NEW);
            this.charset = charset;
        }

        public StringBuilderCharHandler() {
            this(Charset.defaultCharset());
        }

        @Override
        public void handleStdOut(final byte [] buffer, final int length) {
            stdout.write(buffer, 0, length);
        }

        @Override
        public String toString() {
            return new String(stdout.getBuffer(), 0, stdout.size(), charset);
        }

        public String getStdOut() {
            return toString();
        }

        public String getStdErr() {
            return new String(stderr.getBuffer(), 0, stderr.size(), charset);
        }

        @Override
        public void handleStdErr(final byte [] buffer, final int length) {
            stderr.write(buffer, 0, length);
        }

        @Override
        public void stdOutDone() {
        }

        @Override
        public void stdErrDone() {
        }
    }



    public static void queueHookAtBeginning(final Runnable runnable) {
        synchronized (SHUTDOWN_HOOKS) {
            queueHook(Integer.MIN_VALUE, runnable);
        }
    }

    public static void queueHookAtEnd(final Runnable runnable) {
        queueHook(Integer.MAX_VALUE, runnable);
    }

    public static void queueHook(final int priority, final Runnable runnable) {
        synchronized (SHUTDOWN_HOOKS) {
            Integer pr = priority;
            Set<Runnable> runnables = SHUTDOWN_HOOKS.get(pr);
            if (runnables == null) {
                runnables = new HashSet<>();
                SHUTDOWN_HOOKS.put(pr, runnables);
            }
            runnables.add(runnable);
        }
    }

    public static boolean removeQueuedShutdownHook(final Runnable runnable) {
        if ("spf4j queued shutdown".equals(Thread.currentThread().getName())) {
            return false;
        }
        synchronized (SHUTDOWN_HOOKS) {
            for (Set<Runnable> entry : SHUTDOWN_HOOKS.values()) {
                if (entry.remove(runnable)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static final ThreadLocal<Long> DEADLINE = new ThreadLocal<Long>() {

        @Override
        protected Long initialValue() {
            return Long.MAX_VALUE;
        }

    };

    public static long getDeadline() {
        return DEADLINE.get();
    }

    public static void setDeadline(final long deadline) {
        DEADLINE.set(deadline);
    }

    /**
     * Attempts to run the GC in a verifiable way.
     * @param timeoutMillis
     * @return true if GC executed for sure, false otherwise.
     */
    @SuppressFBWarnings
    public static boolean gc(final long timeoutMillis) {
        Object obj = new Object();
        WeakReference ref = new WeakReference<>(obj);
        obj = null;
        long deadline = System.currentTimeMillis() + timeoutMillis;
        do {
            System.gc();
        } while (ref.get() != null && System.currentTimeMillis() < deadline);
        return ref.get() == null;
    }

}
