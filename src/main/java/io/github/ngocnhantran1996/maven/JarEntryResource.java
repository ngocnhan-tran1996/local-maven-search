package io.github.ngocnhantran1996.maven;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;

public record JarEntryResource(JarFile jarFile, InputStream inputStream) implements AutoCloseable {

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