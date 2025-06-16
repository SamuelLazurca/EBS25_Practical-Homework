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
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Main {
    public static void main(String[] args) throws Exception {
        // Load configuration
        String configPath = args.length > 0 ? args[0] : "/config.properties";
        Properties props = new Properties();
        try (InputStream fis = Main.class.getResourceAsStream(configPath)) {
            if (fis == null) {
                throw new IOException("Configuration file not found: " + configPath);
            }
            props.load(fis);
        } catch (IOException e) {
            System.err.println("Error loading configuration: " + e.getMessage());
            return;
        }

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

        // Parse basic numeric settings
        int numberOfSubscriptions  = Integer.parseInt(props.getProperty("numberOfSubscriptions", "10"));
        int numberOfPublications   = Integer.parseInt(props.getProperty("numberOfPublications", "10"));
        int numberOfThreads        = Integer.parseInt(props.getProperty("numberOfThreads", "4"));
        double avgFieldProbability = Double.parseDouble(props.getProperty("probability.avgField", "0.0"));

        // OS stats
        OperatingSystemMXBean osBean =
                (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        int availableProcessors = osBean.getAvailableProcessors();

        System.out.println("\n=== System & CPU Stats ===");
        System.out.println("OS Name:         " + System.getProperty("os.name"));
        System.out.println("OS Version:      " + System.getProperty("os.version"));
        System.out.println("Architecture:    " + System.getProperty("os.arch"));
        System.out.println("Available Cores: " + availableProcessors);

        // Build frequency maps from config
        Map<SchemaField, Double> fieldsFrequency = new HashMap<>();
        Map<SchemaField, Double> equalOpFrequency = new HashMap<>();
        double defaultFieldFreq = Double.parseDouble(props.getProperty("default.fieldsFrequency", "50.0"));
        double defaultEqFreq    = Double.parseDouble(props.getProperty("default.equalOpFrequency", "50.0"));

        for (SchemaField field : schema.fields) {
            String keyF = "fieldsFrequency." + field.field().toString().toUpperCase();
            String keyE = "equalOpFrequency." + field.field().toString().toUpperCase();
            double fFreq = Double.parseDouble(props.getProperty(keyF, String.valueOf(defaultFieldFreq)));
            double eFreq = Double.parseDouble(props.getProperty(keyE, String.valueOf(defaultEqFreq)));
            fieldsFrequency.put(field, fFreq);
            equalOpFrequency.put(field, eFreq);
        }

        // SINGLE-THREADED SUBSCRIPTIONS
        long start = System.nanoTime();
        getSubscriptions(schema, props, fieldsFrequency, equalOpFrequency,
                numberOfSubscriptions, avgFieldProbability);
        long end = System.nanoTime();

        System.out.println("\n\nSUBSCRIPTIONS GENERATION");
        System.out.println("==========================");
        System.out.println("\nSingle-threaded execution\n");
        System.out.println("** Duration: " + (end - start) / 1_000_000 + " ms\n");

        // PARALLEL SUBSCRIPTIONS
        System.out.println("\nMulti-threaded execution");

        start = System.nanoTime();
        getSubscriptionsGeneratedInParallel(schema, props, fieldsFrequency, equalOpFrequency,
                numberOfSubscriptions, numberOfThreads, avgFieldProbability);
        end = System.nanoTime();

        System.out.println("\n** Number of threads: " + numberOfThreads);
        System.out.println("** Duration " + (end - start) / 1_000_000 + " ms\n");

        // SINGLE-THREADED PUBLICATIONS
        System.out.println("\nPUBLICATIONS GENERATION");
        System.out.println("==========================");

        start = System.nanoTime();
        getPublications(schema, props, numberOfPublications);
        end = System.nanoTime();

        System.out.println("\nSingle-threaded execution\n");
        System.out.println("** Duration: " + (end - start) / 1_000_000 + " ms");

        // PARALLEL PUBLICATIONS
        System.out.println("\nMulti-threaded execution");

        start = System.nanoTime();
        getPublicationsGeneratedInParallel(schema, props, numberOfPublications, numberOfThreads);
        end = System.nanoTime();

        System.out.println("\n** Number of threads: " + numberOfThreads);
        System.out.println("** Duration " + (end - start) / 1_000_000 + " ms\n");
    }

    private static void getSubscriptions(Schema schema, Properties props,
                                         Map<SchemaField, Double> fieldsFrequency,
                                         Map<SchemaField, Double> equalOpFrequency,
                                         int numberOfSubscriptions, double avgFieldProbability) throws Exception {
        SubscriptionSaver subSaverSingle = new TextFileSubscriptionSaver(
                props.getProperty("output.subscriptions.single", "output/subscriptions_single_thread.json")
        );
        SubscriptionsGenerator subGen = new SubscriptionsGenerator(
                schema,
                fieldsFrequency,
                equalOpFrequency,
                numberOfSubscriptions,
                avgFieldProbability
        );
        subGen.setSubscriptionSaver(subSaverSingle);
        subGen.generateSubscriptions();
        subSaverSingle.close();
    }

    private static void getSubscriptionsGeneratedInParallel(Schema schema, Properties props,
                                                            Map<SchemaField, Double> fieldsFrequency,
                                                            Map<SchemaField, Double> equalOpFrequency,
                                                            int numberOfSubscriptions, int numberOfThreads,
                                                            double avgFieldProbability) throws Exception {
        SubscriptionSaver subSaverMulti = new TextFileSubscriptionSaver(
                props.getProperty("output.subscriptions.multi", "output/subscriptions_multi_thread.json")
        );
        ParallelSubscriptionsGenerator.generateSubscriptionsMultiThreaded(
                schema,
                numberOfSubscriptions,
                numberOfThreads,
                fieldsFrequency,
                equalOpFrequency,
                avgFieldProbability,
                subSaverMulti
        );
        subSaverMulti.close();
    }

    private static void getPublications(Schema schema, Properties props,
                                        int numberOfPublications) throws IOException {
        PublicationSaver pubSaverSingle = new TextFilePublicationSaver(
                props.getProperty("output.publications.single", "output/publications_single_thread.json")
        );
        PublicationsGenerator pubGen = new PublicationsGenerator(schema, numberOfPublications);
        pubGen.setPublicationSaver(pubSaverSingle);
        pubGen.generatePublications();
        pubSaverSingle.close();
    }

    private static void getPublicationsGeneratedInParallel(Schema schema, Properties props,
                                                           int numberOfPublications, int numberOfThreads) throws Exception {
        PublicationSaver pubSaverMulti = new TextFilePublicationSaver(
                props.getProperty("output.publications.multi", "output/publications_multi_thread.json")
        );
        ParallelPublicationsGenerator.generatePublicationsMultithreaded(
                schema,
                numberOfThreads,
                numberOfPublications,
                pubSaverMulti
        );
        pubSaverMulti.close();
    }
}
