package io.github.ngocnhantran1996.maven.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration for Maven search services.
 *
 * @author Ngoc Nhan
 */
@Configuration(proxyBeanMethods = false)
public class MavenSearchConfig {

    /**
     * Create a {@link RestClient}.
     *
     * @return the REST client
     */
    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }

}
