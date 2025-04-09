package org.example.generators;

import org.example.Publication;
import org.example.schema.Schema;
import org.example.schema.SchemaField;

public class PublicationsGenerator {
    private final Schema schema;

    public PublicationsGenerator(Schema schema) {
        this.schema = schema;
    }

    public Publication generatePublication() {
        Publication publication = new Publication();

        for (SchemaField field : schema.fields) {
            String value = generateValue(field);
            publication.addField(field, value);
        }

        return publication;
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
}
