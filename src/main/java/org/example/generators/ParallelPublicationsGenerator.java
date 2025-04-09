package org.example.generators;

import org.example.schema.Schema;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ParallelPublicationsGenerator {
    public static void generatePublicationsMultithreaded(Schema schema, int numberOfThreads, int numberOfPublications) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            PublicationsGenerator localGen = new PublicationsGenerator(schema, numberOfPublications / (numberOfThreads - i));
            executor.execute(localGen::generatePublications);
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
    }
}
