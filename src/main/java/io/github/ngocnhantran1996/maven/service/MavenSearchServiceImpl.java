package io.github.ngocnhantran1996.maven.service;

import io.github.ngocnhantran1996.maven.JarEntryResource;
import io.github.ngocnhantran1996.maven.http.MavenSearchResponse;
import io.github.ngocnhantran1996.maven.http.MavenSearchResponse.Response.MavenSearchDocument;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Implement {@link MavenSearchService}.
 *
 * @author Ngoc Nhan
 */
@Slf4j
@Service
public class MavenSearchServiceImpl implements MavenSearchService {

    private static final String DEFAULT_TARGET_DIR = "target/javadoc/downloads/";
    private static final String JAVADOC_SUFFIX = "-javadoc.jar";

    private final String targetDir;
    private final RestClient restClient;

    /**
     * Construct a {@link MavenSearchServiceImpl}.
     *
     * @param targetDir  the directory where Javadoc artifacts are downloaded
     * @param restClient the REST client used to download Javadoc artifacts
     */
    public MavenSearchServiceImpl(@Value("${app.jar.download-dir:}") String targetDir, RestClient restClient) {
        this.targetDir = StringUtils.hasText(targetDir) ? targetDir : DEFAULT_TARGET_DIR;
        this.restClient = restClient;
    }

    @Override
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

    @Override
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

    @Override
    public String[] searchArtifacts(String group) {

        List<MavenSearchResponse> responses = this.searchMavenCoordinates(group, null);
        List<MavenSearchDocument> documents = responses.stream()
            .flatMap(mavenSearchResponse -> Stream.of(mavenSearchResponse.getResponse().getDocs()))
            .toList();
        String[] artifacts = documents.stream().map(MavenSearchDocument::getA).distinct().toArray(String[]::new);
        log.debug("Total search artifacts {}", artifacts.length);
        return artifacts;
    }

    @Override
    public String[] searchVersions(String group, String artifact) {

        List<MavenSearchResponse> responses = this.searchMavenCoordinates(group, artifact);
        List<MavenSearchDocument> documents = responses.stream()
            .flatMap(mavenSearchResponse -> Stream.of(mavenSearchResponse.getResponse().getDocs()))
            .toList();
        String[] versions = documents.stream().map(MavenSearchDocument::getV).distinct().toArray(String[]::new);
        log.debug("Total search versions {}", versions.length);
        return versions;
    }

    @Override
    public List<MavenSearchResponse> searchMavenCoordinates(String group, @Nullable String artifact) {

        int rows = 20;
        MavenSearchResponse firstResponse = this.fetch(group, artifact, rows, 0);
        if (firstResponse == null) {

            log.debug("Not found response");
            return Collections.emptyList();
        }

        int totalElements = firstResponse.getResponse().getNumFound();
        if (totalElements == 0) {

            log.debug("Not found elements");
            return Collections.emptyList();
        }

        List<MavenSearchResponse> responses = new ArrayList<>();
        responses.add(firstResponse);
        int startPage = firstResponse.getResponse().getStart() + rows;
        while (totalElements >= startPage) {

            MavenSearchResponse response = this.fetch(group, artifact, rows, startPage);
            responses.add(response);
            startPage += rows;
        }

        log.debug("Total elements {}", responses.size());
        return responses;
    }

    private MavenSearchResponse fetch(String group, String artifact, int rows, int start) {

        Map<String, String> mavenQueries = new HashMap<>();
        mavenQueries.put("g", group);
        mavenQueries.put("a", artifact);
        if (StringUtils.hasText(group) && StringUtils.hasText(artifact)) {
            mavenQueries.put("l", "javadoc");
        }

        String query = mavenQueries.entrySet().stream()
            .filter(entry -> StringUtils.hasText(entry.getValue()))
            .map(entry -> entry.getKey() + ":" + entry.getValue())
            .collect(Collectors.joining(" AND "));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//        params.add("core", "guide");
        params.add("wt", "json");
        params.add("rows", String.valueOf(rows));
        params.add("start", String.valueOf(start));
        params.add("q", query);

        try {

            return this.restClient.get()
                .uri("https://search.maven.org/solrsearch/select", uriBuilder -> uriBuilder.queryParams(params).build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(MavenSearchResponse.class);
        } catch (RestClientResponseException ex) {

            log.error("Could not call API with status {}", ex.getStatusCode(), ex);
            return null;
        }
    }

}
