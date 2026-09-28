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
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping("/")
    public String postHome(@ModelAttribute(name = "mavenSearchRequest") MavenSearchRequest request) {

        StringBuilder redirectBuilder = new StringBuilder("redirect:/javadoc/").append(request.getGroup());
        if (ObjectUtils.isEmpty(request.getSearchArtifacts())) {
            return redirectBuilder.toString();
        }

        redirectBuilder.append("/").append(request.getArtifact());
        if (ObjectUtils.isEmpty(request.getSearchVersions())) {

            return redirectBuilder.toString();
        }

        return redirectBuilder.append("/").append(request.getVersion()).append("/index.html").toString();
    }

    @GetMapping("/javadoc/{group}")
    public String getGroup(@PathVariable String group, Model model) {

        MavenSearchRequest request = (MavenSearchRequest) model.getAttribute("mavenSearchRequest");
        if (request == null) {
            request = new MavenSearchRequest();
            request.setGroup(group);
        }

        if (ObjectUtils.isEmpty(request.getSearchArtifacts())) {
            String[] searchArtifacts = this.mavenSearchService.searchArtifacts(group);
            request.setSearchArtifacts(searchArtifacts);
        }

        model.addAttribute("mavenSearchRequest", request);
        return "index";
    }

    @GetMapping("/javadoc/{group}/{artifact}")
    public String getArtifact(
        @PathVariable String group,
        @PathVariable String artifact,
        Model model) {

        MavenSearchRequest request = (MavenSearchRequest) model.getAttribute("mavenSearchRequest");
        if (request == null) {
            request = new MavenSearchRequest();
            request.setGroup(group);
            request.setArtifact(artifact);
        }

        if (ObjectUtils.isEmpty(request.getSearchArtifacts())) {
            String[] searchArtifacts = this.mavenSearchService.searchArtifacts(group);
            request.setSearchArtifacts(searchArtifacts);
        }

        if (ObjectUtils.isEmpty(request.getSearchVersions())) {

            String[] searchVersions = this.mavenSearchService.searchVersions(request.getGroup(),
                request.getArtifact());
            request.setSearchVersions(searchVersions);
        }

        model.addAttribute("mavenSearchRequest", request);
        return "index";
    }

    @GetMapping("/javadoc/{group}/{artifact}/{version}/{*path}")
    public String getJavadoc(
        @PathVariable String group,
        @PathVariable String artifact,
        @PathVariable String version,
        @PathVariable String path,
        HttpServletResponse response) throws IOException {

        if (!this.mavenSearchService.existsWithJavadoc(group, artifact, version)) {

            return "index";
        }

        String entryName = UriComponentsBuilder.fromPath(path).build().toString().length() > 1
            ? path.substring(1)
            : "index.html";
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
