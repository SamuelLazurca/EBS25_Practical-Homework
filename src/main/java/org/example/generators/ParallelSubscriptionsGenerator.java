package org.example.generators;

import org.example.schema.Schema;
import org.example.schema.SchemaField;

import java.util.*;
import java.util.concurrent.*;

public class ParallelSubscriptionsGenerator {
    public static void generateSubscriptionsMultiThreaded (
            Schema schema,
            int totalSubscriptions,
            int numberOfThreads,
            Map<SchemaField, Double> fieldsFrequencyPercentage,
            Map<SchemaField, Integer> equalOperatorsRequiredFrequency
    ) throws Exception {

        long startTime = System.nanoTime();

        boolean allFieldsHaveFrequencyRestrictions = schema.fields.size() == fieldsFrequencyPercentage.size();

        int totalFieldsCount = 0;

        Map<SchemaField, Integer> globalTargetFieldFrequencies = new ConcurrentHashMap<>();
        for (SchemaField field : schema.fields) {
            Double pct = fieldsFrequencyPercentage.get(field);
            if (pct != null) {
                int freq = (int) Math.round(pct * totalSubscriptions / 100.0);
                globalTargetFieldFrequencies.put(field, freq);
                totalFieldsCount += freq;
            }
        }

        if (allFieldsHaveFrequencyRestrictions && totalFieldsCount < totalSubscriptions) {
            throw new Exception("Total frequency < 100% for multi-thread usage");
        }

        List<Map<SchemaField, Integer>> threadTargetFieldsFrequencies = new ArrayList<>();
        for (int i = 0; i < numberOfThreads; i++) {
            threadTargetFieldsFrequencies.add(new ConcurrentHashMap<>());
        }

        int[] numberOfSubsPerThread = new int[numberOfThreads];

        int chunkSize = totalSubscriptions / numberOfThreads;
        int remainderSize = totalSubscriptions % numberOfThreads;

        for (int i = 0; i < numberOfThreads; i++) {
            numberOfSubsPerThread[i] = chunkSize + (i < remainderSize ? 1 : 0);
        }

        for (Map.Entry<SchemaField, Integer> entry : globalTargetFieldFrequencies.entrySet()) {
            SchemaField field = entry.getKey();
            int fieldCount = entry.getValue();

            int threadChunkSize = fieldCount / numberOfThreads;
            int threadRemainderSize = fieldCount % numberOfThreads;

            for (int i = 0; i < numberOfThreads; i++) {
                int count = threadChunkSize + (i < threadRemainderSize ? 1 : 0);
                threadTargetFieldsFrequencies.get(i).put(field, count);
            }
        }

        int[] countOfFieldsPerThread = new int[numberOfThreads];

        if (allFieldsHaveFrequencyRestrictions) {
            List<Integer> threadsThatNeedMoreFields = new ArrayList<>();
            List<Integer> threadsThatCanDonate = new ArrayList<>();

            for (int i = 0; i < numberOfThreads; i++) {
                countOfFieldsPerThread[i] = threadTargetFieldsFrequencies.get(i)
                        .values()
                        .stream()
                        .mapToInt(Integer::intValue)
                        .sum();

                if (countOfFieldsPerThread[i] < numberOfSubsPerThread[i]) {
                    threadsThatNeedMoreFields.add(i);
                } else if (countOfFieldsPerThread[i] > numberOfSubsPerThread[i]) {
                    threadsThatCanDonate.add(i);
                }
            }

            for (int i : threadsThatNeedMoreFields) {
                int needed = numberOfSubsPerThread[i] - countOfFieldsPerThread[i];
                if (needed <= 0) continue;

                for (int j : threadsThatCanDonate) {
                    if (needed == 0) break;

                    int excess = countOfFieldsPerThread[j] - numberOfSubsPerThread[j];
                    if (excess <= 0) continue;

                    Map<SchemaField, Integer> fromThreadFields = threadTargetFieldsFrequencies.get(j);
                    Map<SchemaField, Integer> toThreadFields = threadTargetFieldsFrequencies.get(i);

                    for (SchemaField field : new HashMap<>(fromThreadFields).keySet()) {
                        var fieldCount = fromThreadFields.get(field);

                        if (fieldCount <= 0) continue;

                        var transferable = Math.min(fieldCount, excess);
                        var transferred = Math.min(transferable, needed);

                        fromThreadFields.put(field, fieldCount - transferred);
                        toThreadFields.put(field, toThreadFields.getOrDefault(field, 0) + transferred);

                        countOfFieldsPerThread[j] -= transferred;
                        countOfFieldsPerThread[i] += transferred;

                        needed -= transferred;
                        excess -= transferred;

                        if (needed == 0 || excess == 0) break;
                    }
                }

                if (needed > 0) {
                    throw new IllegalStateException("Nu s-a putut redistribui suficient pentru thread-ul  " + i);
                }
            }
        }

        int endTime = (int) ((System.nanoTime() - startTime) / 1_000_000);

        System.out.println("Durata pre-procesarii " + endTime + " ms");

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            int numberOfSubsToGenerate = numberOfSubsPerThread[i];

            SubscriptionsGenerator localGen = new SubscriptionsGenerator(
                    schema,
                    numberOfSubsToGenerate,
                    threadTargetFieldsFrequencies.get(i),
                    equalOperatorsRequiredFrequency,
                    allFieldsHaveFrequencyRestrictions,
                    countOfFieldsPerThread[i]
            );

            executor.execute(localGen::generateSubscriptions);
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
    }
}
