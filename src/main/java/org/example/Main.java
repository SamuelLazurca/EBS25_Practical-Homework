package org.example;

import org.example.generators.ParallelPublicationsGenerator;
import org.example.generators.ParallelSubscriptionsGenerator;
import org.example.generators.PublicationsGenerator;
import org.example.generators.SubscriptionsGenerator;
import org.example.schema.Schema;
import org.example.schema.SchemaField;
import org.example.schema.SchemaFields;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {
        Schema schema = new Schema(
                List.of(
                        SchemaFields.STATION,
                        SchemaFields.CITY,
                        SchemaFields.TEMP,
                        SchemaFields.RAIN,
                        SchemaFields.WIND,
                        SchemaFields.DIRECTION,
                        SchemaFields.DATE
                )
        );

        int numberOfSubscriptions = 100000;

        long start = System.nanoTime();
        getSubscriptions(schema, numberOfSubscriptions);
        long end = System.nanoTime();
        System.out.println("Durata generării sincrone: " + (end - start) / 1_000_000 + " ms");

        start = System.nanoTime();
        getSubscriptionsGeneratedInParallel(schema, numberOfSubscriptions);
        end = System.nanoTime();
        System.out.println("Durata generării paralele: " + (end - start) / 1_000_000 + " ms");
    }

    private static void getSubscriptions(Schema schema, int numberOfSubscriptions) throws Exception {
        Map<SchemaField, Double> fieldsFrequencyPercentage = new HashMap<>();
        for (SchemaField field : schema.fields) {
            fieldsFrequencyPercentage.put(field, 0.0);
        }

        fieldsFrequencyPercentage.put(SchemaFields.CITY, 70.0);
        fieldsFrequencyPercentage.put(SchemaFields.WIND, 30.0);

        Map<SchemaField, Double> equalOperatorFrequency = new HashMap<>();
        for (SchemaField field : schema.fields) {
            equalOperatorFrequency.put(field, 50.0);
        }

        SubscriptionsGenerator generator = new SubscriptionsGenerator(
                schema,
                fieldsFrequencyPercentage,
                equalOperatorFrequency,
                numberOfSubscriptions
        );

        generator.generateSubscriptions();
    }

    private static void getSubscriptionsGeneratedInParallel(Schema schema, int numberOfSubscriptions) throws Exception {
        Map<SchemaField, Double> fieldsFrequencyPercentage = new HashMap<>();
        for (SchemaField field : schema.fields) {
            fieldsFrequencyPercentage.put(field, 0.0);
        }

        fieldsFrequencyPercentage.put(SchemaFields.CITY, 70.0);
        fieldsFrequencyPercentage.put(SchemaFields.WIND, 30.0);

        Map<SchemaField, Double> equalOperatorFrequency = new HashMap<>();
        for (SchemaField field : schema.fields) {
            equalOperatorFrequency.put(field, 50.0);
        }

        int threads = 4;

        ParallelSubscriptionsGenerator.generateSubscriptionsMultiThreaded(
                schema,
                numberOfSubscriptions,
                threads,
                fieldsFrequencyPercentage,
                equalOperatorFrequency
        );
    }
}
