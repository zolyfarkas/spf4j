
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
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
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Runtime.class);

    public static void goDownWithError(final Throwable t, final int exitCode) {
        try {
            LOGGER.error("Unrecoverable Error, going down", t);
        } finally {
            try {
                t.printStackTrace();
            } finally {
                System.exit(exitCode);
            }
        }
    }
    public static final int PID;
    public static final String OS_NAME;
    public static final String PROCESS_NAME;

    static {
        PROCESS_NAME = ManagementFactory.getRuntimeMXBean().getName();
        int atIdx = PROCESS_NAME.indexOf('@');
        if (atIdx < 0) {
            PID = -1;
        } else {
            PID = Integer.valueOf(PROCESS_NAME.substring(0, atIdx));
        }
        OS_NAME = System.getProperty("os.name");
    }
    public static final String MAC_OS_X_OS_NAME = "Mac OS X";

    private static final File  FD_FOLDER = new File("/proc/" + PID + "/fd");
    
    public static int getNrOpenFiles() throws IOException {
        if (OS_NAME.equals(MAC_OS_X_OS_NAME)) {
            LineCountCharHandler handler = new LineCountCharHandler();
            run("/usr/sbin/lsof -p " + PID, handler);
            return handler.getLineCount() - 1;
        } else {
            if (FD_FOLDER.exists()) {
                return FD_FOLDER.list().length;
            } else {
                return -1;
            }
        }
    }

    @Nullable
    @edu.umd.cs.findbugs.annotations.SuppressWarnings
    public static String getLsofOutput() throws IOException {
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

    public interface CharHandler {

        void handle(int character);
    }

    public static void run(final String command, final CharHandler handler) throws IOException {
        Process proc = java.lang.Runtime.getRuntime().exec(command);
        InputStream is = proc.getInputStream();
        try {
            int c;
            while ((c = is.read()) >= 0) {
                handler.handle(c);
            }
        } finally {
            is.close();
        }
    }

    private static class LineCountCharHandler implements CharHandler {

        public LineCountCharHandler() {
            lineCount = 0;
        }
        private int lineCount;

        @Override
        public void handle(final int c) {
            if (c == '\n') {
                lineCount++;
            }
        }

        public int getLineCount() {
            return lineCount;
        }
    }

    private static class StringBuilderCharHandler implements CharHandler {

        public StringBuilderCharHandler() {
            builder = new StringBuilder();
        }
        private StringBuilder builder;

        @Override
        public void handle(final int c) {
            builder.append((char) c);
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }
}
