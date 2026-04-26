package com.switchwon.forexordersystem.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

/**
 * 외부 환율 API 호출 WebClient 빈 설정
 * 외부 API 호출은 재사용성을 위해 싱글톤
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient exchangeApiWebClient(
            @Value("${app.exchange-api.base-url}") String baseUrl,
            @Value("${app.exchange-api.timeout-ms:3000}") int timeoutMs
    ) {
        // Netty Client 레벨에서 connect/read/write 타임아웃 각각 설정
        HttpClient httpClient = HttpClient.create()
                                          .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs)
                                          .doOnConnected(
                                            conn -> conn.addHandlerLast(new ReadTimeoutHandler(timeoutMs, TimeUnit.MILLISECONDS))
                                                        .addHandlerLast(new WriteTimeoutHandler(timeoutMs, TimeUnit.MILLISECONDS))
                                          );

        return WebClient.builder()
                        .baseUrl(baseUrl)
                        .clientConnector(
                            new ReactorClientHttpConnector(httpClient)
                        )
                        .build();
    }

}