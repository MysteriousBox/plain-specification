package org.plain.specification.redis.autoconfig;

import org.plain.specification.redis.*;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * RedisRepositoryFactory
 * <p>
 * 设计模式说明：
 * <ul>
 *   <li>全局配置（globalConfig）集中管理所有通用默认配置。</li>
 *   <li>Resolver 负责根据全局配置和可选的自定义覆盖项，组装出最终可用的 Repository 配置。</li>
 *   <li>本工厂只负责“组装”和“交付”最终可用的 Repository，所有配置组装逻辑都交给 Resolver。</li>
 *   <li>Repository 只依赖最终配置对象，专注业务逻辑，不关心配置组装。</li>
 * </ul>
 * 这样实现了配置解耦、职责清晰、易于扩展和维护。
 *
 * @author Jayden.Liang
 */
public class RedisRepositoryFactory {

    private final StringRedisTemplate redisTemplate;
    private final RedisRepositoryConfigResolver resolver;

    /**
     * 推荐构造器：注入已初始化的 Resolver（如通过 Spring auto-config 注入共享 Resolver bean）。
     * 工厂本身不做任何配置组装，只负责交付最终 Repository。
     */
    public RedisRepositoryFactory(StringRedisTemplate redisTemplate,
                                  RedisRepositoryConfigResolver resolver) {
        this.redisTemplate = redisTemplate;
        this.resolver = resolver;
    }

    /**
     * 兼容构造器：接收全局 config 并基于它创建内部 resolver（向后兼容旧用法）。
     * 推荐在新项目中直接注入 Resolver。
     */
    public RedisRepositoryFactory(StringRedisTemplate redisTemplate,
                                  RedisRepositoryConfig<Object, Object> globalConfig) {
        this(redisTemplate, new RedisRepositoryConfigResolver(globalConfig));
    }

    /**
     * 创建指定实体类型和 zSetKey 的 Repository。
     * 组装逻辑全部交给 resolver，工厂只负责交付。
     */
    public <T, TID> GenericRedisRepository<T, TID> create(Class<T> entityClass, String zSetKey) {
        RedisRepositoryConfig<T, TID> cfg = resolver.resolve(entityClass, zSetKey, null);
        return new GenericRedisRepository<>(redisTemplate, cfg);
    }

    /**
     * 创建指定实体类型的 Repository，使用默认 zSetKey。
     */
    public <T, TID> GenericRedisRepository<T, TID> create(Class<T> entityClass) {
        return create(entityClass, null);
    }
}
