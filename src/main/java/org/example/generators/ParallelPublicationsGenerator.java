package org.example.generators;

import org.example.schema.Schema;
import org.example.storage.PublicationSaver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ParallelPublicationsGenerator {
    public static void generatePublicationsMultithreaded(
            Schema schema,
            int numberOfThreads,
            int numberOfPublications,
            PublicationSaver publicationSaver
    ) {
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<Future<Long>> futures = new ArrayList<>();
        List<Integer> records = new ArrayList<>();

        int chunkSize = numberOfPublications / numberOfThreads;
        int reminder = numberOfPublications % numberOfThreads;

        for (int i = 0; i < numberOfThreads; i++) {
            PublicationsGenerator localGen = new PublicationsGenerator(schema, chunkSize + (i < reminder ? 1 : 0));
            localGen.setPublicationSaver(publicationSaver);

            records.add(chunkSize + (i < reminder ? 1 : 0));

            Future<Long> future = executor.submit(localGen::generatePublications);
            futures.add(future);
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

        List<Long> allStats = new ArrayList<>();
        for (Future<Long> future : futures) {
            try {
                allStats.add(future.get());
            } catch (Exception e) {
                System.err.println("Error in thread: " + e.getMessage());
            }
        }

        System.out.println("\n=== Per-Thread Statistics ===");
        System.out.printf("%-8s | %-10s | %-10s%n", "Thread", "Pubs", "Time (ms)");
        System.out.println("----------------------------------");

        for (int i = 0; i < allStats.size(); i++) {
            long timeMs = allStats.get(i);
            System.out.printf("%-8d | %-10d | %10d%n", i, records.get(i), timeMs);
        }
    }
}
