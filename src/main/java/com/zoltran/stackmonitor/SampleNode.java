/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.stackmonitor;

import com.google.common.base.Predicate;
import edu.umd.cs.findbugs.annotations.NonNull;
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
            if (subNodes == null) {
                subNodes = new HashMap();
            }
            else {
                subNode = subNodes.get(method);
            }
            if (subNode == null) {
                subNodes.put(method, new SampleNode(stackTrace, --from));
            }
            else {
                subNode.addSample(stackTrace, --from);
            }               
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
    
    public int height() {
        if (subNodes == null) {
            return 1;
        } else {
            int subHeight = 0;
            for (SampleNode node: subNodes.values()) {
                int nHeight = node.height();
                if (nHeight > subHeight) {
                    subHeight = nHeight;
                }
            }
            return subHeight +1;
        }
            
    }
    
    @Nullable
    public SampleNode filteredByLeaf(Predicate<Method> predicate) {
        
        int newCount = this.count;
        
        Map<Method, SampleNode> sns = null;
        if (this.subNodes != null) {
            for (Map.Entry<Method, SampleNode> entry:  this.subNodes.entrySet()) {
                Method method = entry.getKey();
                SampleNode sn = entry.getValue();
                if (predicate.apply(method) && sn.height() == 1)  {
                    newCount -= sn.getCount();
                } else {
                    if (sns == null) {
                        sns = new HashMap<Method, SampleNode>();
                    }
                    SampleNode sn2 = sn.filteredByLeaf(predicate);
                    if (sn2 == null) {
                        newCount -= sn.getCount();
                    } else {
                        newCount -= sn.getCount() - sn2.getCount();
                        sns.put(method, sn2);
                    }
                    
                }
            }
        }
        if (newCount == 0) {
            return null;
        } else {
            return new SampleNode(newCount, sns);
        }
    }
    
    
}
