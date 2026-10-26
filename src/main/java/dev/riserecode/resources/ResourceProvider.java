package dev.riserecode.resources;

import java.io.IOException;
import java.util.Collection;

/** Network acquisition is separate from the offline compiler and cache transaction. */
public interface ResourceProvider extends AutoCloseable {
    byte[] catalog() throws IOException;
    byte[] pack(ResourceCatalog catalog, Collection<String> selected) throws IOException;
    byte[] icon(ResourceEntry entry) throws IOException;
    byte[] preview(ResourceEntry entry) throws IOException;
    @Override void close();
}
