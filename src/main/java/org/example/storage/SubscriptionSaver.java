package org.example.storage;

import org.example.Subscription;
import java.io.IOException;

public interface SubscriptionSaver {
    void save(Subscription subscription) throws IOException;
    void close() throws IOException;
}
