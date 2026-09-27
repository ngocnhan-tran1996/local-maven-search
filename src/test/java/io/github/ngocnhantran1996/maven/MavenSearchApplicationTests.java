package io.github.ngocnhantran1996.maven;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.util.UriComponentsBuilder;

//@SpringBootTest
class MavenSearchApplicationTests {

    @Test
    void contextLoads() {

        System.out.println(UriComponentsBuilder.fromPath("///").build().getPath().length());
    }

}
