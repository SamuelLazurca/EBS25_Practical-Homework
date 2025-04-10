package org.example;

import org.example.generators.ParallelPublicationsGenerator;
import org.example.generators.ParallelSubscriptionsGenerator;
import org.example.generators.PublicationsGenerator;
import org.example.generators.SubscriptionsGenerator;
import org.example.schema.Schema;
import org.example.schema.SchemaField;
import org.example.schema.SchemaFields;
import org.example.storage.PublicationSaver;
import org.example.storage.SubscriptionSaver;
import org.example.storage.TextFilePublicationSaver;
import org.example.storage.TextFileSubscriptionSaver;

import java.io.IOException;
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

        int numberOfSubscriptions = 1000;
        int numberOfPublications = 100;

        // SINGLE-THREADED SUBSCRIPTIONS
        long start = System.nanoTime();
        getSubscriptions(schema, numberOfSubscriptions);
        long end = System.nanoTime();
        System.out.println("Durata generării sincrone (subscriptii): " + (end - start) / 1_000_000 + " ms");

        // PARALLEL SUBSCRIPTIONS
        start = System.nanoTime();
        getSubscriptionsGeneratedInParallel(schema, numberOfSubscriptions);
        end = System.nanoTime();
        System.out.println("Durata generării paralele (subscriptii): " + (end - start) / 1_000_000 + " ms");

        // SINGLE-THREADED PUBLICATIONS
        start = System.nanoTime();
        getPublications(schema, numberOfPublications);
        end = System.nanoTime();
        System.out.println("Durata generării sincrone (publicatii): " + (end - start) / 1_000_000 + " ms");

        // PARALLEL PUBLICATIONS
        start = System.nanoTime();
        getPublicationsGeneratedInParallel(schema, numberOfPublications);
        end = System.nanoTime();
        System.out.println("Durata generării paralele (publicatii): " + (end - start) / 1_000_000 + " ms");
    }

    private static void getSubscriptions(Schema schema, int numberOfSubscriptions) throws Exception {
        Map<SchemaField, Double> fieldsFrequencyPercentage = new HashMap<>();
//        for (SchemaField field : schema.fields) {
//            fieldsFrequencyPercentage.put(field, 50.0);
//        }

        fieldsFrequencyPercentage.put(SchemaFields.CITY, 70.0);
        fieldsFrequencyPercentage.put(SchemaFields.WIND, 30.0);

        Map<SchemaField, Double> equalOperatorFrequency = new HashMap<>();
        for (SchemaField field : schema.fields) {
            equalOperatorFrequency.put(field, 50.0);
        }

        SubscriptionSaver saver = new TextFileSubscriptionSaver("output/subscriptions_single_thread.json");

        SubscriptionsGenerator generator = new SubscriptionsGenerator(
                schema,
                fieldsFrequencyPercentage,
                equalOperatorFrequency,
                numberOfSubscriptions
        );
        // inject the saver
        generator.setSubscriptionSaver(saver);
        generator.generateSubscriptions();

        try {
            saver.close();
        } catch (IOException e) {
            System.err.println("Error closing saver: " + e.getMessage());
        }
    }

    private static void getSubscriptionsGeneratedInParallel(Schema schema, int numberOfSubscriptions) throws Exception {
        Map<SchemaField, Double> fieldsFrequencyPercentage = new HashMap<>();
//        for (SchemaField field : schema.fields) {
//            fieldsFrequencyPercentage.put(field, 20.0);
//        }

        fieldsFrequencyPercentage.put(SchemaFields.CITY, 30.0);
        fieldsFrequencyPercentage.put(SchemaFields.WIND, 70.0);

        Map<SchemaField, Double> equalOperatorFrequency = new HashMap<>();
        for (SchemaField field : schema.fields) {
            equalOperatorFrequency.put(field, 50.0);
        }

        int threads = 10;

        SubscriptionSaver saver = new TextFileSubscriptionSaver("output/subscriptions_multi_thread.json");

        ParallelSubscriptionsGenerator.generateSubscriptionsMultiThreaded(
                schema,
                numberOfSubscriptions,
                threads,
                fieldsFrequencyPercentage,
                equalOperatorFrequency,
                saver
        );

        try {
            saver.close();
        } catch (IOException e) {
            System.err.println("Error closing saver: " + e.getMessage());
        }
    }

    private static void getPublications(Schema schema, int numberOfPublications) throws IOException {
        PublicationSaver saver = new TextFilePublicationSaver("output/publications_single_thread.json");
        PublicationsGenerator generator = new PublicationsGenerator(schema, numberOfPublications);
        generator.setPublicationSaver(saver);
        generator.generatePublications();

        try {
            saver.close();
        } catch (IOException e) {
            System.err.println("Error closing saver: " + e.getMessage());
        }
    }

    private static void getPublicationsGeneratedInParallel(Schema schema, int numberOfPublications) throws Exception {
        int threads = 4;
        PublicationSaver saver = new TextFilePublicationSaver("output/publications_multi_thread.json");
        ParallelPublicationsGenerator.generatePublicationsMultithreaded(
                schema,
                threads,
                numberOfPublications,
                saver
        );

        try {
            saver.close();
        } catch (IOException e) {
            System.err.println("Error closing saver: " + e.getMessage());
        }
    }
}
