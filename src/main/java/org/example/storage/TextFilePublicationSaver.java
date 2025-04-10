package org.example.storage;

import org.example.Publication;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class TextFilePublicationSaver implements PublicationSaver {
    private final BufferedWriter writer;

    public TextFilePublicationSaver(String fileName) throws IOException {
        writer = new BufferedWriter(new FileWriter(fileName));
    }

    @Override
    public synchronized void save(Publication publication) throws IOException {
        writer.write(publication.toString());
        writer.newLine();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
