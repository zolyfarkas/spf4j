
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
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.spf4j.base.Runtime.Lsof.LSOF;
import static org.spf4j.base.Runtime.Lsof.LSOF_CMD;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.io.ByteArrayBuilder;
import org.spf4j.recyclable.impl.ArraySuppliers;
import org.spf4j.stackmonitor.FastStackCollector;

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
    public static final int WAIT_FOR_SHUTDOWN_MILLIS = Integer.getInteger("spf4j.waitForShutdownMillis", 30000);
    public static final String TMP_FOLDER = System.getProperty("java.io.tmpdir");
    public static final int PID;
    public static final String OS_NAME;
    public static final String PROCESS_NAME;
    public static final int NR_PROCESSORS;
    public static final String JAVA_VERSION = System.getProperty("java.version");
    public static final String USER_NAME = System.getProperty("user.name");
    public static final String USER_DIR = System.getProperty("user.dir");
    public static final String USER_HOME = System.getProperty("user.home");
    public static final String JAVA_HOME = System.getProperty("java.home");

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
            public void doRun() {
                SortedMap<Integer, Set<Runnable>> hooks;
                synchronized (SHUTDOWN_HOOKS) {
                    hooks = new TreeMap<>(SHUTDOWN_HOOKS);
                    for (Map.Entry<Integer, Set<Runnable>> entry: hooks.entrySet()) {
                        entry.setValue(new HashSet<>(entry.getValue()));
                    }
                }
                for (Map.Entry<Integer, Set<Runnable>> runnables : hooks.entrySet()) {
                    final Set<Runnable> values = runnables.getValue();
                    if (values.size() <= 1) {
                        for (Runnable runnable : values) {
                            try {
                                runnable.run();
                            } catch (RuntimeException ex) {
                                ex.printStackTrace();
                            }
                        }
                    } else if (((int) runnables.getKey()) >= Integer.MAX_VALUE) {
                        Thread[] threads = new Thread[values.size()];
                        int i = 0;
                        for (Runnable runnable : values) {
                            Thread thread = new Thread(runnable);
                            thread.start();
                            threads[i++] = thread;
                        }
                        long deadline = System.currentTimeMillis() + WAIT_FOR_SHUTDOWN_MILLIS;
                        for (Thread thread : threads) {
                            try {
                                thread.join(deadline - System.currentTimeMillis());
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }
                    } else {
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
                // print out info on all remaining non daemon threads.
                Thread[] threads = FastStackCollector.getThreads();
                Thread current = Thread.currentThread();
                boolean first = true;
                for (Thread thread : threads) {
                    if (thread.isAlive() && !thread.isDaemon() && !thread.equals(current)) {
                        if (first) {
                            System.err.println("Non daemon threads still running:");
                            first = false;
                        }
                        System.err.println("Non daemon thread " + thread + ", stackTrace = "
                                + java.util.Arrays.toString(thread.getStackTrace()));
                    }
                }
            }
        }, "spf4j queued shutdown"));
        //JAVA_PLATFORM = Version.fromSpecVersion(runtimeMxBean.getSpecVersion());
        JAVA_PLATFORM = Version.fromSpecVersion(JAVA_VERSION);
    }
    public static final String MAC_OS_X_OS_NAME = "Mac OS X";
    private static final Path FD_FOLDER = Paths.get("/proc/" + PID + "/fd");

    private static final boolean IS_MAC_OSX = MAC_OS_X_OS_NAME.equals(OS_NAME);

    public static boolean isMacOsx() {
        return IS_MAC_OSX;
    }

    private static final boolean IS_WINDOWS = OS_NAME.startsWith("Windows");

    public static boolean isWindows() {
        return IS_WINDOWS;
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

        private static final File BASH = new File("/usr/bin/bash");

        private static final File ULIMIT = new File("/usr/bin/ulimit");


        private static final String [] ULIMIT_CMD;

        public static final int MAX_NR_OPENFILES;

        static {
            int mfiles;
            if (ULIMIT.exists() && ULIMIT.canExecute()) {
                ULIMIT_CMD = new String[]{ULIMIT.getPath()};
            } else if (BASH.exists() && BASH.canExecute()) {
                ULIMIT_CMD = new String[]{BASH.getPath(), "-c", "ulimit"};
            } else {
                ULIMIT_CMD = null;
            }
            if (ULIMIT_CMD == null) {
                Lazy.LOGGER.warn("No ulimit available on {}, assuming no limit for nr open files",
                        Runtime.OS_NAME);
                mfiles = Integer.MAX_VALUE;
            } else {
                mfiles = runUlimit("-Sn");
            }
            MAX_NR_OPENFILES = mfiles;
        }

        public static int runUlimit(final String ... options) {
            if (ULIMIT_CMD == null) {
                throw new RuntimeException("Ulimit not available on " + Runtime.OS_NAME);
            }
            int mfiles;
            try {
                String result = Runtime.run(Arrays.concat(ULIMIT_CMD, options), 10000);
                if (result.contains("unlimited")) {
                    mfiles = Integer.MAX_VALUE;
                } else {
                    mfiles = Integer.parseInt(result.trim());
                }
            } catch (TimeoutException | IOException | InterruptedException | ExecutionException ex) {
                Lazy.LOGGER.error("Error while running ulimit, assuming no limit", ex);
                mfiles = Integer.MAX_VALUE;
            }
            return mfiles;
        }

    }


    /**
     *
     * @return -1 if cannot get nr of open files
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     */

    public static int getNrOpenFiles() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        if (isMacOsx()) {
            if (Lsof.LSOF == null) {
                return -1;
            }
            LineCountCharHandler handler = new LineCountCharHandler();
            run(LSOF_CMD, handler, 60000);
            return handler.getLineCount() - 1;
        } else {
            if (Files.isDirectory(FD_FOLDER)) {
                int result = 0;
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(FD_FOLDER)) {
                    Iterator<Path> iterator = stream.iterator();
                    while (iterator.hasNext()) {
                        iterator.next();
                        result++;
                    }
                }
                return result;
            } else {
                return -1;
            }
        }
    }

    @Nullable
    public static String getLsofOutput()
            throws IOException, InterruptedException, ExecutionException, TimeoutException {
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
            final long timeoutMillis) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        StringBuilderCharHandler handler = new StringBuilderCharHandler();
        int result = run(command, handler, timeoutMillis);
        if (result != 0) {
            throw new ExecutionException("Error While Executing: " + java.util.Arrays.toString(command)
                                           + ";\n returned " + result + ";\n stdErr = " + handler.getStdErr(), null);
        } else {
            return handler.getStdOut();
        }
    }


    public static void killProcess(final Process proc) {
        if (JAVA_PLATFORM.ordinal() >= Version.V1_8.ordinal()) {
            final Class<? extends Process> aClass = proc.getClass();
            try {
                if (isAlive(proc)) {
                    aClass.getMethod("destroyForcibly").invoke(proc);
                }
            } catch (NoSuchMethodException | SecurityException |
                    IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            proc.destroy();
        }
    }


    @SuppressFBWarnings("NPMC_NON_PRODUCTIVE_METHOD_CALL")
    public static boolean isAlive(final Process proc) {
        if (JAVA_PLATFORM.ordinal() >= Version.V1_8.ordinal()) {
            try {
                return (Boolean) Process.class.getMethod("isAlive").invoke(proc);
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException
                    | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            try {
                proc.exitValue();
                return false;
            } catch (IllegalThreadStateException ex) {
                return true;
            }
        }
    }


    @SuppressFBWarnings("LEST_LOST_EXCEPTION_STACK_TRACE") // not really lost, suppressed exceptions are used.
    public static int run(final String [] command, final ProcOutputHandler handler,
            final long timeoutMillis)
            throws IOException, InterruptedException, ExecutionException, TimeoutException {
        final Process proc = java.lang.Runtime.getRuntime().exec(command);
        final AtomicBoolean timedOut = new AtomicBoolean(false);
        ScheduledFuture<?> schedule = DefaultScheduler.INSTANCE.schedule(new AbstractRunnable(false) {

            @Override
            public void doRun() throws Exception {
                timedOut.set(true);
                killProcess(proc);
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
            Future<?> osh = DefaultExecutor.INSTANCE.submit(new AbstractRunnable() {
                @Override
                public void doRun() throws Exception {
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
                }
            });
            try {
                int result = proc.waitFor();
                esh.get();
                osh.get();
                if (timedOut.get()) {
                    killProcess(proc);
                    throw new TimeoutException("Timed out while executing: " + java.util.Arrays.toString(command)
                     + ";\n process returned " + result + ";\n output handler: " + handler);
                } else {
                    return result;
                }
            } catch (ExecutionException | InterruptedException | RuntimeException ex) {
                 killProcess(proc);
                 throw ex;
            }

        } catch (ExecutionException | IOException | InterruptedException | RuntimeException ex) {
            if (timedOut.get()) {
               TimeoutException te = new TimeoutException("Timed out while executing: "
                       + java.util.Arrays.toString(command)
                + ";\n output handler: " + handler);
               te.addSuppressed(ex);
               throw te;
            } else {
                throw ex;
            }
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

    public static long millisToDeadline() throws TimeoutException {
        final long deadline = DEADLINE.get();
        long result = deadline - System.currentTimeMillis();
        if (result < 0) {
            throw new TimeoutException("Deadline passed " + ISODateTimeFormat.basicDateTime().print(deadline));
        } else {
            return result;
        }
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

    public static String jrun(final Class<?> classWithMain,
            final long timeoutMillis, final String ... arguments)
            throws IOException, InterruptedException, ExecutionException, TimeoutException {
        final String classPath = ManagementFactory.getRuntimeMXBean().getClassPath();
        return jrun(classWithMain, classPath, timeoutMillis,  arguments);
    }

    public static String jrun(final Class<?> classWithMain, final String classPath, final long timeoutMillis,
            final String... arguments) throws InterruptedException, ExecutionException, TimeoutException, IOException {
        final String jvmPath = JAVA_HOME + File.separatorChar + "bin" + File.separatorChar + "java";
        String[] command = Arrays.concat(new String[] {jvmPath, "-cp", classPath, classWithMain.getName() }, arguments);
        return run(command, timeoutMillis);
    }

}
