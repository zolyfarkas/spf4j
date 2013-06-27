
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author zoly
 */
public final class Runtime {
 
    private Runtime() { }
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Runtime.class);

    
    public static  void goDownWithError(final Throwable t, final int exitCode) {
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
    
    static {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        int atIdx = name.indexOf('@');
        if (atIdx < 0) {
            PID = -1;
        } else {
            PID = Integer.valueOf(name.substring(0, atIdx));
        }
        OS_NAME = System.getProperty("os.name");
    }
    
    public static final String MAC_OS_X_OS_NAME = "Mac OS X";
    
    public static int getNrOpenFiles() throws IOException {
        if (OS_NAME.equals(MAC_OS_X_OS_NAME)) {
            Process proc = java.lang.Runtime.getRuntime().exec("/usr/sbin/lsof -p " + PID);
            InputStream is = proc.getInputStream();
            int lineCount = 0;
            try {
              int c;
              while ((c = is.read()) >= 0) {
                if (c == '\n') {
                    lineCount++;
                }
              }
            } finally {
                is.close();
            }
            return lineCount;
        } else {
            File procFsFdFolder = new File("/proc/" + PID + "/fd");
            if (procFsFdFolder.exists()) {
                return procFsFdFolder.list().length;
            } else {
                return -1;
            }
        }
    }
    
}
