package io.github.ngocnhantran1996.maven.service;

import io.github.ngocnhantran1996.maven.JarEntryResource;
import io.github.ngocnhantran1996.maven.http.MavenSearchResponse;
import java.io.IOException;
import java.util.List;

/**
 * Service for downloading and reading Javadoc artifacts from Maven Central.
 *
 * <p>The Javadoc artifacts are stored in the configured target directory and
 * reused when they are already available locally.
 *
 * @author Ngoc Nhan
 */
public interface MavenSearchService {

    /**
     * Checks whether the Javadoc artifact for the specified Maven coordinates is available locally or can be downloaded
     * from Maven Central.
     *
     * @param group    the Maven group ID
     * @param artifact the Maven artifact ID
     * @param version  the Maven artifact version
     * @return {@code true} if the Javadoc artifact is available locally or was successfully downloaded; {@code false}
     * if the artifact could not be downloaded
     */
    boolean existsWithJavadoc(String group, String artifact, String version);

    /**
     * Reads the specified entry from a locally stored Javadoc JAR. The caller must close the returned resource after
     * the entry has been consumed.
     *
     * @param artifact  the Maven artifact ID
     * @param version   the Maven artifact version
     * @param entryName the path of the entry within the Javadoc JAR
     * @return a {@link JarEntryResource} for the requested entry, or {@code null} if the entry does not exist or is a
     * directory
     * @throws IOException if the Javadoc JAR cannot be opened or the entry cannot be read
     */
    JarEntryResource readJavadoc(String artifact, String version, String entryName) throws IOException;

    String[] searchArtifacts(String group);

    String[] searchVersions(String group, String artifact);

    List<MavenSearchResponse> searchMavenCoordinates(String group, String artifact);

}
