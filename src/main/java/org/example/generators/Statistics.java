package org.example.generators;

import org.example.schema.SchemaField;

import java.util.Map;

public record Statistics(Map<SchemaField, Integer> fieldsFrequencies,
                         Map<SchemaField, Integer> equalOperatorFrequencies, long totalTimeInMillis,
                         long totalRecords) {
}
