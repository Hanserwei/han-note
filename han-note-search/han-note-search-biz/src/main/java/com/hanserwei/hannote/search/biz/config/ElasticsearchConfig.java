package com.hanserwei.hannote.search.biz.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ElasticsearchConfig {

    @Value("${elasticsearch.host}")
    private String host;

    @Resource
    private ObjectMapper objectMapper;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // 1. 创建底层 RestClient（低级客户端）
        RestClient restClient = RestClient
                .builder(HttpHost.create(host))
                .build();

        // 3. 构建传输层
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper));

        // 4. 创建高层次的 Elasticsearch 客户端
        return new ElasticsearchClient(transport);
    }

}
