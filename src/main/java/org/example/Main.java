package org.example;

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
        List<Subscription> subscriptions = getSubscriptions(schema, numberOfSubscriptions);
        long end = System.nanoTime();
        System.out.println("Durata generării sincrone: " + (end - start) / 1_000_000 + " ms");

        start = System.nanoTime();
        List<Subscription> subscriptionsParallel = getSubscribersGeneratedInParallel(schema, numberOfSubscriptions);
        end = System.nanoTime();
        System.out.println("Durata generării paralele: " + (end - start) / 1_000_000 + " ms");
    }

    private static List<Subscription> getSubscriptions(Schema schema, int numberOfSubscriptions) throws Exception {
        Map<SchemaField, Double> fieldsFrequencyPercentage = new HashMap<>();
        for (SchemaField field : schema.fields) {
            fieldsFrequencyPercentage.put(field, 51.0);
        }
        fieldsFrequencyPercentage.put(SchemaFields.CITY, 90.0);

        Map<SchemaField, Double> equalFrequencyPercentage = new HashMap<>();
        for (SchemaField field : schema.fields) {
            equalFrequencyPercentage.put(field, 20.0);
        }

        SubscriptionsGenerator generator = new SubscriptionsGenerator(
                schema,
                numberOfSubscriptions,
                fieldsFrequencyPercentage,
                equalFrequencyPercentage
        );
        return generator.generateSubscriptions(numberOfSubscriptions);
    }

    private static List<Subscription> getSubscribersGeneratedInParallel(Schema schema, int numberOfSubscriptions) throws Exception {
        Map<SchemaField, Double> fieldsFrequencyPercentage = new HashMap<>();
        for (SchemaField field : schema.fields) {
            fieldsFrequencyPercentage.put(field, 51.0);
        }
        fieldsFrequencyPercentage.put(SchemaFields.CITY, 90.0);

        Map<SchemaField, Integer> equalFrequencyPercentage = new HashMap<>();
        for (SchemaField field : schema.fields) {
            equalFrequencyPercentage.put(field, 20);
        }

        int threads = 4;

        return SubscriptionsGenerator.generateSubscriptionsMultiThreaded(
                schema,
                numberOfSubscriptions,
                threads,
                fieldsFrequencyPercentage,
                equalFrequencyPercentage
        );
    }
}
