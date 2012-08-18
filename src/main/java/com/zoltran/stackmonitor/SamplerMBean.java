/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.stackmonitor;

import com.zoltran.base.ReportGenerator;
import java.io.IOException;

/**
 *
 * @author zoly
 */
public interface SamplerMBean extends ReportGenerator{

    void generateHtmlMonitorReport(String fileName, int chartWidth, int maxDepth) throws IOException;  
    
    void generateSvgHtmlMonitorReport(String fileName, int chartWidth, int maxDepth) throws IOException;  
    
    void generateCpuSvg(String fileName, int chartWidth, int maxDepth) throws IOException;
    
    void generateWaitSvg(String fileName, int chartWidth, int maxDepth) throws IOException;

    void start();

    void stop() throws InterruptedException;
    
    void clear();

    long getSampleTimeMillis();
    
    void setSampleTimeMillis(long sampleTimeMillis) ;

    boolean isStopped();
    
}
