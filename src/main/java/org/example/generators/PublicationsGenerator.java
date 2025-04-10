package org.example.generators;

import org.example.Publication;
import org.example.schema.Schema;
import org.example.schema.SchemaField;
import org.example.storage.PublicationSaver;

public class PublicationsGenerator {
    private final Schema schema;
    private final int numberOfPublications;
    private PublicationSaver publicationSaver;

    public PublicationsGenerator(Schema schema, int numberOfPublications) {
        this.schema = schema;
        this.numberOfPublications = numberOfPublications;
    }

    public void setPublicationSaver(PublicationSaver saver) {
        this.publicationSaver = saver;
    }

    private void generatePublication() {
        Publication publication = new Publication();

        for (SchemaField field : schema.fields) {
            String value = generateValue(field);
            publication.addField(field, value);
        }

        if (publicationSaver != null) {
            try {
                publicationSaver.save(publication);
            } catch (Exception e) {
                System.err.println("Error saving publication: " + e.getMessage());
            }
        } else {
            System.out.println(publication);
        }
    }

    public void generatePublications() {
        for (int i = 0; i < this.numberOfPublications; i++) {
            generatePublication();
        }
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
