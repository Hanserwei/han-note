package com.hanserwei.hannote.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.hanserwei.hannote.search.model.vo.SearchUserRspVO;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ElasticsearchClientTest {

    @Autowired
    private ElasticsearchClient client;

    @Test
    @SneakyThrows
    public void testClient() {
        SearchResponse<SearchUserRspVO> response = client.search(s -> s
                        .index("user")
                        .query(q -> q
                                .multiMatch(mm -> mm
                                        .query("Han")
                                        .fields("nickname", "han_note_id")
                                )
                        )
                        .sort(so -> so
                                .field(f -> f.field("fans_total").order(SortOrder.Desc))
                        )
                        .from(0)
                        .size(10),
                SearchUserRspVO.class
        );
        response.hits().hits().forEach(hit -> System.out.println(hit.source()));
    }
}
