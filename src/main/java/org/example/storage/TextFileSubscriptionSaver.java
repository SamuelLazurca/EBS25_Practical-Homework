package org.example.storage;

import org.example.Subscription;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class TextFileSubscriptionSaver implements SubscriptionSaver {
    private final BufferedWriter writer;
    private boolean firstWrite = true;

    public TextFileSubscriptionSaver(String fileName) throws IOException {
        writer = new BufferedWriter(new FileWriter(fileName));
        //writer.write("[\n");
    }

    @Override
    public synchronized void save(Subscription subscription) throws IOException {
        if (!firstWrite) {
            writer.write("\n");
        }
        writer.write(subscription.toJson());
        firstWrite = false;
    }

    @Override
    public void close() throws IOException {
        //writer.write("\n]");
        writer.close();
    }
}
