package org.plain.specification.redis;

import org.plain.specification.core.IBaseRepository;
import org.plain.specification.core.IPageResult;
import org.plain.specification.core.ISpecification;
import org.plain.specification.core.PageQuery;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

public abstract class RedisBaseRepository<T, TID> implements IBaseRepository<T, TID> {


    private final StringRedisTemplate redisTemplate;

    private static final RedisScript<Long> LUA_SAVE_ATOMIC_SCRIPT = RedisScript.of(
            "redis.call('ZADD', KEYS[1], ARGV[1], ARGV[2])\n" +
                    "redis.call('SETEX', KEYS[2], ARGV[4], ARGV[3])\n" +
                    "return 1", Long.class);

    private static final RedisScript<Long> LUA_DELETE_ATOMIC_SCRIPT = RedisScript.of(
            "redis.call('ZREM', KEYS[1], ARGV[1])\n" +
                    "redis.call('DEL', KEYS[2])\n" +
                    "return 1", Long.class);

    private static final RedisScript<Long> LUA_DELETE_BATCH_ATOMIC_SCRIPT = RedisScript.of(
            "redis.call('ZREM', KEYS[1], unpack(ARGV))\n" +
                    "for i = 2, #KEYS do\n" +
                    "    redis.call('DEL', KEYS[i])\n" +
                    "end\n" +
                    "return 1", Long.class);

    protected RedisBaseRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public T save(T entity) {
        return saveAtomic(entity);
    }

    private T saveAtomic(T entity) {

        String key = zSetKey();
        double score = getScore(entity);
        String id = getId(entity).toString();
        String serializedEntity = serialize(entity);
        String setexKey = getKey(id);
        long ttl = getTtl(entity) == null ? 3600L : getTtl(entity);
        List<String> keyList=Arrays.asList(key, setexKey);
        Object[] args = { String.valueOf(score), id, serializedEntity,String.valueOf(ttl)};
        redisTemplate.execute(LUA_SAVE_ATOMIC_SCRIPT,keyList,args);
        return entity;
    }

    @Override
    public CompletableFuture<T> saveAsync(T entity) {
        return CompletableFuture.supplyAsync(() -> {
            saveAtomic(entity);
            return entity;
        });
    }

    @Override
    public CompletableFuture<Collection<T>> saveRangeAsync(Collection<T> entities) {
        return CompletableFuture.supplyAsync(() -> {
            for (T entity : entities) {
                saveAtomic(entity);
            }
            return entities;
        });
    }

    @Override
    public void update(T entity) {
        saveAtomic(entity);
    }

    @Override
    public CompletableFuture<Void> updateAsync(T entity) {
        return CompletableFuture.runAsync(() -> {
            saveAtomic(entity);
        });
    }

    @Override
    public CompletableFuture<Void> updateRangeAsync(Collection<T> entities) {
        return CompletableFuture.runAsync(() -> {
            entities.forEach(this::saveAtomic);
        });
    }

    @Override
    public void deleteById(TID id) {
        String zSetKey = zSetKey();
        String key = getKey(String.valueOf(id));
        redisTemplate.execute(LUA_DELETE_ATOMIC_SCRIPT,Arrays.asList(zSetKey, key),id);
    }

    @Override
    public void deleteByIds(Collection<TID> ids) {
        String zSetKey = zSetKey();
        List<String> keys = new ArrayList<>();
        keys.add(zSetKey);
        List<String> idStrings = new ArrayList<>();
        for (TID id : ids) {
            String idStr = String.valueOf(id);
            idStrings.add(idStr);
            keys.add(getKey(idStr));
        }
        redisTemplate.execute(LUA_DELETE_BATCH_ATOMIC_SCRIPT,keys,idStrings.toArray());
    }


    @Override
    public void delete(T entity) {
        deleteAtomic(entity);
    }

    public void deleteAtomic(T entity){
        String id=getId(entity).toString();
        String zSetKey = zSetKey();
        String key = getKey(String.valueOf(id));
        redisTemplate.execute(LUA_DELETE_ATOMIC_SCRIPT,Arrays.asList(zSetKey, key),id);
    }

