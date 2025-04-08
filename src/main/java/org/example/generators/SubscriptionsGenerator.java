package org.example.generators;

import org.example.Subscription;
import org.example.schema.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SubscriptionsGenerator {
    private final Schema schema;
    private final int maxCount;
    private final Map<SchemaField, Integer> fieldsRequiredFrequency;
    private final Map<SchemaField, Integer> fieldsCurrentFrequency;
    private final Map<SchemaField, Integer> equalOperatorsRequiredFrequency;
    private final int fieldsTotalMaxCount;
    private final boolean allFieldsHaveFrequencyRestrictions;
    private int currentCount;
    private int fieldsTotalCount;

    public SubscriptionsGenerator(Schema schema,
                                         int maxCount,
                                         Map<SchemaField, Double> fieldsFrequencyPercentage,
                                         Map<SchemaField, Integer> equalOperatorsRequiredFrequency) throws Exception {
        this.schema = schema;
        this.maxCount = maxCount;
        this.equalOperatorsRequiredFrequency = equalOperatorsRequiredFrequency;
        this.fieldsCurrentFrequency = new ConcurrentHashMap<>();
        this.fieldsRequiredFrequency = new ConcurrentHashMap<>();
        int totalMaxCount = 0;
        boolean restricted = true;

        for (SchemaField field : schema.fields) {
            fieldsCurrentFrequency.put(field, 0);
            Double pct = fieldsFrequencyPercentage.get(field);
            if (pct != null) {
                int freq = (int) (pct * maxCount / 100.0);
                fieldsRequiredFrequency.put(field, freq);
                totalMaxCount += freq;
            } else {
                restricted = false;
            }
        }
        if (restricted && totalMaxCount < maxCount) {
            throw new Exception("Total frequency < 100% for single-thread usage");
        }
        this.fieldsTotalMaxCount = totalMaxCount;
        this.allFieldsHaveFrequencyRestrictions = restricted;
    }

    private SubscriptionsGenerator(Schema schema,
                                          int maxCount,
                                          Map<SchemaField, Integer> fieldsRequiredFrequency,
                                          Map<SchemaField, Integer> equalOperatorsRequiredFrequency,
                                          int fieldsTotalMaxCount,
                                          boolean allFieldsHaveFrequencyRestrictions) {
        this.schema = schema;
        this.maxCount = maxCount;
        this.fieldsRequiredFrequency = fieldsRequiredFrequency;
        this.equalOperatorsRequiredFrequency = equalOperatorsRequiredFrequency;
        this.fieldsTotalMaxCount = fieldsTotalMaxCount;
        this.allFieldsHaveFrequencyRestrictions = allFieldsHaveFrequencyRestrictions;
        this.fieldsCurrentFrequency = new ConcurrentHashMap<>();
        for (SchemaField f : schema.fields) {
            this.fieldsCurrentFrequency.put(f, 0);
        }
    }

    public List<Subscription> generateSubscriptions(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> generateSubscription())
                .collect(Collectors.toList());
    }

    public static List<Subscription> generateSubscriptionsMultiThreaded(Schema schema,
                                                                        int totalSubscriptions,
                                                                        int threads,
                                                                        Map<SchemaField, Double> fieldsFrequencyPercentage,
                                                                        Map<SchemaField, Integer> equalOperatorsRequiredFrequency
    ) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<List<Subscription>>> futures = new ArrayList<>();
        int chunk = totalSubscriptions / threads;
        int rest = totalSubscriptions % threads;
        List<Subscription> result = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threads; i++) {
            int toGenerate = chunk + (i < rest ? 1 : 0);
            Map<SchemaField, Integer> localFreq = new ConcurrentHashMap<>();
            int totalLocalMaxCount = 0;
            boolean allLocalRestricted = true;

            for (SchemaField field : schema.fields) {
                Double pct = fieldsFrequencyPercentage.get(field);
                if (pct != null) {
                    int freq = (int) (pct * toGenerate / 100.0);
                    localFreq.put(field, freq);
                    totalLocalMaxCount += freq;
                } else {
                    allLocalRestricted = false;
                }
            }

            if (allLocalRestricted && totalLocalMaxCount < toGenerate) {
                throw new Exception("Total frequency < 100% in partition");
            }

            SubscriptionsGenerator localGen = new SubscriptionsGenerator(
                    schema,
                    toGenerate,
                    localFreq,
                    equalOperatorsRequiredFrequency,
                    totalLocalMaxCount,
                    allLocalRestricted
            );
            futures.add(executor.submit(() -> localGen.generateSubscriptions(toGenerate)));
        }

        for (Future<List<Subscription>> f : futures) {
            try {
                result.addAll(f.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();
        return result;
    }

    private Subscription generateSubscription() {
        Subscription subscription = new Subscription();
        int subscriptionFieldsCount = 0;
        while (subscriptionFieldsCount == 0) {
            for (SchemaField field : schema.fields) {
                if (allFieldsHaveFrequencyRestrictions
                        && fieldsTotalMaxCount - fieldsCurrentFrequency.values().stream().mapToInt(Integer::intValue).sum()
                        < maxCount - currentCount) {
                    break;
                }
                Integer freqRequired = fieldsRequiredFrequency.get(field);
                if (freqRequired == null) {
                    if (Math.random() < 0.5) {
                        updateSubscription(subscription, field);
                        subscriptionFieldsCount++;
                    }
                    continue;
                }
                int dif = freqRequired - fieldsCurrentFrequency.get(field);
                if (dif > 0) {
                    if ((maxCount - currentCount) > dif) {
                        if (Math.random() < 0.5) {
                            updateSubscription(subscription, field);
                            subscriptionFieldsCount++;
                        }
                    } else {
                        updateSubscription(subscription, field);
                        subscriptionFieldsCount++;
                    }
                }
            }
        }
        currentCount++;
        return subscription;
    }

    private void updateSubscription(Subscription subscription, SchemaField field) {
        String value = switch (field.field()) {
            case Station -> String.valueOf(GeneratorsParams.stationLimit.getRandomValue());
            case City -> GeneratorsParams.cities.get((int) (Math.random() * GeneratorsParams.cities.size()));
            case Temp -> String.valueOf(GeneratorsParams.tempLimit.getRandomValue());
            case Rain -> String.valueOf(GeneratorsParams.rainLimit.getRandomValue());
            case Wind -> String.valueOf(GeneratorsParams.windLimit.getRandomValue());
            case Direction -> GeneratorsParams.directions.get((int) (Math.random() * GeneratorsParams.directions.size()));
            case Date -> GeneratorsParams.dateFormat.format(GeneratorsParams.dateLimit.getRandomValue());
        };
        Operator operator = Operator.values()[(int) (Math.random() * Operator.values().length)];
        subscription.addField(field, new SubscriptionValue(operator, value));
        fieldsCurrentFrequency.put(field, fieldsCurrentFrequency.get(field) + 1);
        fieldsTotalCount++;
    }
}
