package io.github.ngocnhantran1996.maven.http;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MavenSearchRequest {

    private String group;
    private String artifact;
    private String version;
    private String[] searchArtifacts;
    private String[] searchVersions;

}
