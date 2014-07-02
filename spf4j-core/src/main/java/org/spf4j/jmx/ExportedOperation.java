
package org.spf4j.jmx;

import javax.management.MBeanParameterInfo;

/**
 *
 * @author zoly
 */
public interface ExportedOperation {
        
    String getName();
    
    String getDescription();
    
    Object invoke(Object [] parameters);
    
    MBeanParameterInfo [] getParameterInfos();
    
    Class<?> getReturnType();
}
