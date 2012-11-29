/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.stackmonitor.proto;

import com.zoltran.stackmonitor.Method;
import com.zoltran.stackmonitor.SampleNode;
import com.zoltran.stackmonitor.proto.gen.ProtoSampleNodes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author zoly
 */
public class Converter {
    
    
    public static ProtoSampleNodes.Method fromMethodToProto(Method m) {
        return ProtoSampleNodes.Method.newBuilder().setMethodName(m.getMethodName())
                .setDeclaringClass(m.getDeclaringClass()).build();
    }
    
    public static ProtoSampleNodes.SampleNode fromSampleNodeToProto(SampleNode node) {
        
        ProtoSampleNodes.SampleNode.Builder resultBuilder = ProtoSampleNodes.SampleNode.newBuilder().setCount(node.getSampleCount()); 
        
        Map<Method,SampleNode> subNodes = node.getSubNodes();
        if (subNodes != null) {
           for(Map.Entry<Method,SampleNode> entry : subNodes.entrySet()) {
               resultBuilder.addSubNodes( 
               ProtoSampleNodes.SamplePair.newBuilder().setMethod(fromMethodToProto(entry.getKey())).
                        setNode(fromSampleNodeToProto(entry.getValue())).build());
           }
            
        }        
        return resultBuilder.build();
    }
    
    
    
    public static SampleNode  fromProtoToSampleNode(ProtoSampleNodes.SampleNode node) {
        
        Map<Method,SampleNode> subNodes = null;
        
        List<ProtoSampleNodes.SamplePair> sns =  node.getSubNodesList();
        if (sns != null) {
            subNodes = new HashMap<Method, SampleNode>();
            for ( ProtoSampleNodes.SamplePair pair : sns) {
                subNodes.put(new Method(pair.getMethod().getMethodName(), pair.getMethod().getDeclaringClass() ),
                        fromProtoToSampleNode(pair.getNode()) );
            }
        }
        return new SampleNode(node.getCount(), subNodes);
        
        
    }
    
    
}
