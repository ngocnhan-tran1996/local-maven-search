package io.github.ngocnhantran1996.maven.controller;

import io.github.ngocnhantran1996.maven.JarEntryResource;
import io.github.ngocnhantran1996.maven.http.MavenSearchRequest;
import io.github.ngocnhantran1996.maven.service.MavenSearchService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequiredArgsConstructor
public class MavenSearchController {

    private final MavenSearchService mavenSearchService;

    @GetMapping("/")
    public String getHome(Model model) {

        model.addAttribute("mavenSearchRequest", new MavenSearchRequest());
        return "index";
    }

    @GetMapping("/javadoc/{group}/{artifact}/{version}/{*path}")
    public String getJavadoc(
        @PathVariable String group,
        @PathVariable String artifact,
        @PathVariable String version,
        @PathVariable String path,
        HttpServletResponse response,
        Model model) throws IOException {

        model.addAttribute("group", group)
            .addAttribute("artifact", artifact)
            .addAttribute("version", version);

        if (!this.mavenSearchService.existsWithJavadoc(group, artifact, version)) {

            return "index";
        }

        String entryName = UriComponentsBuilder.fromPath(path).build().getPath().length() > 1
            ? path.substring(1)
            : "overview-summary.html";
        JarEntryResource jarEntryResource = this.mavenSearchService.readJavadoc(artifact, version, entryName);

        if (jarEntryResource == null) {

            return "index";
        }

        try (jarEntryResource) {

            String cacheHeader = CacheControl.maxAge(1, TimeUnit.HOURS)
                .cachePublic()
                .immutable()
                .getHeaderValue();

            response.setHeader("Cache-Control", cacheHeader);

            String contentType = MediaTypeFactory.getMediaType(entryName).map(MediaType::toString)
                .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setContentType(contentType);

            jarEntryResource.inputStream().transferTo(response.getOutputStream());
            response.flushBuffer();
        }

        return null;
    }

}
