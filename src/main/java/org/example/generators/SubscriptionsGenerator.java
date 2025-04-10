package org.example.generators;

import org.example.Subscription;
import org.example.schema.Operator;
import org.example.schema.Schema;
import org.example.schema.SchemaField;
import org.example.schema.SubscriptionValue;
import org.example.storage.SubscriptionSaver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SubscriptionsGenerator {
    private final Schema schema;
    private final int numberOfSubscriptions;
    private final Map<SchemaField, Integer> fieldsRequiredFrequencies;
    private final Map<SchemaField, Integer> fieldsCurrentFrequencies;
    private final Map<SchemaField, Integer> equalRequiredFrequencies;
    private final Map<SchemaField, Integer> equalCurrentFrequencies;
    private final int fieldsTotalMaxCount;
    private int fieldsCurrentCount = 0;
    private final boolean allFieldsHaveFrequencyRestrictions;
    private int generatedSubscriptionsCount;
    private SubscriptionSaver subscriptionSaver;

    public SubscriptionsGenerator(
            Schema schema,
            Map<SchemaField, Double> fieldsFrequencyPercentages,
            Map<SchemaField, Double> equalFrequencyPercentages,
            int numberOfSubscriptions
    ) throws Exception {
        this.schema = schema;
        this.numberOfSubscriptions = numberOfSubscriptions;
        this.equalRequiredFrequencies = new HashMap<>();
        this.equalCurrentFrequencies = new HashMap<>();
        this.fieldsRequiredFrequencies = new HashMap<>();
        this.fieldsCurrentFrequencies = new HashMap<>();

        int fieldsCount = 0;
        boolean restricted = true;

        for (SchemaField field : schema.fields) {
            fieldsCurrentFrequencies.put(field, 0);
            Double fieldPercentage = fieldsFrequencyPercentages.get(field);
            if (fieldPercentage != null) {
                int fieldFreq = (int) (fieldPercentage * numberOfSubscriptions / 100.0);
                fieldsRequiredFrequencies.put(field, fieldFreq);
                fieldsCount += fieldFreq;
            } else {
                restricted = false;
            }
            // get the required number of subscriptions for this field
            int fieldReqFreq = fieldsRequiredFrequencies.get(field) == null ?
                    numberOfSubscriptions : fieldsRequiredFrequencies.get(field);

            equalCurrentFrequencies.put(field, 0);
            Double eqPercentage = equalFrequencyPercentages.get(field);
            if (eqPercentage != null) {
                int eqFreq = (int) (eqPercentage * fieldReqFreq / 100.0);
                equalRequiredFrequencies.put(field, eqFreq);
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
            Map<SchemaField, Integer> equalRequiredFrequencies,
            boolean allFieldsHaveFrequencyRestrictions,
            int fieldsTotalMaxCount
    ) throws Exception {
        this.schema = schema;
        this.numberOfSubscriptions = numberOfSubscriptions;
        this.fieldsRequiredFrequencies = fieldsRequiredFrequencies;
        this.equalRequiredFrequencies = equalRequiredFrequencies;
        this.fieldsCurrentFrequencies = new ConcurrentHashMap<>();
        this.equalCurrentFrequencies = new ConcurrentHashMap<>();
        this.fieldsTotalMaxCount = fieldsTotalMaxCount;
        this.allFieldsHaveFrequencyRestrictions = allFieldsHaveFrequencyRestrictions;

        for (SchemaField f : schema.fields) {
            this.fieldsCurrentFrequencies.put(f, 0);
            this.equalCurrentFrequencies.put(f, 0);

//            if (this.equalRequiredFrequencies.get(f) != null &&
//                    this.fieldsRequiredFrequencies.get(f) == null)
//            {
//                throw new Exception("Field " + f + " has equal operator frequency but no required frequency");
//            }
        }

        if (this.allFieldsHaveFrequencyRestrictions && this.fieldsTotalMaxCount < numberOfSubscriptions) {
            throw new Exception("Total frequency < 100% in partition");
        }
    }

    public void setSubscriptionSaver(SubscriptionSaver saver) {
        this.subscriptionSaver = saver;
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

        if (subscriptionSaver != null) {
            try {
                subscriptionSaver.save(subscription);
            } catch (IOException e) {
                System.err.println("Error saving subscription: " + e.getMessage());
            }
        } else {
            System.out.println(subscription);
        }
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

        fieldsCurrentFrequencies.put(field, fieldsCurrentFrequencies.get(field) + 1);
        fieldsCurrentCount++;

        if (operator == Operator.EQ) {
            equalCurrentFrequencies.put(field, equalCurrentFrequencies.get(field) + 1);
        }
    }

    private Operator generateOperator(SchemaField field) {
        if (equalRequiredFrequencies.get(field) != null) {
            // get the remaining number of subscriptions for this field
            int fieldReqFreq = fieldsRequiredFrequencies.get(field) == null ? numberOfSubscriptions : fieldsRequiredFrequencies.get(field);
            int remainingFieldFreq = fieldReqFreq - fieldsCurrentFrequencies.get(field);

            int remainingEqualFreq = equalRequiredFrequencies.get(field) - equalCurrentFrequencies.get(field);
            if (remainingEqualFreq > 0) {
                if (remainingFieldFreq == remainingEqualFreq) {
                    return Operator.EQ;
                }
            }
        }
        // 1. There was no equal operator frequency
        // 2. The minimum equal operator frequency is already reached
        // 3. There are more remaining subscriptions than equal operator frequency
        return Operator.values()[(int) (Math.random() * Operator.values().length)];
    }
}
