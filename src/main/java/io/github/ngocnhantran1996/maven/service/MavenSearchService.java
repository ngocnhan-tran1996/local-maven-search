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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class MavenSearchService {

    private static final String DEFAULT_TARGET_DIR = "target/javadoc/downloads/";
    private static final String JAVADOC_SUFFIX = "-javadoc.jar";

    private final String targetDir;
    private final RestClient restClient;

    public MavenSearchService(@Value("${app.jar.dowload-dir:}") String targetDir, RestClient restClient) {
        this.targetDir = StringUtils.hasText(targetDir) ? targetDir : DEFAULT_TARGET_DIR;
        this.restClient = restClient;
    }

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
