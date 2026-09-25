package org.plain.specification.example.service;

import org.plain.specification.core.IPageResult;
import org.plain.specification.core.ISpecification;
import org.plain.specification.core.PageQuery;
import org.plain.specification.core.Specification;
import org.plain.specification.core.builder.QuickSpecificationBuilder;
import org.plain.specification.example.entity.User;
import org.plain.specification.example.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 演示 plain-specification 的各种用法。
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ========== 快速 API（Quick Specification） ==========

    /**
     * 单条件查询：age > 18
     */
    public Collection<User> findAdults() {
        ISpecification<User> spec = Specification.where(User::getAge).gt(18).build();
        return userRepository.findRange(spec);
    }

    /**
     * AND 组合：age > 18 AND name LIKE '张%'
     */
    public Collection<User> findAdultsWithName(String namePrefix) {
        ISpecification<User> spec = Specification.where(User::getAge).gt(18)
                .and(User::getName).like(namePrefix)
                .build();
        return userRepository.findRange(spec);
    }

    /**
     * OR 组合：age < 18 OR age > 60
     */
    public Collection<User> findSpecialAgeGroups() {
        ISpecification<User> spec = Specification.where(User::getAge).lt(18)
                .or(User::getAge).gt(60)
                .build();
        return userRepository.findRange(spec);
    }

    /**
     * IN 查询：age IN (18, 25, 30)
     */
    public Collection<User> findBySpecificAges() {
        ISpecification<User> spec = Specification.where(User::getAge).in(18, 25, 30).build();
        return userRepository.findRange(spec);
    }

    /**
     * BETWEEN 查询：age BETWEEN 20 AND 30
     */
    public Collection<User> findAgeBetween(int min, int max) {
        ISpecification<User> spec = Specification.where(User::getAge).between(min, max).build();
        return userRepository.findRange(spec);
    }

    /**
     * NULL 检查：email IS NOT NULL
     */
    public Collection<User> findWithEmail() {
        ISpecification<User> spec = Specification.where(User::getEmail).isNotNull().build();
        return userRepository.findRange(spec);
    }

    // ========== 排序 ==========

    /**
     * 排序：按 age 降序
     */
    public Collection<User> findAllOrderByAgeDesc() {
        ISpecification<User> spec = new QuickSpecificationBuilder<User>(new Specification<>())
                .orderByDescending(User::getAge)
                .build();
        return userRepository.findRange(spec);
    }

    /**
     * 多字段排序：按 age 降序，再按 name 升序
     */
    public Collection<User> findAllOrderByAgeDescNameAsc() {
        ISpecification<User> spec = new QuickSpecificationBuilder<User>(new Specification<>())
                .orderByDescending(User::getAge)
                .thenBy(User::getName)
                .build();
        return userRepository.findRange(spec);
    }

    /**
     * 过滤 + 排序组合
     */
    public Collection<User> findAdultsOrderByAgeDesc() {
        ISpecification<User> spec = Specification.where(User::getAge).gt(18)
                .orderByDescending(User::getAge)
                .build();
        return userRepository.findRange(spec);
    }

    // ========== 分页 ==========

    /**
     * 分页查询
     */
    public IPageResult<User> findAdultsByPage(int page, int pageSize) {
        ISpecification<User> spec = Specification.where(User::getAge).gt(18).build();
        return userRepository.page(spec, new PageQuery(page, pageSize));
    }

    /**
     * 分页 + 排序
     */
    public IPageResult<User> findAdultsByPageWithOrder(int page, int pageSize) {
        ISpecification<User> spec = Specification.where(User::getAge).gt(18)
                .orderByDescending(User::getAge)
                .build();
        return userRepository.page(spec, new PageQuery(page, pageSize));
    }

    // ========== 便捷方法 ==========

    /**
     * 判断是否存在
     */
    public boolean hasAdults() {
        ISpecification<User> spec = Specification.where(User::getAge).gt(18).build();
        return userRepository.exists(spec);
    }

    /**
     * Optional 查找
     */
    public Optional<User> findFirstAdult(String name) {
        ISpecification<User> spec = Specification.where(User::getName).eq(name)
                .and(User::getAge).gt(18)
                .build();
        return userRepository.findOptional(spec);
    }

    /**
     * 统计数量
     */
    public long countAdults() {
        ISpecification<User> spec = Specification.where(User::getAge).gt(18).build();
        return userRepository.count(spec);
    }

    // ========== 异步操作 ==========

    /**
     * 异步查找
     */
    public CompletableFuture<User> findUserByIdAsync(Long id) {
        return userRepository.findByIdAsync(id);
    }

    /**
     * 异步查询成年用户
     */
    public CompletableFuture<Collection<User>> findAdultsAsync() {
        ISpecification<User> spec = Specification.where(User::getAge).gt(18).build();
        return userRepository.findRangeAsync(spec);
    }

    /**
     * 异步分页
     */
    public CompletableFuture<IPageResult<User>> findAdultsByPageAsync(int page, int pageSize) {
        ISpecification<User> spec = Specification.where(User::getAge).gt(18).build();
        return userRepository.pageAsync(spec, new PageQuery(page, pageSize));
    }

    // ========== 查询全部 ==========

    public Collection<User> findAll() {
        return userRepository.findAll();
    }
}
