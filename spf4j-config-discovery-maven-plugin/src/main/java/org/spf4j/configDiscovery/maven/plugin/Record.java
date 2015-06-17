
package org.spf4j.configDiscovery.maven.plugin;

import java.util.Map;
import org.apache.avro.Schema;
import org.codehaus.jackson.JsonNode;

/**
 *
 * @author zoly
 */
public class Record {

    public static Schema buildRecords(final String recordName, final String namespace,
            final Map<String, Object> recordDef) {
        Schema result = Schema.createRecord(recordName, "generated", namespace, true);
        for (Map.Entry<String, Object> entry : recordDef.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();
            Schema fieldSchema;
            JsonNode defaultValue;
            if (value instanceof Map) {
                fieldSchema = buildRecords(recordName, namespace, (Map<String, Object>) value);
                defaultValue = null;
            } else {
                FieldInfo fi = (FieldInfo) value;
//                if (Integer.class == fi.getType()) {
//                    fieldSchema = Schema.create(Schema.Type.i)
//                }
            }
//            Schema.Field field = new Schema.Field(fieldName, fieldSchema, "generated", null);
        }
        return result;
    }

}
