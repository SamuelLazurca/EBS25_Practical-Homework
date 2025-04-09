package org.example.generators;

import org.example.Subscription;
import org.example.schema.Operator;
import org.example.schema.Schema;
import org.example.schema.SchemaField;
import org.example.schema.SubscriptionValue;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SubscriptionsGenerator {
    private final Schema schema;
    private final int maxCount;
    private final Map<SchemaField, Integer> fieldsRequiredFrequency;
    private final Map<SchemaField, Integer> fieldsCurrentFrequency;
    private final Map<SchemaField, Integer> equalRequiredFrequency;
    private final Map<SchemaField, Integer> equalCurrentFrequency;
    private final int fieldsTotalMaxCount;
    private final boolean allFieldsHaveFrequencyRestrictions;
    private int currentCount;
    private int fieldsTotalCount;

    public SubscriptionsGenerator(Schema schema,
                                         int maxCount,
                                         Map<SchemaField, Double> fieldsFrequencyPercentage,
                                         Map<SchemaField, Double> equalFrequencyPercentage) throws Exception {
        this.schema = schema;
        this.maxCount = maxCount;
        this.equalRequiredFrequency = new ConcurrentHashMap<>();
        this.equalCurrentFrequency = new ConcurrentHashMap<>();
        this.fieldsRequiredFrequency = new ConcurrentHashMap<>();
        this.fieldsCurrentFrequency = new ConcurrentHashMap<>();
        this.currentCount = 0;
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
            // get the required number of subscriptions for this field
            int fieldReqFreq = fieldsRequiredFrequency.get(field) == null ? maxCount : fieldsRequiredFrequency.get(field);

            equalCurrentFrequency.put(field, 0);
            Double equalFreq = equalFrequencyPercentage.get(field);
            if (equalFreq != null) {
                int freq = (int) (equalFreq * fieldReqFreq / 100.0);
                equalRequiredFrequency.put(field, freq);
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
                                          Map<SchemaField, Integer> equalRequiredFrequency,
                                          int fieldsTotalMaxCount,
                                          boolean allFieldsHaveFrequencyRestrictions) {
        this.schema = schema;
        this.maxCount = maxCount;
        this.fieldsRequiredFrequency = fieldsRequiredFrequency;
        this.equalRequiredFrequency = equalRequiredFrequency;
        this.fieldsTotalMaxCount = fieldsTotalMaxCount;
        this.allFieldsHaveFrequencyRestrictions = allFieldsHaveFrequencyRestrictions;
        this.fieldsCurrentFrequency = new ConcurrentHashMap<>();
        this.equalCurrentFrequency = new ConcurrentHashMap<>();
        for (SchemaField f : schema.fields) {
            this.fieldsCurrentFrequency.put(f, 0);
            this.equalCurrentFrequency.put(f, 0);
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
                                                                        Map<SchemaField, Integer> equalFrequencyPercentage
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
                    equalFrequencyPercentage,
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

    private Operator generateOperator(SchemaField field) {
        if (equalRequiredFrequency.get(field) != null) {
            // get the remaining number of subscriptions for this field
            int fieldReqFreq = fieldsRequiredFrequency.get(field) == null ? maxCount : fieldsRequiredFrequency.get(field);
            int remainingFieldFreq = fieldReqFreq - fieldsCurrentFrequency.get(field);

            int remainingEqualFreq = equalRequiredFrequency.get(field) - equalCurrentFrequency.get(field);
            if (remainingEqualFreq > 0) {
                if (remainingFieldFreq == remainingEqualFreq) {
                    return Operator.EQ;
                }
                // if >, generate a random operator, which could include EQ
                // do nothing here, the random operator will be returned using the final return statement
            }
            else {
                // random operator that is not EQ
                List<Operator> nonEqOperators = Arrays.stream(Operator.values())
                        .filter(op -> op != Operator.EQ)
                        .toList();
                return nonEqOperators.get((int) (Math.random() * nonEqOperators.size()));
            }
        }
        return Operator.values()[(int) (Math.random() * Operator.values().length)];
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
        Operator operator = generateOperator(field);
        subscription.addField(field, new SubscriptionValue(operator, value));
        fieldsCurrentFrequency.put(field, fieldsCurrentFrequency.get(field) + 1);
        fieldsTotalCount++;

        if (operator == Operator.EQ) {
            equalCurrentFrequency.put(field, equalCurrentFrequency.get(field) + 1);
        }
    }
}
