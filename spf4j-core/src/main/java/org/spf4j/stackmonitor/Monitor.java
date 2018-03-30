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
package org.spf4j.stackmonitor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.base.SysExits;
import static org.spf4j.stackmonitor.Sampler.DEFAULT_SS_DUMP_FILE_NAME_PREFIX;
import static org.spf4j.stackmonitor.Sampler.DEFAULT_SS_DUMP_FOLDER;

/**
 * @author zoly
 */
public final class Monitor {

    private static class Options {

        @Option(name = "-df", usage = "dump folder")
        private String dumpFolder = DEFAULT_SS_DUMP_FOLDER;

        @Option(name = "-dp", usage = "dump file prefix")
        private String dumpFilePrefix = DEFAULT_SS_DUMP_FILE_NAME_PREFIX;

        @Option(name = "-main", usage = "the main class name", required = true)
        private String mainClass;

        @Option(name = "-si", usage = "the stack sampling interval in milliseconds")
        private int sampleInterval = 100;

        @Option(name = "-di", usage = "the stack dump to file interval in milliseconds")
        private int dumpInterval = 3600000;

        @Option(name = "-ss", usage = "start the stack sampling. (can also be done manually via jmx)")
        private boolean startSampler = false;


    }

    private Monitor() { }

    @SuppressWarnings("checkstyle:regexp")
    public static void main(final String[] args)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException  {

        int sepPos = args.length;
        for (int i = 0; i < args.length; i++) {
            if ("--".equals(args[i])) {
                sepPos = i;
                break;
            }
        }
        String[] newArgs;
        String[] ownArgs;
        if (sepPos == args.length) {
            newArgs = new String[0];
            ownArgs = args;
        } else {
            newArgs = new String[args.length - sepPos - 1];
            ownArgs = new String[sepPos];
            System.arraycopy(args, sepPos + 1, newArgs, 0, newArgs.length);
            System.arraycopy(args, 0, ownArgs, 0, sepPos);
        }
        Options options = new Options();
        CmdLineParser parser = new CmdLineParser(options);
        try {
            parser.parseArgument(ownArgs);
        } catch (CmdLineException e) {
            System.err.println("Error: " + e.getMessage() + "\nUsage:");
            parser.printUsage(System.err);
            System.exit(SysExits.EX_USAGE.exitCode());
        }

        final Sampler sampler = new Sampler(options.sampleInterval, options.dumpInterval,
                (t) -> new FastStackCollector(false, true, new Thread[]{t}),
                options.dumpFolder, options.dumpFilePrefix);
        Runtime.getRuntime().addShutdownHook(new Thread(new AbstractRunnable() {

            @Override
            public void doRun() throws InterruptedException, IOException {
                sampler.stop();
                sampler.dumpToFile();
                sampler.dispose();
            }

        }, "Sampling report"));
        sampler.registerJmx();

        if (options.startSampler) {
            sampler.start();
        }
        Class.forName(options.mainClass).getMethod("main", String[].class).invoke(null, (Object) newArgs);
        System.exit(SysExits.OK.exitCode());
    }


}
