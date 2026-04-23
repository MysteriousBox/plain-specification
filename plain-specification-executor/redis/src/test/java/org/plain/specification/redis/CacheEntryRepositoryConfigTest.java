package org.plain.specification.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.plain.utils.JsonUtil;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * CacheEntry 配置示例测试：演示以 CacheEntry<T> 作为实体时的配置组装。
 *
 * @author Jayden.Liang
 */
class CacheEntryRepositoryConfigTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private ZSetOperations<String, String> zSetOps;
    @Mock
    private HashOperations<String, String, String> hashOps;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        doReturn(hashOps).when(redisTemplate).opsForHash();
    }

    public static class User {
        private String id;
        private String name;

        public User() {
        }

        public User(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class CacheMetadata implements Serializable {
        private Long ttlSeconds;
        private Double score;

        public CacheMetadata() {
        }

        public CacheMetadata(Long ttlSeconds, Double score) {
            this.ttlSeconds = ttlSeconds;
            this.score = score;
        }

        public Long getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(Long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }
    }

    public static class CacheEntry<T> implements Serializable {
        private String cacheKey;
        private T value;
        private CacheMetadata cacheMetadata;

        public CacheEntry() {
        }

        public CacheEntry(String cacheKey, T value, CacheMetadata cacheMetadata) {
            this.cacheKey = cacheKey;
            this.value = value;
            this.cacheMetadata = cacheMetadata;
        }

        public String getCacheKey() {
            return cacheKey;
        }

        public void setCacheKey(String cacheKey) {
            this.cacheKey = cacheKey;
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public CacheMetadata getCacheMetadata() {
            return cacheMetadata;
        }

        public void setCacheMetadata(CacheMetadata cacheMetadata) {
            this.cacheMetadata = cacheMetadata;
        }
    }

    @Test
    void testCacheEntryConfigRoundTrip() {
        RedisRepositoryConfig<CacheEntry<User>, String> config = buildConfig();

        CacheMetadata metadata = new CacheMetadata(120L, 10d);
        CacheEntry<User> entry = new CacheEntry<>("user:1", new User("1", "Alice"), metadata);

        Map<String, Object> fields = config.getFieldExtractor().extractFields(entry);
        Map<Object, Object> serializedFields = new HashMap<>();
        for (Map.Entry<String, Object> e : fields.entrySet()) {
            serializedFields.put(e.getKey(), JsonUtil.serialize(e.getValue()));
        }

        CacheEntry<User> restored = config.getEntityBuilder().buildEntity(serializedFields);
        assertNotNull(restored);
        assertEquals("user:1", restored.getCacheKey());
        assertNotNull(restored.getValue());
        assertEquals("Alice", restored.getValue().getName());
        assertNotNull(restored.getCacheMetadata());
        assertEquals(120L, restored.getCacheMetadata().getTtlSeconds());
        assertEquals(10d, restored.getCacheMetadata().getScore());

        assertEquals("user:1", config.getIdExtractor().getId(entry));
        assertEquals(10d, config.getScoreProvider().getScore(entry));
        assertEquals(120L, config.getTtlProvider().getTtl(entry));
    }

    @Test
    void testRepositoryWithCacheEntry() {
        RedisRepositoryConfig<CacheEntry<User>, String> config = buildConfig();
        GenericRedisRepository<CacheEntry<User>, String> repo = new GenericRedisRepository<>(redisTemplate, config);

        CacheMetadata metadata = new CacheMetadata(120L, 10d);
        CacheEntry<User> entry = new CacheEntry<>("user:1", new User("1", "Alice"), metadata);

        // Mock save
        when(redisTemplate.execute(any(), anyList(), any())).thenReturn(1L);

        CacheEntry<User> saved = repo.save(entry);
        assertNotNull(saved);
        verify(redisTemplate, atLeastOnce()).execute(any(), anyList(), any());

        // Mock findById
        Map<String, String> entries = new HashMap<>();
        entries.put("cacheKey", JsonUtil.serialize("user:1"));
        entries.put("value", JsonUtil.serialize(new User("1", "Alice")));
        entries.put("cacheMetadata", JsonUtil.serialize(metadata));
        when(hashOps.entries("cache:user:1")).thenReturn(entries);

        CacheEntry<User> found = repo.findById("user:1");
        assertNotNull(found);
        assertEquals("user:1", found.getCacheKey());
        assertEquals("Alice", found.getValue().getName());
        assertEquals(10d, found.getCacheMetadata().getScore());
    }

    private RedisRepositoryConfig<CacheEntry<User>, String> buildConfig() {
        @SuppressWarnings("unchecked")
        Deserializer<CacheEntry<User>> deserializer = s -> (CacheEntry<User>) JsonUtil.deserialize(s, CacheEntry.class);
        IdExtractor<CacheEntry<User>, String> idExtractor = CacheEntry::getCacheKey;
        ScoreProvider<CacheEntry<User>> scoreProvider = entry -> entry.getCacheMetadata() == null || entry.getCacheMetadata().getScore() == null
            ? 0d
            : entry.getCacheMetadata().getScore();
        TtlProvider<CacheEntry<User>> ttlProvider = entry -> entry.getCacheMetadata() == null ? null : entry.getCacheMetadata().getTtlSeconds();
        FieldExtractor<CacheEntry<User>> fieldExtractor = entry -> {
            Map<String, Object> map = new HashMap<>();
            map.put("cacheKey", entry.getCacheKey());
            map.put("value", entry.getValue());
            map.put("cacheMetadata", entry.getCacheMetadata());
            return map;
        };
        EntityBuilder<CacheEntry<User>> entityBuilder = fields -> {
            if (fields == null || fields.isEmpty()) {
                return null;
            }
            String cacheKey = JsonUtil.deserialize(String.valueOf(fields.get("cacheKey")), String.class);
            User value = JsonUtil.deserialize(String.valueOf(fields.get("value")), User.class);
            CacheMetadata meta = JsonUtil.deserialize(String.valueOf(fields.get("cacheMetadata")), CacheMetadata.class);
            return new CacheEntry<>(cacheKey, value, meta);
        };

        return new RedisRepositoryConfig.Builder<CacheEntry<User>, String>()
            .resolvedZsetKey("cache:index")
            .resolvedHashKeyPrefix("cache:")
            .serializer(DefaultStrategies.jacksonSerializer())
            .deserializer(deserializer)
            .idExtractor(idExtractor)
            .scoreProvider(scoreProvider)
            .ttlProvider(ttlProvider)
            .fieldExtractor(fieldExtractor)
            .entityBuilder(entityBuilder)
            .build();
    }


}
