/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.base;


import java.io.IOException;
import java.util.List;
import java.util.Properties;
import javax.annotation.Nonnull;

/**
 *
 * @author zoly
 */
public interface ReportGenerator {
    
    
    @Nonnull
    List<String> generate(Properties props) throws IOException;
    
    @Nonnull
    List<String> getParameters();
    
}
