package io.github.ngocnhantran1996.maven;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;

/**
 * A resource backed by a {@link JarFile} entry and its {@link InputStream}.
 *
 * <p>Closing this resource closes both the input stream and the JAR file.
 *
 * @author Ngoc Nhan
 */
public record JarEntryResource(JarFile jarFile, InputStream inputStream) implements AutoCloseable {

    /**
     * Closes the input stream and the JAR file.
     *
     * @throws IOException if an I/O error occurs while closing either resource
     */
    @Override
    public void close() throws IOException {
        try {

            if (this.inputStream != null) {
                this.inputStream.close();
            }
        } finally {

            if (this.jarFile != null) {
                this.jarFile.close();
            }
        }
    }

}