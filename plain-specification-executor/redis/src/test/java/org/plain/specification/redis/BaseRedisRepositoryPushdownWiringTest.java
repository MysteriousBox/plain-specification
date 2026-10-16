package org.plain.specification.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.plain.specification.core.ISpecification;
import org.plain.specification.core.Specification;
import org.plain.specification.core.expression.EqualExpression;
import org.plain.specification.core.expression.IExpression;
import org.plain.specification.core.expression.IsNotNullExpression;
import org.plain.specification.core.expression.NotExpression;
import org.plain.specification.redis.pushdown.RedisNarrowingVisitor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Wiring coverage for the two SPI methods and the read-path dispatch, without a Redis server: which
 * capability is advertised, and which command stream each path issues.
 */
class BaseRedisRepositoryPushdownWiringTest {

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

    public static class User {
        private String id;
        private String name;

        public User() { }

        public User(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    private GenericRedisRepository<User, String> repository(boolean pushdown) {
        Map<Object, Object> alice = new LinkedHashMap<Object, Object>();
        alice.put("id", "1");
        alice.put("name", "Alice");
        doReturn(alice).when(hashOps).entries("user:1");
        doReturn(Collections.emptyMap()).when(hashOps).entries("user:2");

        // Both paths must be able to see the same two index entries: the batch scan reads them with
        // ZRANGE, so stubbing it here is what makes "never() called ZRANGE" a real assertion.
        when(zSetOps.range(anyString(), anyLong(), anyLong()))
                .thenReturn(new LinkedHashSet<String>(Arrays.asList("1", "2")));
        when(zSetOps.size(anyString())).thenReturn(2L);
        doReturn(Arrays.asList(alice, new LinkedHashMap<Object, Object>()))
                .when(redisTemplate).executePipelined(any(org.springframework.data.redis.core.SessionCallback.class));

        RedisRepositoryConfig<User, String> config = new RedisRepositoryConfig.Builder<User, String>()
                .resolvedZsetKey("user:index")
                .resolvedHashKeyPrefix("user:")
                .batchSize(10)
                .serializer(DefaultStrategies.jacksonSerializer())
                .deserializer(DefaultStrategies.jacksonDeserializer(User.class))
                .idExtractor(DefaultStrategies.reflectionIdExtractor(User.class, null))
                .scoreProvider(DefaultStrategies.defaultScoreProvider())
                .ttlProvider(DefaultStrategies.defaultTtlProvider())
                .fieldExtractor(entity -> {
                    Map<String, Object> fields = new LinkedHashMap<String, Object>();
                    fields.put("id", entity.getId());
                    fields.put("name", entity.getName());
                    return fields;
                })
                .entityBuilder(fields -> {
                    User user = new User();
                    user.setId(fields.get("id") == null ? null : String.valueOf(fields.get("id")));
                    user.setName(fields.get("name") == null ? null : String.valueOf(fields.get("name")));
                    return user;
                })
                .expressionPushdown(pushdown)
                .build();
        return new GenericRedisRepository<User, String>(redisTemplate, config);
    }

    /** Stubbed script result: the ids the narrowing could not rule out. */
    private void stubScriptResult(List<String> survivingIds) {
        doReturn(survivingIds).when(redisTemplate)
                .execute(ArgumentMatchers.<RedisScript<Object>>any(), ArgumentMatchers.<List<String>>any(),
                        ArgumentMatchers.<Object[]>any());
    }

    @Test
    void visitorIsAvailableInsteadOfThrowing() {
        GenericRedisRepository<User, String> repo = repository(false);
        assertTrue(repo.getVisitor() instanceof RedisNarrowingVisitor,
                "getVisitor() must return the narrowing compiler, not throw");
    }

    @Test
    void capabilitiesStaySilentByDefault() {
        Set<Class<? extends IExpression<User>>> none = repository(false).supportedExpressions();
        assertTrue(none.isEmpty(), "pushdown off must keep the advertised set empty");
    }

    @Test
    void capabilitiesListExactlyTheNarrowedTypes() {
        Set<Class<? extends IExpression<User>>> declared = repository(true).supportedExpressions();
        assertTrue(declared.contains(EqualExpression.class));
        assertFalse(declared.contains(NotExpression.class));
        assertFalse(declared.contains(IsNotNullExpression.class));
    }

    @Test
    void pushdownPathUsesTheScriptAndNotTheIndexRange() {
        GenericRedisRepository<User, String> repo = repository(true);
        stubScriptResult(Collections.singletonList("1"));

        List<User> found = repo.execute(Specification.where(User::getName).eq("Alice").build());

        assertEquals(1, found.size());
        assertEquals("Alice", found.get(0).getName());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> keys = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate, times(1)).execute(ArgumentMatchers.<RedisScript<Object>>any(), keys.capture(),
                ArgumentMatchers.<Object[]>any());
        assertEquals(Collections.singletonList("user:index"), keys.getValue());
        // the batch scan path would have called ZRANGE per batch; the scan window is inside the script
        verify(zSetOps, never()).range(anyString(), anyLong(), anyLong());
    }

    @Test
    void unfilterableSpecificationFallsBackToTheBatchScan() {
        GenericRedisRepository<User, String> repo = repository(true);

        // a float-backed getter cannot be narrowed, so no script may run at all
        ISpecification<User> notNarrowable = new Specification<User>().query()
                .where(org.plain.specification.core.expression.Expressions.<User>create()
                        .equal(u -> 1.0d, 1.0d))
                .getSpecification();
        repo.findRange(notNarrowable);
        verify(redisTemplate, times(1)).executePipelined(any(org.springframework.data.redis.core.SessionCallback.class));

        verify(redisTemplate, never()).execute(ArgumentMatchers.<RedisScript<Object>>any(),
                ArgumentMatchers.<List<String>>any(), ArgumentMatchers.<Object[]>any());
        verify(zSetOps, times(1)).range(anyString(), anyLong(), anyLong());
    }

    @Test
    void disabledPushdownNeverIssuesTheScript() {
        GenericRedisRepository<User, String> repo = repository(false);
        repo.findRange(Specification.where(User::getName).eq("Alice").build());
        verify(redisTemplate, times(1)).executePipelined(any(org.springframework.data.redis.core.SessionCallback.class));

        verify(redisTemplate, never()).execute(ArgumentMatchers.<RedisScript<Object>>any(),
                ArgumentMatchers.<List<String>>any(), ArgumentMatchers.<Object[]>any());
        verify(zSetOps, times(1)).range(anyString(), anyLong(), anyLong());
    }

    @Test
    void unfilteredReadsKeepUsingZcardAndIndexRange() {
        GenericRedisRepository<User, String> repo = repository(true);

        assertEquals(2L, repo.count(new Specification<User>()));

        verify(zSetOps, times(1)).size(anyString());
        verify(redisTemplate, never()).execute(ArgumentMatchers.<RedisScript<Object>>any(),
                ArgumentMatchers.<List<String>>any(), ArgumentMatchers.<Object[]>any());
    }
}
