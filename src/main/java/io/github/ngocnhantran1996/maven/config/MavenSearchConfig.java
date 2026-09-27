package io.github.ngocnhantran1996.maven.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class MavenSearchConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }

}
