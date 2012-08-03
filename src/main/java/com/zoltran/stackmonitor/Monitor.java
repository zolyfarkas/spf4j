/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.stackmonitor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import javax.management.*;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * @author zoly
 */
public class Monitor {


    private static class Options {

        @Option(name = "-f", usage = "output to this file the perf report")
        private String reportOut;
        @Option(name = "-main", usage = "the main class name", required=true)
        private String mainClass;
        @Option(name = "-si", usage = "the stack sampling interval")
        private int sampleInterval = 100;
        @Option(name = "-w", usage = "flame chart width")
        private int chartWidth = 2000;
        @Option(name = "-ss", usage = "start the stack sampler")
        private boolean startSampler = false;
        @Option(name = "-simple", usage = "start the stack sampler with simple stack sampling")
        private boolean simpleCollector = false;        
        
    }

    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException, CmdLineException, MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, InterruptedException {

        int sepPos = args.length;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--")) {
                sepPos = i;
                break;
            }
        }
        String[] newArgs ;
        String[] ownArgs ;
        if (sepPos == args.length) {
           newArgs = new String[0];
           ownArgs = args;
        } else {
            newArgs = new String[args.length - sepPos-1];
            ownArgs = new String[sepPos];
            System.arraycopy(args, sepPos+1, newArgs, 0, newArgs.length);
            System.arraycopy(args, 0, ownArgs, 0, sepPos);            
        }
        Options options = new Options();
        CmdLineParser parser = new CmdLineParser(options);
        parser.parseArgument(ownArgs);

        Sampler sampler; 
        if (options.simpleCollector) {
            sampler = new Sampler(options.sampleInterval, new SimpleStackCollector());
        } else {       
            sampler = new Sampler(options.sampleInterval);
        }
        sampler.registerJmx();
        try {
            if (options.startSampler)
                sampler.start();
            Class.forName(options.mainClass).getMethod("main", String[].class).invoke(null, (Object) newArgs);
        } finally {
            sampler.generateHtmlMonitorReport(options.reportOut, options.chartWidth);
            sampler.dispose();
        }

    }
}
