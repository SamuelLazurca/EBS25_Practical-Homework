package org.example.storage;

import org.example.Publication;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class TextFilePublicationSaver implements PublicationSaver {
    private final BufferedWriter writer;
    private boolean firstWrite = true;

    public TextFilePublicationSaver(String fileName) throws IOException {
        writer = new BufferedWriter(new FileWriter(fileName));
        writer.write("[\n");
    }

    @Override
    public synchronized void save(Publication publication) throws IOException {
        if (!firstWrite) {
            writer.write(",\n");
        }
        writer.write(publication.toJson());
        firstWrite = false;
    }

    @Override
    public void close() throws IOException {
        writer.write("\n]");
        writer.close();
    }
}
