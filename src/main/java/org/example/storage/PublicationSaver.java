package org.example.storage;

import org.example.Publication;

import java.io.IOException;

public interface PublicationSaver {
    void save(Publication publication) throws IOException;
    void close() throws IOException;
}

