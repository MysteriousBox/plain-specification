package org.plain.specification.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.plain.specification.core.ISpecification;
import org.plain.specification.core.PageQuery;
import org.plain.specification.core.Specification;
import org.plain.specification.core.expression.Expressions;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Which index command serves an ORDER BY that the declared score field can answer, and which
 * orderings must fall back to reading in index order and sorting in memory.
 */
class RedisWriteAndOrderCommandTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ZSetOperations<String, String> zSetOps;
    @Mock
    private HashOperations<String, Object, Object> hashOps;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        doReturn(hashOps).when(redisTemplate).opsForHash();
    }

    static class Item {
        private String id;
        private int score;
        private String tag;

        Item(String id, int score, String tag) {
            this.id = id;
            this.score = score;
            this.tag = tag;
        }

        public String getId() { return id; }
        public int getScore() { return score; }
        public String getTag() { return tag; }
    }

    private RedisRepositoryConfig.Builder<Item, String> builder() {
        return new RedisRepositoryConfig.Builder<Item, String>()
                .resolvedZsetKey("item:index")
                .resolvedHashKeyPrefix("item:")
                .batchSize(10)
                .serializer(DefaultStrategies.jacksonSerializer())
                .deserializer(DefaultStrategies.jacksonDeserializer(Item.class))
                .idExtractor(DefaultStrategies.reflectionIdExtractor(Item.class, null))
                .scoreProvider(Item::getScore)
                .ttlProvider(DefaultStrategies.defaultTtlProvider())
                .fieldExtractor(entity -> {
                    Map<String, Object> fields = new LinkedHashMap<String, Object>();
                    fields.put("id", entity.getId());
                    fields.put("score", entity.getScore());
                    fields.put("tag", entity.getTag());
                    return fields;
                })
                .entityBuilder(fields -> new Item(
                        String.valueOf(fields.get("id")),
                        Integer.parseInt(String.valueOf(fields.get("score"))),
                        fields.get("tag") == null ? null : String.valueOf(fields.get("tag"))));
    }

    private GenericRedisRepository<Item, String> repository(RedisRepositoryConfig<Item, String> config) {
        return new GenericRedisRepository<Item, String>(redisTemplate, config);
    }

    // ---- A1 / C: ORDER BY on the declared score field is answered by the index itself ----

    private void stubIndex(long total, String... ids) {
        when(zSetOps.size(anyString())).thenReturn(total);
        when(zSetOps.range(anyString(), anyLong(), anyLong())).thenReturn(new java.util.LinkedHashSet<String>(Arrays.asList(ids)));
        when(zSetOps.reverseRange(anyString(), anyLong(), anyLong())).thenReturn(new java.util.LinkedHashSet<String>(Arrays.asList(ids)));
        Map<Object, Object> empty = new LinkedHashMap<Object, Object>();
        doReturn(Collections.singletonList(empty)).when(redisTemplate)
                .executePipelined(any(org.springframework.data.redis.core.SessionCallback.class));
        doReturn(empty).when(hashOps).entries(anyString());
    }

    @Test
    void descendingScoreOrderReversesTheIndexWalk() {
        stubIndex(30L, "i3", "i2");
        GenericRedisRepository<Item, String> repo = repository(builder().scoreField("score").build());

        ISpecification<Item> desc = new Specification<Item>().query()
                .orderBy(Expressions.<Item>create().orderByDescending(Item::getScore))
                .getSpecification();
        repo.page(desc, new PageQuery(1, 2));

        verify(zSetOps).reverseRange("item:index", 0L, 1L);
        verify(zSetOps, never()).range(anyString(), anyLong(), anyLong());
    }

    @Test
    void ascendingScoreOrderWalksTheIndexForwards() {
        stubIndex(30L, "i1", "i2");
        GenericRedisRepository<Item, String> repo = repository(builder().scoreField("score").build());

        ISpecification<Item> asc = new Specification<Item>().query()
                .orderBy(Expressions.<Item>create().orderBy(Item::getScore))
                .getSpecification();
        repo.page(asc, new PageQuery(1, 2));

        verify(zSetOps).range("item:index", 0L, 1L);
        verify(zSetOps, never()).reverseRange(anyString(), anyLong(), anyLong());
    }

    @Test
    void orderOnAnotherFieldDoesNotTouchTheIndexOrder() {
        stubIndex(30L, "i1", "i2");
        GenericRedisRepository<Item, String> repo = repository(builder().scoreField("score").build());

        ISpecification<Item> byTag = new Specification<Item>().query()
                .orderBy(Expressions.<Item>create().orderByDescending(Item::getTag))
                .getSpecification();
        repo.page(byTag, new PageQuery(1, 2));

        // the fast path cannot serve it, so the rows are read in index order and sorted in memory
        verify(zSetOps, never()).reverseRange(anyString(), anyLong(), anyLong());
        verify(zSetOps, org.mockito.Mockito.atLeastOnce()).range(anyString(), anyLong(), anyLong());
    }
}