    @Override
    public CompletableFuture<Void> deleteAsync(T entity) {

        return CompletableFuture.runAsync(()->{
            try{
                deleteAtomic(entity);
            }catch (Exception e){
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteRangeAsync(Collection<T> entities) {

        return CompletableFuture.runAsync(()->{
            try{
                entities.forEach(this::deleteAtomic);
            }catch (Exception e){
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteRangeAsync(ISpecification<T> specification) {
        return CompletableFuture.runAsync(()->{
            Collection<T> entities = findRange(specification);
            entities.forEach(this::deleteAtomic);
        });
    }

    @Override
    public T findById(TID id) {
        Object object = redisTemplate.opsForValue().get(getKey(String.valueOf(id)));
        if (object == null) {
            return null;
        }
        return deserialize(String.valueOf(object));
    }

    private String getKey(String id) {
        return setexKeyPrefix() + id;
    }

    @Override
    public  CompletableFuture<T> findByIdAsync(TID id) {
        return CompletableFuture.supplyAsync(()->{
            try{
                return findById(id);
            }catch (Exception e){
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public T findOne(ISpecification<T> specification) {
        String key = zSetKey();
        Set<String> ids = redisTemplate.opsForZSet().range(key, 0, -1);
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        List<String> idList = ids.stream().map(item->setexKeyPrefix()+item).collect(Collectors.toList());
        List<?> entities = redisTemplate.opsForValue().multiGet(idList);
        entities = entities!=null?entities:new ArrayList<>();
        return entities.stream().map(item->deserialize(String.valueOf(item))).filter(specification::isSatisfiedBy).findFirst().orElse(null);

    }

    @Override
    public CompletableFuture<T> findOneAsync(ISpecification<T> specification) {
        return CompletableFuture.supplyAsync(() -> findOne(specification));
    }

    @Override
    public Collection<T> findRange(ISpecification<T> specification) {
        String key = zSetKey();
        Set<?> ids = redisTemplate.opsForZSet().range(key, 0, -1);
        if (ids != null) {
            List<String> idList = ids.stream().map(item->setexKeyPrefix()+item).collect(Collectors.toList());
            List<?> entities = redisTemplate.opsForValue().multiGet(idList);
            entities = entities!=null?entities:new ArrayList<>();
            return entities.stream().map(item->deserialize(String.valueOf(item))).filter(specification::isSatisfiedBy).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public CompletableFuture<Collection<T>> findRangeAsync(ISpecification<T> specification) {
        return CompletableFuture.supplyAsync(() -> findRange(specification));
    }

    @Override
    public long count(ISpecification<T> specification) {
        String key = zSetKey();
        Set<String> range = redisTemplate.opsForZSet().range(key, 0, -1);
        if (range == null) {
            return 0;
        }
        return range.stream().map(this::deserialize).filter(specification::isSatisfiedBy).count();

    }

    @Override
    public CompletableFuture<Long> countAsync(ISpecification<T> specification) {

        return CompletableFuture.supplyAsync(() -> count(specification));
    }

    @Override
    public IPageResult<T> page(ISpecification<T> specification, PageQuery pageQuery) {
        if (pageQuery.getPage() < 1) {
            throw new IllegalArgumentException("Page number must be greater than 0");
        }
        if (pageQuery.getPageSize() < 1) {
            throw new IllegalArgumentException("Page size must be greater than 0");
        }

        String key = zSetKey();

        long start = (long) (pageQuery.getPage() - 1) * pageQuery.getPageSize();
        long end = start + pageQuery.getPageSize() - 1;
        Set<?> ids = redisTemplate.opsForZSet().range(key, start, end);
        if (ids == null|| ids.isEmpty()){
            return new RedisPageResultAdapter<>(Collections.emptyList(), pageQuery, 0L);
        }
        List<String> idList = ids.stream().map(item->setexKeyPrefix()+item).collect(Collectors.toList());
        List<?> entities = redisTemplate.opsForValue().multiGet(idList);
        entities = entities!=null?entities:new ArrayList<>();
        List<T> entityList = entities.stream().map(item->this.deserialize(String.valueOf(item))).filter(specification::isSatisfiedBy).collect(Collectors.toList());

        Long total = redisTemplate.opsForZSet().size(key);
        if (total == null) {
            total = 0L;
        }
        return new RedisPageResultAdapter<>(entityList, pageQuery, total);
    }

    protected abstract String serialize(T entity);

    protected abstract String zSetKey();

    protected abstract String setexKeyPrefix();

    protected abstract T deserialize(String serializedEntity);

    protected abstract TID getId(T entity);

    protected abstract double getScore(T entity);

    protected abstract Long getTtl(T entity);

}
