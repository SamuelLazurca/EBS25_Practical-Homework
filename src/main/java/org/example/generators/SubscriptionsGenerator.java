package org.example.generators;

import org.example.Subscription;
import org.example.schema.*;
import org.example.storage.SubscriptionSaver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SubscriptionsGenerator {
    private final Schema schema;
    private final int targetNumberOfSubscriptions;
    private final Map<SchemaField, Integer> fieldsRequiredFrequencies;
    private final Map<SchemaField, Integer> fieldsCurrentFrequencies;
    private final Map<SchemaField, Integer> equalRequiredFrequencies;
    private final Map<SchemaField, Integer> equalCurrentFrequencies;
    private final Map<SchemaField, Double> equalRequiredPercentages;
    private final int fieldsTotalMaxCount;
    private int fieldsCurrentCount = 0;
    private final boolean allFieldsHaveFrequencyRestrictions;
    private int generatedSubscriptionsCount;
    private SubscriptionSaver subscriptionSaver;
    private final double avgFieldProbability;

    public SubscriptionsGenerator(
            Schema schema,
            Map<SchemaField, Double> fieldsFrequencyPercentages,
            Map<SchemaField, Double> equalFrequencyPercentages,
            int targetNumberOfSubscriptions, double avgFieldProbability
    ) throws Exception {
        this.schema = schema;
        this.targetNumberOfSubscriptions = targetNumberOfSubscriptions;

        this.equalRequiredFrequencies = new HashMap<>();
        this.equalCurrentFrequencies = new HashMap<>();

        // Used for computing the minimum equal
        // operator frequency for unrestricted fields
        this.equalRequiredPercentages = equalFrequencyPercentages;

        this.fieldsRequiredFrequencies = new HashMap<>();
        this.fieldsCurrentFrequencies = new HashMap<>();

        int fieldsCount = 0;
        boolean restricted = true;

        for (SchemaField field : schema.fields) {
            fieldsCurrentFrequencies.put(field, 0);
            equalCurrentFrequencies.put(field, 0);

            Double fieldPercentage = fieldsFrequencyPercentages.get(field);
            if (fieldPercentage != null) {
                int fieldFreq = (int) (fieldPercentage * targetNumberOfSubscriptions / 100.0);
                fieldsRequiredFrequencies.put(field, fieldFreq);
                fieldsCount += fieldFreq;

                Double eqPercentage = equalFrequencyPercentages.get(field);
                if (eqPercentage != null) {
                    int eqFreq = (int) (eqPercentage * fieldFreq / 100.0);
                    equalRequiredFrequencies.put(field, eqFreq);
                }
            } else {
                restricted = false;
            }
        }

        if (restricted && fieldsCount < targetNumberOfSubscriptions) {
            throw new Exception("Total frequency < 100% for single-thread usage");
        }

        this.fieldsTotalMaxCount = fieldsCount;
        this.allFieldsHaveFrequencyRestrictions = restricted;

        // Probability to determine if a field is avg_field in a subscription (complex)
        this.avgFieldProbability = avgFieldProbability;
    }

    public SubscriptionsGenerator(
            Schema schema,
            int targetNumberOfSubscriptions,
            Map<SchemaField, Integer> fieldsRequiredFrequencies,
            Map<SchemaField, Integer> equalRequiredFrequencies,
            Map<SchemaField, Double> equalFrequencyPercentages,
            boolean allFieldsHaveFrequencyRestrictions,
            int fieldsTotalMaxCount,
            double avgFieldProbability
    ) throws Exception {
        this.schema = schema;
        this.targetNumberOfSubscriptions = targetNumberOfSubscriptions;

        this.fieldsRequiredFrequencies = fieldsRequiredFrequencies;
        this.fieldsCurrentFrequencies = new HashMap<>();

        this.equalRequiredFrequencies = equalRequiredFrequencies;
        this.equalCurrentFrequencies = new HashMap<>();

        this.fieldsTotalMaxCount = fieldsTotalMaxCount;

        this.allFieldsHaveFrequencyRestrictions = allFieldsHaveFrequencyRestrictions;
        this.equalRequiredPercentages = equalFrequencyPercentages;

        for (SchemaField f : schema.fields) {
            this.fieldsCurrentFrequencies.put(f, 0);
            this.equalCurrentFrequencies.put(f, 0);
        }

        if (this.allFieldsHaveFrequencyRestrictions && this.fieldsTotalMaxCount < targetNumberOfSubscriptions) {
            throw new Exception("Total frequency < 100% in partition");
        }

        // Probability to determine if a field is avg_field in a subscription (complex)
        this.avgFieldProbability = avgFieldProbability;
    }

    public void setSubscriptionSaver(SubscriptionSaver saver) {
        this.subscriptionSaver = saver;
    }

    public Statistics generateSubscriptions() {
        long start = System.nanoTime();
        for (int i = 0; i < targetNumberOfSubscriptions; i++)
        {
            generateSubscription();
        }
        long end = System.nanoTime();

        return new Statistics (
                this.fieldsCurrentFrequencies,
                this.equalCurrentFrequencies,
                (end - start) / 1_000_000,
                this.generatedSubscriptionsCount
        );
    }

    private void generateSubscription() {
        Subscription subscription = new Subscription();

        int subscriptionFieldsCount = 0;

        while (subscriptionFieldsCount == 0) {
            for (SchemaField field : schema.fields) {
                // necessary to not generate empty subscriptions in the future
                if (allFieldsHaveFrequencyRestrictions &&
                        fieldsTotalMaxCount - fieldsCurrentCount <
                                targetNumberOfSubscriptions - generatedSubscriptionsCount) {
                    break;
                }

                // if the field has no required frequency
                Integer freqRequired = fieldsRequiredFrequencies.get(field);
                if (freqRequired == null) {
                    if (Math.random() < 0.5) {
                        updateSubscription(subscription, field);
                        subscriptionFieldsCount++;
                    }
                    continue;
                }

                // if the field has a required frequency
                int dif = freqRequired - fieldsCurrentFrequencies.get(field);
                if (dif > 0) {
                    if ((targetNumberOfSubscriptions - generatedSubscriptionsCount) > dif) {
                        // it's allowed to not generate a subscription for this field
                        if (Math.random() < 0.5) {
                            updateSubscription(subscription, field);
                            subscriptionFieldsCount++;
                        }
                    } else {
                        // must generate a subscription for this field
                        // so we can reach the target number of subscriptions
                        // before the end of the generation
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

        // Generate a random operator for the field
        // and determine if it is an avg_field
        Operator operator = generateOperator(field);
        boolean isAvgField = isAverageField(field);
        subscription.addField(field, new SubscriptionValue(operator, value, isAvgField));

        fieldsCurrentFrequencies.put(field, fieldsCurrentFrequencies.get(field) + 1);
        fieldsCurrentCount++;

        if (operator == Operator.EQ) {
            equalCurrentFrequencies.put(field, equalCurrentFrequencies.get(field) + 1);
        }
    }

    private Operator generateOperator(SchemaField field) {
        if (equalRequiredPercentages.get(field) != null) {
            if (fieldsRequiredFrequencies.get(field) == null) {
                // if the field has no required frequency (random number),
                // we have to compute the minimum eq req frequency every time
                // we generate a subscription for this field.
                // Then, if the minimum eq req frequency is not reached, we
                // return the EQ operator
                int eqReqFreq = (int) (equalRequiredPercentages.get(field) * (fieldsCurrentFrequencies.get(field) + 1) / 100.0);

                if (equalCurrentFrequencies.get(field) < eqReqFreq) {
                    return Operator.EQ;
                }

                return randomOperator(field);
            }

            // we know that the field has a required frequency
            int fieldReqFreq = fieldsRequiredFrequencies.get(field) == null ? targetNumberOfSubscriptions : fieldsRequiredFrequencies.get(field);
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
        return randomOperator(field);
    }

    private Operator randomOperator(SchemaField field) {
        // Generate a random operator from the Operator enum

        // If the field is City or Direction, we only allow EQ and NEQ operators
        if (field.field() == SchemaFieldNames.City || field.field() == SchemaFieldNames.Direction) {
            return Math.random() < 0.5 ? Operator.EQ : Operator.NEQ;
        }

        // For other fields, we allow all operators
        return Operator.values()[(int) (Math.random() * Operator.values().length)];
    }

    private boolean isAverageField(SchemaField field) {
        if (field.field() == SchemaFieldNames.Temp ||
            field.field() == SchemaFieldNames.Rain ||
            field.field() == SchemaFieldNames.Wind) {
            return Math.random() < avgFieldProbability;
        }
        // For other fields, we assume they are not average fields
        return false;
    }
}
