package com.company.aa.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Configuration
public class RestClientConfig {

    /**
     * RestTemplate configured for Digio's AA APIs, with Basic Auth applied from
     * DIGIO_USERNAME / DIGIO_PASSWORD (never hardcoded — see application.yml + .env.example).
     */
    @Bean
    public RestTemplate digioRestTemplate(RestTemplateBuilder builder, DigioProperties digioProperties) {
        ClientHttpRequestFactory factory = clientHttpRequestFactory(digioProperties);
        RestTemplate template = builder
                .rootUri(digioProperties.baseUrl())
                .requestFactory(() -> factory)
                .build();

        if (digioProperties.username() != null && !digioProperties.username().isBlank()) {
            String creds = digioProperties.username() + ":" + digioProperties.password();
            String basicAuth = "Basic " + Base64.getEncoder().encodeToString(creds.getBytes());
            template.getInterceptors().add((request, body, execution) -> {
                request.getHeaders().add("Authorization", basicAuth);
                request.getHeaders().add("Content-Type", "application/json");
                return execution.execute(request, body);
            });
        }
        return template;
    }

    private ClientHttpRequestFactory clientHttpRequestFactory(DigioProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.connectTimeoutMs());
        factory.setReadTimeout(props.readTimeoutMs());
        return factory;
    }
}
