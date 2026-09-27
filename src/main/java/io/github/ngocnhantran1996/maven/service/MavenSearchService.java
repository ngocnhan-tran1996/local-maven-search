package io.github.ngocnhantran1996.maven.service;

import io.github.ngocnhantran1996.maven.JarEntryResource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Service for downloading and reading Javadoc artifacts from Maven Central.
 *
 * <p>The Javadoc artifacts are stored in the configured target directory and
 * reused when they are already available locally.
 *
 * @author Ngoc Nhan
 */
@Slf4j
@Service
public class MavenSearchService {

    private static final String DEFAULT_TARGET_DIR = "target/javadoc/downloads/";
    private static final String JAVADOC_SUFFIX = "-javadoc.jar";

    private final String targetDir;
    private final RestClient restClient;

    /**
     * Construct a {@link MavenSearchService}.
     *
     * @param targetDir  the directory where Javadoc artifacts are downloaded
     * @param restClient the REST client used to download Javadoc artifacts
     */
    public MavenSearchService(@Value("${app.jar.download-dir:}") String targetDir, RestClient restClient) {
        this.targetDir = StringUtils.hasText(targetDir) ? targetDir : DEFAULT_TARGET_DIR;
        this.restClient = restClient;
    }

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
    public boolean existsWithJavadoc(String group, String artifact, String version) {

        String fileName = artifact + "-" + version + JAVADOC_SUFFIX;
        Path path = Path.of(this.targetDir, fileName);
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {

            log.info("FileName {} in folder {} already exists", fileName, path);
            return true;
        }

        return this.restClient.get()
            .uri("https://repo1.maven.org/maven2/{group}/{artifact}/{version}/{fileName}",
                group.replace(".", "/"), artifact, version, fileName)
            .accept(MediaType.APPLICATION_JSON)
            .exchange((clientRequest, response) -> {

                if (response.getStatusCode().isError()) {

                    log.debug("Could not search maven with status {}", response.getStatusText());
                    return false;
                }

                log.info("Download fileName {} into folder {}", fileName, path);
                Files.createDirectories(path);
                try (InputStream inputStream = response.getBody()) {
                    Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
                }
                return true;
            });
    }

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
    public JarEntryResource readJavadoc(String artifact, String version, String entryName) throws IOException {

        String fileName = artifact + "-" + version + JAVADOC_SUFFIX;
        Path path = Path.of(this.targetDir, fileName);
        JarFile jarFile = new JarFile(path.toFile());
        boolean success = false;

        try {

            JarEntry entry = jarFile.getJarEntry(entryName);
            if (entry != null && !entry.isDirectory()) {
                success = true;
                return new JarEntryResource(jarFile, jarFile.getInputStream(entry));
            }

            log.debug("Not found entryName {}", entryName);
            return null;
        } finally {

            if (!success) {
                jarFile.close();
            }
        }
    }

}
