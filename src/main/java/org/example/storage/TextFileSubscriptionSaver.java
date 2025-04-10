package org.example.storage;

import org.example.Subscription;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class TextFileSubscriptionSaver implements SubscriptionSaver {
    private final BufferedWriter writer;

    public TextFileSubscriptionSaver(String fileName) throws IOException {
        writer = new BufferedWriter(new FileWriter(fileName));
    }

    @Override
    public synchronized void save(Subscription subscription) throws IOException {
        writer.write(subscription.toString());
        writer.newLine();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
