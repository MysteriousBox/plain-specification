package org.plain.specification.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.plain.utils.JsonUtil;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.HashOperations;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GenericRedisRepositoryTest {

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

    static class User {
        public String id;
        public String name;

        public User() {}

        public User(String id, String name) {
            this.id = id; this.name = name;
        }

        public String getId(){ return id; }
        public void setId(String id){ this.id = id; }
        public String getName(){ return name; }
        public void setName(String name){ this.name = name; }
    }

    @Test
    @SuppressWarnings({"unchecked", "null"})
    void testSaveAndFindById() {
        RedisRepositoryConfig<User, String> config = new RedisRepositoryConfig.Builder<User, String>()
            .resolvedZsetKey("users:zset")
            .resolvedHashKeyPrefix("user:")
            .serializer(DefaultStrategies.jacksonSerializer())
            .deserializer(DefaultStrategies.jacksonDeserializer(User.class))
            .idExtractor(DefaultStrategies.reflectionIdExtractor(User.class, null))
            .scoreProvider(DefaultStrategies.defaultScoreProvider())
            .ttlProvider(DefaultStrategies.defaultTtlProvider())
            .fieldExtractor(entity -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", entity.id);
                map.put("name", entity.name);
                return map;
            }) // mock
            .entityBuilder(map -> {
                User user = new User();
                user.id = JsonUtil.deserialize((String) ((Map<?, ?>)map).get("id"), String.class);
                user.name = JsonUtil.deserialize((String) ((Map<?, ?>)map).get("name"), String.class);
                return user;
            }) // mock entityBuilder
            .build();
        GenericRedisRepository<User, String> repo = new GenericRedisRepository<>(redisTemplate, config);
        User u = new User("1","Alice");

        // mock ZADD and SETEX: we can't assert internal execute of script easily; verify that no exception
        doReturn(null).when(valueOps).get(anyString());
        Map<String, String> entries = new java.util.HashMap<>();
        entries.put("id", "\"1\"");
        entries.put("name", "\"Alice\"");
        when(hashOps.entries(anyString())).thenReturn(entries);
        when(zSetOps.range("users:zset", 0L, 0L)).thenReturn(Collections.singleton("1"));
        String userJson = JsonUtil.serialize(u);
        when(valueOps.multiGet(ArgumentMatchers.<java.util.Collection<String>>any())).thenReturn(Collections.singletonList(userJson));
        when(redisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(), ArgumentMatchers.<java.util.List<String>>any(), ArgumentMatchers.<Object[]>any())).thenReturn(1L); // mock execute to return success

        User saved = repo.save(u);
        assertNotNull(saved);
        verify(redisTemplate, atLeastOnce()).execute(ArgumentMatchers.<RedisScript<Long>>any(), ArgumentMatchers.<java.util.List<String>>any(), ArgumentMatchers.<Object[]>any());

        User found = repo.findById("1");
        assertNotNull(found);
        assertEquals("Alice", found.getName());
    }

    @Test
    @SuppressWarnings({"unchecked", "null"})
    void testDeleteById() {
        RedisRepositoryConfig<User, String> config = new RedisRepositoryConfig.Builder<User, String>()
            .resolvedZsetKey("users:zset")
            .resolvedHashKeyPrefix("user:")
            .serializer(DefaultStrategies.jacksonSerializer())
            .deserializer(DefaultStrategies.jacksonDeserializer(User.class))
            .idExtractor(DefaultStrategies.reflectionIdExtractor(User.class, null))
            .scoreProvider(DefaultStrategies.defaultScoreProvider())
            .ttlProvider(DefaultStrategies.defaultTtlProvider())
            .fieldExtractor(entity -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", entity.id);
                map.put("name", entity.name);
                return map;
            }) // mock
            .entityBuilder(map -> {
                User user = new User();
                user.id = JsonUtil.deserialize((String) ((Map<?, ?>)map).get("id"), String.class);
                user.name = JsonUtil.deserialize((String) ((Map<?, ?>)map).get("name"), String.class);
                return user;
            }) // mock entityBuilder
            .build();
        GenericRedisRepository<User, String> repo = new GenericRedisRepository<>(redisTemplate, config);
        when(redisTemplate.execute((RedisScript<Long>) any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(1L); // mock execute to return success
        repo.deleteById("1");
        // delete uses redisTemplate.execute with lua script
        verify(redisTemplate, atLeastOnce()).execute(ArgumentMatchers.<RedisScript<Long>>any(), ArgumentMatchers.<List<String>>any(), ArgumentMatchers.<Object[]>any());
    }
}
