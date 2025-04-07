package org.example;

import org.example.generators.SubscriptionsGenerator;
import org.example.schema.Schema;
import org.example.schema.SchemaField;
import org.example.schema.SchemaFields;

import java.util.HashMap;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        Schema schema = new Schema(
                List.of(
                        SchemaFields.STATION,
                        SchemaFields.CITY,
                        SchemaFields.TEMP,
                        SchemaFields.RAIN,
                        SchemaFields.WIND,
                        SchemaFields.DIRECTION,
                        SchemaFields.DATE
                )
        );

        /*
        int numberOfPublications = 100;

        PublicationsGenerator generator = new PublicationsGenerator(schema);

        for (int i = 0; i < numberOfPublications; i++)
        {
            Publication publication = generator.generatePublication();
            System.out.println(publication);
        }*/

        int numberOfSubscriptions = 1000;

        HashMap<SchemaField, Double> fieldsFrequencyPercentage = new HashMap<>();
        for (SchemaField field : schema.fields) {
            fieldsFrequencyPercentage.put(field, 51.0);
        }

        fieldsFrequencyPercentage.put(SchemaFields.CITY, 90.0);

        HashMap<SchemaField, Integer> equalOperatorFrequency = new HashMap<>();
        for (SchemaField field : schema.fields) {
            equalOperatorFrequency.put(field, 20);
        }

        SubscriptionsGenerator subscriptionsGenerator = new SubscriptionsGenerator(
                schema,
                numberOfSubscriptions,
                fieldsFrequencyPercentage,
                equalOperatorFrequency
        );

        for (int i = 0; i < numberOfSubscriptions; i++)
        {
            Subscription subscription = subscriptionsGenerator.generateSubscription();
            System.out.println(subscription);
        }

        System.out.println(subscriptionsGenerator.getFieldsCurrentFrequency());
    }
}