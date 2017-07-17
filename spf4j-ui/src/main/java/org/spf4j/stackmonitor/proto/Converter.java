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
package org.spf4j.stackmonitor.proto;

import gnu.trove.map.TMap;
import gnu.trove.map.hash.THashMap;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.spf4j.base.Method;
import org.spf4j.stackmonitor.SampleNode;
import org.spf4j.stackmonitor.proto.gen.ProtoSampleNodes;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 *
 * @author zoly
 */
public final class Converter {

    private Converter() { }


    public static void saveToFile(@Nonnull final File file, @Nonnull final SampleNode input) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(
                Files.newOutputStream(file.toPath()))) {
            fromSampleNodeToProto(input).writeTo(bos);
        }
    }

    public static ProtoSampleNodes.Method fromMethodToProto(final Method m) {
        return ProtoSampleNodes.Method.newBuilder().setMethodName(m.getMethodName())
                .setDeclaringClass(m.getDeclaringClass())
                .build();
    }

    public static ProtoSampleNodes.SampleNode fromSampleNodeToProto(final SampleNode node) {

        ProtoSampleNodes.SampleNode.Builder resultBuilder
                = ProtoSampleNodes.SampleNode.newBuilder().setCount(node.getSampleCount());

        Map<Method, SampleNode> subNodes = node.getSubNodes();
        if (subNodes != null) {
           for (Map.Entry<Method, SampleNode> entry : subNodes.entrySet()) {
               resultBuilder.addSubNodes(
               ProtoSampleNodes.SamplePair.newBuilder().setMethod(fromMethodToProto(entry.getKey())).
                        setNode(fromSampleNodeToProto(entry.getValue())).build());
           }
        }
        return resultBuilder.build();
    }



    public static SampleNode  fromProtoToSampleNode(final ProtoSampleNodes.SampleNodeOrBuilder node) {

        TMap<Method, SampleNode> subNodes = null;

        List<ProtoSampleNodes.SamplePair> sns =  node.getSubNodesList();
        if (sns != null) {
            subNodes = new THashMap<>();
            for (ProtoSampleNodes.SamplePair pair : sns) {
                final ProtoSampleNodes.Method method = pair.getMethod();
                subNodes.put(new Method(method.getDeclaringClass(),
                        method.getMethodName()),
                        fromProtoToSampleNode(pair.getNode()));
            }
        }
        return new SampleNode(node.getCount(), subNodes);


    }


}
