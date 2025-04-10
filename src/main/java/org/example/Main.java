package org.example;

import com.sun.management.OperatingSystemMXBean;
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
import java.lang.management.ManagementFactory;
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

        int numberOfSubscriptions = 10;
        int numberOfPublications = 10;
        int numberOfThreads = 4;

        OperatingSystemMXBean osBean =
                (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        int availableProcessors = osBean.getAvailableProcessors();

        System.out.println("\n=== System & CPU Stats ===");
        System.out.println("OS Name:         " + System.getProperty("os.name"));
        System.out.println("OS Version:      " + System.getProperty("os.version"));
        System.out.println("Architecture:    " + System.getProperty("os.arch"));
        System.out.println("Available Cores: " + availableProcessors);

        // SINGLE-THREADED SUBSCRIPTIONS
        long start = System.nanoTime();
        getSubscriptions(schema, numberOfSubscriptions);
        long end = System.nanoTime();

        System.out.println("\n\nSUBSCRIPTIONS GENERATION");
        System.out.println("==========================");
        System.out.println("\nSingle-threaded execution\n");
        System.out.println("** Duration: " + (end - start) / 1_000_000 + " ms\n");

        // PARALLEL SUBSCRIPTIONS
        System.out.println("\nMulti-threaded execution");

        start = System.nanoTime();
        getSubscriptionsGeneratedInParallel(schema, numberOfSubscriptions, numberOfThreads);
        end = System.nanoTime();

        System.out.println("\n** Number of threads: " + numberOfThreads);
        System.out.println("** Duration " + (end - start) / 1_000_000 + " ms\n");

        // SINGLE-THREADED PUBLICATIONS
        System.out.println("\nPUBLICATIONS GENERATION");
        System.out.println("==========================");

        start = System.nanoTime();
        getPublications(schema, numberOfPublications);
        end = System.nanoTime();

        System.out.println("\nSingle-threaded execution\n");
        System.out.println("** Duration: " + (end - start) / 1_000_000 + " ms");

        // PARALLEL PUBLICATIONS
        System.out.println("\nMulti-threaded execution");

        start = System.nanoTime();
        getPublicationsGeneratedInParallel(schema, numberOfPublications, numberOfThreads);
        end = System.nanoTime();

        System.out.println("\n** Number of threads: " + numberOfThreads);
        System.out.println("** Duration " + (end - start) / 1_000_000 + " ms\n");
    }

    private static void getSubscriptions(Schema schema, int numberOfSubscriptions) throws Exception {
        Map<SchemaField, Double> fieldsFrequencyPercentage = new HashMap<>();
        for (SchemaField field : schema.fields) {
            fieldsFrequencyPercentage.put(field, 50.0);
        }

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

        generator.setSubscriptionSaver(saver);
        generator.generateSubscriptions();

        try {
            saver.close();
        } catch (IOException e) {
            System.err.println("Error closing saver: " + e.getMessage());
        }
    }

    private static void getSubscriptionsGeneratedInParallel(Schema schema, int numberOfSubscriptions, int threads) throws Exception {
        Map<SchemaField, Double> fieldsFrequencyPercentage = new HashMap<>();
        for (SchemaField field : schema.fields) {
            fieldsFrequencyPercentage.put(field, 50.0);
        }

        fieldsFrequencyPercentage.put(SchemaFields.CITY, 70.0);
        fieldsFrequencyPercentage.put(SchemaFields.WIND, 30.0);

        Map<SchemaField, Double> equalOperatorFrequency = new HashMap<>();
        for (SchemaField field : schema.fields) {
            equalOperatorFrequency.put(field, 50.0);
        }

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

    private static void getPublicationsGeneratedInParallel(Schema schema, int numberOfPublications, int threads) throws Exception {
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
