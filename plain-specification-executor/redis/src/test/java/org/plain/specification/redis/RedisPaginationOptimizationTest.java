package org.plain.specification.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.plain.specification.core.*;
import org.plain.specification.core.IPageResult;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedisPaginationOptimizationTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ZSetOperations<String, String> zSetOps;
    @Mock
    private HashOperations<String, Object, Object> hashOps;

    private TestableRedisRepository repo;

    static class User {
        public String id;
        public String name;
        public int age;

        public User() {}
        public User(String id, String name, int age) {
            this.id = id; this.name = name; this.age = age;
        }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }

    /**
     * Test subclass that overrides multiGetHashes to use individual hashOps.entries()
     * calls instead of pipeline, avoiding mock issues with executePipelined.
     */
    static class TestableRedisRepository extends BaseRedisRepository<User, String> {
        private final HashOperations<String, Object, Object> hashOps;

        TestableRedisRepository(StringRedisTemplate redisTemplate,
                                RedisRepositoryConfig<User, String> config,
                                HashOperations<String, Object, Object> hashOps) {
            super(redisTemplate, config);
            this.hashOps = hashOps;
        }

        @Override
        protected List<Map<Object, Object>> fetchHashes(List<String> keyList) {
            if (keyList.isEmpty()) {
                return Collections.emptyList();
            }
            List<Map<Object, Object>> result = new ArrayList<>();
            for (String key : keyList) {
                Map<Object, Object> entries = hashOps.entries(key);
                result.add(entries == null || entries.isEmpty() ? null : entries);
            }
            return result;
        }

        @Override
        public org.plain.specification.core.visitor.IExpressionVisitor<User, ?> getVisitor() {
            throw new UnsupportedOperationException("Redis test stub does not support expression pushdown");
        }

        @Override
        public List<User> execute(ISpecification<User> specification) {
            return new ArrayList<>(findRange(specification));
        }

        @Override
        public IPageResult<User> execute(ISpecification<User> specification, PageQuery pageQuery) {
            return page(specification, pageQuery);
        }

        @Override
        public Set<Class<? extends org.plain.specification.core.expression.IExpression<User>>> supportedExpressions() {
            return Collections.emptySet();
        }
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        doReturn(hashOps).when(redisTemplate).opsForHash();

        RedisRepositoryConfig<User, String> config = new RedisRepositoryConfig.Builder<User, String>()
            .resolvedZsetKey("users:zset")
            .resolvedHashKeyPrefix("user:")
            .serializer(DefaultStrategies.jacksonSerializer())
            .deserializer(DefaultStrategies.jacksonDeserializer(User.class))
            .idExtractor(DefaultStrategies.reflectionIdExtractor(User.class, null))
            .scoreProvider(DefaultStrategies.defaultScoreProvider())
            .ttlProvider(DefaultStrategies.defaultTtlProvider())
            .fieldExtractor(entity -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", entity.id);
                map.put("name", entity.name);
                map.put("age", String.valueOf(entity.age));
                return map;
            })
            .entityBuilder(map -> {
                User user = new User();
                user.id = String.valueOf(map.get("id"));
                user.name = String.valueOf(map.get("name"));
                user.age = Integer.parseInt(String.valueOf(map.get("age")));
                return user;
            })
            .build();
        repo = new TestableRedisRepository(redisTemplate, config, hashOps);
    }

    private void mockHashEntries(String id, String name, int age) {
        Map<Object, Object> hash = new HashMap<>();
        hash.put("id", id);
        hash.put("name", name);
        hash.put("age", String.valueOf(age));
        when(hashOps.entries("user:" + id)).thenReturn(hash);
    }

    // --- count() tests ---

    @Test
    void count_withNoWhereFilters_shouldUseZCARD() {
        when(zSetOps.size("users:zset")).thenReturn(42L);

        long count = repo.count(new Specification<>());

        assertEquals(42L, count);
        verify(zSetOps).size("users:zset");
        verify(zSetOps, never()).range(anyString(), anyLong(), anyLong());
    }

    @Test
    void count_withNoWhereFilters_shouldReturnZeroWhenSizeIsNull() {
        when(zSetOps.size("users:zset")).thenReturn(null);

        long count = repo.count(new Specification<>());

        assertEquals(0L, count);
    }

    @Test
    void count_withWhereFilters_shouldFallBackToBatchScan() {
        ISpecification<User> spec = Specification.where(User::getAge).gt(18).build();

        when(zSetOps.range("users:zset", 0L, 99L))
            .thenReturn(new LinkedHashSet<>(Arrays.asList("1", "2")));
        when(zSetOps.range("users:zset", 100L, 199L))
            .thenReturn(Collections.emptySet());

        mockHashEntries("1", "Alice", 25);
        mockHashEntries("2", "Bob", 16);

        long count = repo.count(spec);

        assertEquals(1L, count);
        verify(zSetOps).range("users:zset", 0L, 99L);
    }

    // --- page() optimized path tests ---

    @Test
    void page_withNoWhereFilters_shouldUseOptimizedPath() {
        when(zSetOps.size("users:zset")).thenReturn(50L);

        Set<String> pageIds = new LinkedHashSet<>(Arrays.asList("11", "12", "13"));
        when(zSetOps.range("users:zset", 3L, 5L)).thenReturn(pageIds);

        mockHashEntries("11", "User11", 20);
        mockHashEntries("12", "User12", 21);
        mockHashEntries("13", "User13", 22);

        ISpecification<User> spec = new Specification<>();
        PageQuery pageQuery = new PageQuery(2, 3);

        IPageResult<User> result = repo.page(spec, pageQuery);

        assertNotNull(result);
        assertEquals(50L, result.getTotal());
        assertEquals(2L, result.getPage());
        assertEquals(3L, result.getPageSize());
        assertEquals(3, result.getRecords().size());
        assertTrue(result.hasNext());
        assertTrue(result.hasPrevious());

        verify(zSetOps).size("users:zset");
        verify(zSetOps).range("users:zset", 3L, 5L);
    }

    @Test
    void page_withNoWhereFilters_offsetBeyondTotal_shouldReturnEmpty() {
        when(zSetOps.size("users:zset")).thenReturn(5L);

        ISpecification<User> spec = new Specification<>();
        PageQuery pageQuery = new PageQuery(10, 3);

        IPageResult<User> result = repo.page(spec, pageQuery);

        assertNotNull(result);
        assertEquals(5L, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
        assertFalse(result.hasNext());
        assertTrue(result.hasPrevious());

        verify(zSetOps).size("users:zset");
        verify(zSetOps, never()).range(eq("users:zset"), anyLong(), anyLong());
    }

    @Test
    void page_withNoWhereFilters_firstPage_shouldCalculateCorrectOffset() {
        when(zSetOps.size("users:zset")).thenReturn(100L);

        Set<String> pageIds = new LinkedHashSet<>(Arrays.asList("1", "2", "3", "4", "5"));
        when(zSetOps.range("users:zset", 0L, 4L)).thenReturn(pageIds);

        mockHashEntries("1", "User1", 20);
        mockHashEntries("2", "User2", 21);
        mockHashEntries("3", "User3", 22);
        mockHashEntries("4", "User4", 23);
        mockHashEntries("5", "User5", 24);

        ISpecification<User> spec = new Specification<>();
        PageQuery pageQuery = new PageQuery(1, 5);

        IPageResult<User> result = repo.page(spec, pageQuery);

        assertEquals(100L, result.getTotal());
        assertEquals(5, result.getRecords().size());
        assertEquals(20L, result.getPages());
        assertTrue(result.hasNext());
        assertFalse(result.hasPrevious());

        verify(zSetOps).range("users:zset", 0L, 4L);
    }

    // --- page() fallback path tests ---

    @Test
    void page_withWhereFilters_shouldFallBackToFullScan() {
        ISpecification<User> spec = Specification.where(User::getAge).gt(18).build();

        when(zSetOps.range("users:zset", 0L, 99L))
            .thenReturn(new LinkedHashSet<>(Arrays.asList("1", "2", "3")));
        when(zSetOps.range("users:zset", 100L, 199L))
            .thenReturn(Collections.emptySet());

        mockHashEntries("1", "Alice", 25);
        mockHashEntries("2", "Bob", 16);
        mockHashEntries("3", "Charlie", 30);

        PageQuery pageQuery = new PageQuery(1, 2);
        IPageResult<User> result = repo.page(spec, pageQuery);

        assertNotNull(result);
        assertEquals(2L, result.getTotal());
        assertEquals(2, result.getRecords().size());
        assertFalse(result.hasNext());

        verify(zSetOps, never()).size(anyString());
    }
}
