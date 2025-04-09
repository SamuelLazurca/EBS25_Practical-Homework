package org.example.generators;

import org.example.Subscription;
import org.example.schema.*;

import java.util.*;
import java.util.concurrent.*;

public class SubscriptionsGenerator {
    private final Schema schema;
    private final int numberOfSubscriptions;
    private final Map<SchemaField, Integer> fieldsRequiredFrequencies;
    private final Map<SchemaField, Integer> fieldsCurrentFrequencies;
    private final Map<SchemaField, Integer> equalOperatorsRequiredFrequencies;
    private final int fieldsTotalMaxCount;
    private int fieldsCurrentCount = 0;
    private final boolean allFieldsHaveFrequencyRestrictions;
    private int generatedSubscriptionsCount;

    public SubscriptionsGenerator(
            Schema schema,
            Map<SchemaField, Double> fieldsFrequencyPercentage,
            Map<SchemaField, Integer> equalOperatorsRequiredFrequencies,
            int numberOfSubscriptions
    ) throws Exception {
        this.schema = schema;
        this.numberOfSubscriptions = numberOfSubscriptions;
        this.equalOperatorsRequiredFrequencies = equalOperatorsRequiredFrequencies;
        this.fieldsCurrentFrequencies = new HashMap<>();
        this.fieldsRequiredFrequencies = new HashMap<>();

        int fieldsCount = 0;
        boolean restricted = true;

        for (SchemaField field : schema.fields) {
            fieldsCurrentFrequencies.put(field, 0);
            Double pct = fieldsFrequencyPercentage.get(field);
            if (pct != null) {
                int freq = (int) (pct * numberOfSubscriptions / 100.0);
                fieldsRequiredFrequencies.put(field, freq);
                fieldsCount += freq;
            } else {
                restricted = false;
            }
        }

        if (restricted && fieldsCount < numberOfSubscriptions) {
            throw new Exception("Total frequency < 100% for single-thread usage");
        }

        this.fieldsTotalMaxCount = fieldsCount;
        this.allFieldsHaveFrequencyRestrictions = restricted;
    }

    public SubscriptionsGenerator(
            Schema schema,
            int numberOfSubscriptions,
            Map<SchemaField, Integer> fieldsRequiredFrequencies,
            Map<SchemaField, Integer> equalOperatorsRequiredFrequencies,
            boolean allFieldsHaveFrequencyRestrictions,
            int fieldsTotalMaxCount
    ) throws Exception {

        this.schema = schema;
        this.numberOfSubscriptions = numberOfSubscriptions;
        this.fieldsRequiredFrequencies = fieldsRequiredFrequencies;
        this.equalOperatorsRequiredFrequencies = equalOperatorsRequiredFrequencies;
        this.fieldsCurrentFrequencies = new ConcurrentHashMap<>();

        for (SchemaField f : schema.fields) {
            this.fieldsCurrentFrequencies.put(f, 0);
        }

        this.fieldsTotalMaxCount = fieldsTotalMaxCount;
        this.allFieldsHaveFrequencyRestrictions = allFieldsHaveFrequencyRestrictions;

        if (this.allFieldsHaveFrequencyRestrictions && this.fieldsTotalMaxCount < numberOfSubscriptions) {
            throw new Exception("Total frequency < 100% in partition");
        }
    }

    public void generateSubscriptions() {
        long start = System.nanoTime();
        for (int i = 0; i < numberOfSubscriptions; i++)
        {
            generateSubscription();
        }
        long end = System.nanoTime();

        System.out.println("Durata unei generari " + (end - start) / 1_000_000 + " ms");
    }

    private void generateSubscription() {
        Subscription subscription = new Subscription();

        int subscriptionFieldsCount = 0;

        while (subscriptionFieldsCount == 0) {
            for (SchemaField field : schema.fields) {
                if (allFieldsHaveFrequencyRestrictions &&
                        fieldsTotalMaxCount - fieldsCurrentCount <
                                numberOfSubscriptions - generatedSubscriptionsCount) {
                    break;
                }

                Integer freqRequired = fieldsRequiredFrequencies.get(field);
                if (freqRequired == null) {
                    if (Math.random() < 0.5) {
                        updateSubscription(subscription, field);
                        subscriptionFieldsCount++;
                    }
                    continue;
                }

                int dif = freqRequired - fieldsCurrentFrequencies.get(field);
                if (dif > 0) {
                    if ((numberOfSubscriptions - generatedSubscriptionsCount) > dif) {
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

        generatedSubscriptionsCount++;

        // System.out.println(subscription);
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

        fieldsCurrentFrequencies.put(field, fieldsCurrentFrequencies.get(field) + 1);
        fieldsCurrentCount++;
    }
}
