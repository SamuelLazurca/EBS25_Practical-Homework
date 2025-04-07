package org.example.generators;

import org.example.Subscription;
import org.example.schema.*;

import java.util.HashMap;

public class SubscriptionsGenerator {
    private final Schema schema;
    private final int maxCount;
    private int currentCount = 0;
    private int fieldsTotalCount = 0;
    private int fieldsTotalMaxCount;
    private boolean allFieldsHaveFrequencyRestrictions = true;

    private final HashMap<SchemaField, Integer> fieldsRequiredFrequency;
    private final HashMap<SchemaField, Integer> equalOperatorsRequiredFrequency;

    public HashMap<SchemaField, Integer> getFieldsCurrentFrequency() {
        return fieldsCurrentFrequency;
    }

    private final HashMap<SchemaField, Integer> fieldsCurrentFrequency;

    public SubscriptionsGenerator(Schema schema,
                                  int maxCount,
                                  HashMap<SchemaField, Double> fieldsFrequencyPercentage,
                                  HashMap<SchemaField, Integer> equalOperatorsRequiredFrequency) throws Exception {
        this.maxCount = maxCount;
        this.schema = schema;
        this.fieldsRequiredFrequency = new HashMap<>();
        this.fieldsCurrentFrequency = new HashMap<>();

        for (SchemaField field : schema.fields) {
            fieldsCurrentFrequency.put(field, 0);

            if (fieldsFrequencyPercentage.get(field) != null) {
                int fieldFrequency = (int) Math.round(fieldsFrequencyPercentage.get(field) * maxCount) / 100;
                fieldsRequiredFrequency.put(field, fieldFrequency);
                fieldsTotalMaxCount += fieldFrequency;
            }
            else {
                // nu am restricții de frecvență pentru unele field-uri
                // nu voi avea riscul de a produce subscriptii goale

                this.allFieldsHaveFrequencyRestrictions = false;
            }
        }

        if (allFieldsHaveFrequencyRestrictions && fieldsTotalMaxCount < maxCount)
        {
            throw new Exception("Total frequency percentage is less than 100%");
        }

        this.equalOperatorsRequiredFrequency = equalOperatorsRequiredFrequency;
    }

    public Subscription generateSubscription() {
        Subscription subscription = new Subscription();
        int subscriptionFieldsCount = 0;

        while (subscriptionFieldsCount == 0) // macar un field pe subscriptie
        {
            for (SchemaField field : schema.fields) {
                if (allFieldsHaveFrequencyRestrictions && fieldsTotalMaxCount - fieldsTotalCount < maxCount - currentCount) {
                    // ar trebui sa pastrez field-uri si pentru urmatoarele iteratii
                    // ca sa nu am subscriptii goale

                    break;
                }

                if (fieldsRequiredFrequency.get(field) == null)
                {
                    // n-am restricții de frecvență => dau cu banul

                    if (Math.random() < 0.5) {
                        updateSubscription(subscription, field);
                        subscriptionFieldsCount += 1;
                    }
                    continue;
                }

                int dif = fieldsRequiredFrequency.get(field) - fieldsCurrentFrequency.get(field);
                if (dif > 0) {
                    // inca nu am atins frecventa necesara

                    if ((maxCount - currentCount) > dif) {
                        // am posibilitatea sa ating frecventa necesara
                        // fara sa aleg numaidecat field-ul respectiv
                        // la iteratia curenta
                        // dau cu banul

                        if (Math.random() < 0.5) {
                            updateSubscription(subscription, field);
                            subscriptionFieldsCount += 1;
                        }
                    } else {
                        // pot atinge frecventa necesara doar
                        // alegand field-ul respectiv la fiecare iteratie

                        updateSubscription(subscription, field);
                        subscriptionFieldsCount += 1;
                    }
                }
            }
        }

        this.currentCount += 1;

        return subscription;
    }

    private String generateValue(SchemaField field) {
        return switch (field.field()) {
            case Station -> String.valueOf(GeneratorsParams.stationLimit.getRandomValue());
            case City -> GeneratorsParams.cities.get((int) (Math.random() * GeneratorsParams.cities.size()));
            case Temp -> String.valueOf(GeneratorsParams.tempLimit.getRandomValue());
            case Rain -> String.valueOf(GeneratorsParams.rainLimit.getRandomValue());
            case Wind -> String.valueOf(GeneratorsParams.windLimit.getRandomValue());
            case Direction -> GeneratorsParams.directions.get((int) (Math.random() * GeneratorsParams.directions.size()));
            case Date -> GeneratorsParams.dateFormat.format(GeneratorsParams.dateLimit.getRandomValue());
        };
    }

    private Operator getRandomOperator() {
        int randomIndex = (int) (Math.random() * Operator.values().length);
        return Operator.values()[randomIndex];
    }

    private void updateSubscription(Subscription subscription, SchemaField field) {
        String value = generateValue(field);
        Operator operator = getRandomOperator();
        subscription.addField(field, new SubscriptionValue(operator, value));
        fieldsCurrentFrequency.put(field, fieldsCurrentFrequency.get(field) + 1);
        fieldsTotalCount += 1;
    }
}
