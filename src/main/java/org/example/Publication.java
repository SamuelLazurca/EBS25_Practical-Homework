package org.example;

import org.example.schema.SchemaField;

import java.util.HashMap;

public class Publication {
    public HashMap<SchemaField, String> fields = new HashMap<>();

    public void addField(SchemaField field, String value) {
        fields.put(field, value);
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
