package org.example;

import org.example.schema.SchemaField;
import org.example.schema.SubscriptionValue;

import java.util.HashMap;

public class Subscription {
    public HashMap<SchemaField, SubscriptionValue> fields = new HashMap<>();

    public void addField(SchemaField field, SubscriptionValue value) {
        fields.put(field, value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (SchemaField field : fields.keySet()) {
            sb.append(" (").append(field.field().toString().toLowerCase()).append(",").append(fields.get(field).operator()).append(",").append(fields.get(field).value()).append("),\n");
        }
        sb.append("}\n");
        return sb.toString();
    }
}
