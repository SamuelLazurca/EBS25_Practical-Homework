package org.example;

import org.example.schema.SchemaField;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Publication {
    public HashMap<SchemaField, String> fields = new HashMap<>();

    public void addField(SchemaField field, String value) {
        fields.put(field, value);
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        Iterator<Map.Entry<SchemaField, String>> it = fields.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<SchemaField, String> entry = it.next();
            SchemaField field = entry.getKey();
            String value = entry.getValue();
            sb.append(" \"")
                    .append(field.field().toString().toLowerCase())
                    .append("\": \"")
                    .append(value)
                    .append("\"");
            if (it.hasNext()) {
                sb.append(",");
            }
        }
        sb.append(" }");
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (SchemaField field : fields.keySet()) {
            sb.append(" (").append(field.field().toString().toLowerCase()).append(",").append(fields.get(field)).append(")\n");
        }
        sb.append("}\n");
        return sb.toString();
    }
}
