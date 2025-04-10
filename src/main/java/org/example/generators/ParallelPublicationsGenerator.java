package org.example.generators;

import org.example.schema.Schema;
import org.example.storage.PublicationSaver;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ParallelPublicationsGenerator {
    public static void generatePublicationsMultithreaded(
            Schema schema,
            int numberOfThreads,
            int numberOfPublications,
            PublicationSaver publicationSaver
    ) {
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            PublicationsGenerator localGen = new PublicationsGenerator(schema, numberOfPublications / (numberOfThreads - i));
            localGen.setPublicationSaver(publicationSaver);
            executor.execute(localGen::generatePublications);
        }

        executor.shutdown();

        try {
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
