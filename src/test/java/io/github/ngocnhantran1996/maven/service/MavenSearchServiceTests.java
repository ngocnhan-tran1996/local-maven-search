package io.github.ngocnhantran1996.maven.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.ngocnhantran1996.maven.JarEntryResource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

/**
 * Test for {@link MavenSearchService}
 *
 * @author Ngoc Nhan
 */
class MavenSearchServiceTests {

    @TempDir
    Path tempDir;

    @Test
    void existsWithJavadocWhenFileExists() throws Exception {

        String group = "io.github.ngocnhan-tran1996";
        String artifact = "maven-search";
        String version = "1.0";

        Files.createFile(this.tempDir.resolve(artifact + "-" + version + "-javadoc.jar"));

        MavenSearchService mavenSearchService = new MavenSearchService(this.tempDir.toString(), mock());
        assertThat(mavenSearchService.existsWithJavadoc(group, artifact, version)).isTrue();
    }

    @Test
    void existsWithJavadocWhenFileNotExistsAndBadRequest() {

        String group = "io.github.ngocnhan-tran1996";
        String artifact = "maven-search";
        String version = "1.0";

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        mockServer.expect(MockRestRequestMatchers.anything())
            .andRespond(MockRestResponseCreators.withBadRequest());

        MavenSearchService mavenSearchService = new MavenSearchService(this.tempDir.toString(), builder.build());
        assertThat(mavenSearchService.existsWithJavadoc(group, artifact, version)).isFalse();
    }

    @Test
    void existsWithJavadocWhenFileNotExistsAndSuccessRequest() {

        String group = "io.github.ngocnhan-tran1996";
        String artifact = "maven-search";
        String version = "1.0";

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        mockServer.expect(MockRestRequestMatchers.anything())
            .andRespond(MockRestResponseCreators.withSuccess());

        MavenSearchService mavenSearchService = new MavenSearchService(this.tempDir.toString(), builder.build());
        assertThat(mavenSearchService.existsWithJavadoc(group, artifact, version)).isTrue();
        assertThat(Files.exists(this.tempDir.resolve(artifact + "-" + version + "-javadoc.jar"))).isTrue();
    }

    @Test
    void readJavadocWhenFileExists() throws IOException {

        String artifact = "maven-search";
        String version = "1.0";
        String entryName = "index.html";
        String fileContent = "<h1>Hello Javadoc</h1>";
        File file = this.tempDir.resolve(artifact + "-" + version + "-javadoc.jar").toFile();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))) {

            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            zos.write(fileContent.getBytes(StandardCharsets.UTF_8));
        }

        MavenSearchService mavenSearchService = new MavenSearchService(this.tempDir.toString(), mock());

        try (JarEntryResource jarEntryResource = mavenSearchService.readJavadoc(artifact, version, entryName)) {
            assertThat(jarEntryResource).isNotNull();

            String actualContent = new String(jarEntryResource.inputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(actualContent).isEqualTo(fileContent);
        }
    }

    @Test
    void readJavadocWhenFileNotExists() throws IOException {

        String artifact = "maven-search";
        String version = "1.0";
        String entryName = "index.html";
        File file = this.tempDir.resolve(artifact + "-" + version + "-javadoc.jar").toFile();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))) {
            // ignored
        }

        MavenSearchService mavenSearchService = new MavenSearchService(this.tempDir.toString(), mock());

        try (JarEntryResource jarEntryResource = mavenSearchService.readJavadoc(artifact, version, entryName)) {
            assertThat(jarEntryResource).isNull();
        }
    }

}