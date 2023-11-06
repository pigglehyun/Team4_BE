
package com.ktc.matgpt.chatgpt.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.util.Collections;

@Profile("deploy")
@Configuration
public class HttpConnectionProxyConfig {

    private static final String PROXY_HOST = "krmp-proxy.9rum.cc";
    private static final int PROXY_PORT = 3128;
    public static final Duration API_RESPONSE_TIMEOUT = Duration.ofSeconds(60);

    @Value("${chatgpt.api.key}")
    private String apiKey;

    @Configuration
    public class HttpConnectionInterceptorConfig {

        @Value("${chatgpt.api.key}")
        private String apiKey;

        @Bean
        public RestTemplate restTemplateWithGlobalProxy(RestTemplateBuilder builder) {
            SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXY_HOST, PROXY_PORT));
            clientHttpRequestFactory.setProxy(proxy);
            clientHttpRequestFactory.setConnectTimeout((int) API_RESPONSE_TIMEOUT.toMillis());
            clientHttpRequestFactory.setReadTimeout((int) API_RESPONSE_TIMEOUT.toMillis());

            RestTemplate restTemplate = builder.requestFactory(() -> clientHttpRequestFactory).build();
            restTemplate.setInterceptors(Collections.singletonList(openAIAuthInterceptor()));
            return restTemplate;
        }

        @Bean
        public ClientHttpRequestInterceptor openAIAuthInterceptor() {
            return (request, body, execution) -> {
                HttpHeaders headers = request.getHeaders();
                if ("api.openai.com".equals(request.getURI().getHost())) {
                    headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
                }
                headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                return execution.execute(request, body);
            };
        }
    }
}