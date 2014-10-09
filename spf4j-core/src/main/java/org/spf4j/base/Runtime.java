
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

import org.spf4j.concurrent.DefaultExecutor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Runtime.class);

    // Calling Halt is the only sensible thing to do when the JVM is hosed.
    @edu.umd.cs.findbugs.annotations.SuppressWarnings
    public static void goDownWithError(final Throwable t, final int exitCode) {
        try {
            LOGGER.error("Unrecoverable Error, going down", t);
        } finally {
            try {
                t.printStackTrace();
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

    static {
        final java.lang.Runtime runtime = java.lang.Runtime.getRuntime();
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        final int availableProcessors = runtime.availableProcessors();
        if (availableProcessors <= 0) {
            LOGGER.warn("Number of processors returned by MBean is invalid: {} defaultng to 1", availableProcessors);
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
                    for (Runnable runnable : SHUTDOWN_HOOKS) {
                        try {
                            runnable.run();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }, "tsdb shutdown"));
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
    
    public static int getNrOpenFiles() throws IOException, InterruptedException, ExecutionException {
        if (isMacOsx()) {
            LineCountCharHandler handler = new LineCountCharHandler();
            run("/usr/sbin/lsof -p " + PID, handler);
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
    @edu.umd.cs.findbugs.annotations.SuppressWarnings
    public static String getLsofOutput() throws IOException, InterruptedException, ExecutionException {
        File lsofFile = new File("/usr/sbin/lsof");
        if (!lsofFile.exists()) {
            lsofFile = new File("/usr/bin/lsof");
            if (!lsofFile.exists()) {
                lsofFile = new File("/usr/local/bin/lsof");
                if (!lsofFile.exists()) {
                    return null;
                }
            }
        }
        StringBuilderCharHandler handler = new StringBuilderCharHandler();
        run(lsofFile.getAbsolutePath() + " -p " + PID, handler);
        return handler.toString();
    }

    public interface ProcOutputHandler {

        void handleStdOut(int character);

        void handleStdErr(int character);
    }

    public static int run(final String command, final ProcOutputHandler handler)
            throws IOException, InterruptedException, ExecutionException {
        Process proc = java.lang.Runtime.getRuntime().exec(command);
        try (InputStream pos = proc.getInputStream();
                InputStream pes = proc.getErrorStream();
                OutputStream pis = proc.getOutputStream()) {
            Future<?> esh = DefaultExecutor.INSTANCE.submit(new AbstractRunnable() {
                @Override
                public void doRun() throws Exception {
                    int eos;
                    while ((eos = pes.read()) >= 0) {
                        handler.handleStdErr(eos);
                    }
                }
            });
            int cos;
            while ((cos = pos.read()) >= 0) {
                handler.handleStdOut(cos);
            }
            esh.get();
        }
        return proc.waitFor();
    }

    private static class LineCountCharHandler implements ProcOutputHandler {

        public LineCountCharHandler() {
            lineCount = 0;
        }
        private int lineCount;

        @Override
        public void handleStdOut(final int c) {
            if (c == '\n') {
                lineCount++;
            }
        }

        public int getLineCount() {
            return lineCount;
        }

        @Override
        public void handleStdErr(final int character) {
        }
    }

    private static class StringBuilderCharHandler implements ProcOutputHandler {

        public StringBuilderCharHandler() {
            builder = new StringBuilder();
        }
        private final StringBuilder builder;

        @Override
        public void handleStdOut(final int c) {
            builder.append((char) c);
        }

        @Override
        public String toString() {
            return builder.toString();
        }

        @Override
        public void handleStdErr(final int c) {
            builder.append(c);
        }
    }
    private static final LinkedList<Runnable> SHUTDOWN_HOOKS = new LinkedList<>();


    public static void addHookAtBeginning(final Runnable runnable) {
        synchronized (SHUTDOWN_HOOKS) {
            SHUTDOWN_HOOKS.addFirst(runnable);
        }
    }

    public static void addHookAtEnd(final Runnable runnable) {
        synchronized (SHUTDOWN_HOOKS) {
            SHUTDOWN_HOOKS.addLast(runnable);
        }
    }
}
