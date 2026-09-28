package io.github.ngocnhantran1996.maven.http;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MavenSearchResponse {

    private ResponseHeader responseHeader;
    private Response response;

    @Getter
    @Setter
    public static class ResponseHeader {

        private int status;
    }

    @Getter
    @Setter
    public static class Response {

        private int numFound;
        private int start;
        private MavenSearchDocument[] docs;

        @Getter
        @Setter
        public static class MavenSearchDocument {

            /**
             * fullname
             */
            private String id;

            /**
             * group
             */
            private String g;

            /**
             * artifact
             */
            private String a;

            /**
             * version
             */
            private String v;

            private long timestamp;
            private String[] ec;
        }
    }
}