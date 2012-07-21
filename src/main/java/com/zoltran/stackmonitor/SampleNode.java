/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.stackmonitor;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author zoly
 */
@ParametersAreNonnullByDefault
public class SampleNode {
    
    private int count;
    private Map<Method, SampleNode> subNodes;
    
    public SampleNode(StackTraceElement[] stackTrace, int from) {
        count = 1;
        if (from >= 0) {
            subNodes = new HashMap();
            subNodes.put(new Method(stackTrace[from]), new SampleNode(stackTrace, --from));
        }
    }

    public SampleNode(int count, @Nullable Map<Method, SampleNode> subNodes) {
        this.count = count;
        this.subNodes = subNodes;
    }
    
    
    
    public void addSample(StackTraceElement[] stackTrace, int from) {
        count++;
        if (from >= 0) {
            Method method = new Method(stackTrace[from]);
            SampleNode subNode = null;
            if (subNodes == null)
                subNodes = new HashMap();
            else
                subNode = subNodes.get(method);
            if (subNode == null)
                subNodes.put(method, new SampleNode(stackTrace, --from));
            else
                subNode.addSample(stackTrace, --from);               
        }
    }

    public int getCount() {
        return count;
    }

    @Nullable
    public Map<Method, SampleNode> getSubNodes() {
        return subNodes;
    }

    @Override
    public String toString() {
        return "SampleNode{" + "count=" + count + ", subNodes=" + subNodes + '}';
    }
    
    
}
