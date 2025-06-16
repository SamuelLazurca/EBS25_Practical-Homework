package org.example;

import org.example.generators.GeneratorsParams;
import org.example.schema.SchemaField;
import org.example.schema.SubscriptionValue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Subscription {
    public HashMap<SchemaField, SubscriptionValue> fields = new HashMap<>();

    public void addField(SchemaField field, SubscriptionValue value) {
        fields.put(field, value);
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"subscription\": [\n");
        Iterator<Map.Entry<SchemaField, SubscriptionValue>> it = fields.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<SchemaField, SubscriptionValue> entry = it.next();
            SchemaField field = entry.getKey();
            SubscriptionValue subVal = entry.getValue();
            sb.append("    {\"field\": \"")
                    .append(field.field().toString().toLowerCase())
                    .append("\", \"operator\": \"")
                    .append(GeneratorsParams.MapOperatorToString.get(subVal.operator().ordinal()))
                    .append("\", \"value\": \"")
                    .append(subVal.value())
                    .append("\", \"isAverage\": \"")
                    .append(subVal.isAverage())
                    .append("\"}");
            if (it.hasNext()) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ]\n}");
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (SchemaField field : fields.keySet()) {
            sb
                    .append(" (")
                    .append(field.field().toString().toLowerCase())
                    .append(",")
                    .append(GeneratorsParams.MapOperatorToString.get(fields.get(field).operator().ordinal()))
                    .append(",")
                    .append(fields.get(field).value())
                    .append(",")
                    .append(fields.get(field).isAverage())
                    .append("),\n");
        }
        sb.append("}\n");
        return sb.toString();
    }
}
