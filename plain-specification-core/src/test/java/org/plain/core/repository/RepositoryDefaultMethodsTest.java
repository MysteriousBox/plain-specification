package org.plain.core.repository;

import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.plain.specification.core.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryDefaultMethodsTest {

    @Getter
    private static class User {
        String name;
        int age;

        User(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    private static class SimplePageResult<T> implements IPageResult<T> {
        private static final long serialVersionUID = 1L;
        private final Collection<T> records;
        private final long page;
        private final long pageSize;
        private final long total;

        SimplePageResult(Collection<T> records, long page, long pageSize, long total) {
            this.records = records;
            this.page = page;
            this.pageSize = pageSize;
            this.total = total;
        }

        @Override
        public Long getPageSize() { return pageSize; }

        @Override
        public Long getPage() { return page; }

        @Override
        public Long getTotal() { return total; }

        @Override
        public Long getPages() { return (total + pageSize - 1) / pageSize; }

        @Override
        public Collection<T> getRecords() { return records; }

        @Override
        public Boolean hasPrevious() { return page > 1; }

        @Override
        public Boolean hasNext() { return page < getPages(); }
    }

    private static class TestRepository implements IBaseRepository<User, String> {
        private final List<User> store = new ArrayList<>();
        Executor asyncExecutor;

        TestRepository(User... users) {
            store.addAll(Arrays.asList(users));
        }

        @Override
        public Executor getAsyncExecutor() {
            return asyncExecutor != null ? asyncExecutor : ForkJoinPool.commonPool();
        }

        @Override
        public User findById(String id) {
            return store.stream().filter(u -> u.getName().equals(id)).findFirst().orElse(null);
        }

        @Override
        public User findOne(ISpecification<User> specification) {
            return store.stream().filter(u -> specification.isSatisfiedBy(u)).findFirst().orElse(null);
        }

        @Override
        public Collection<User> findRange(ISpecification<User> specification) {
            List<User> result = new ArrayList<>();
            for (User u : store) {
                if (specification.isSatisfiedBy(u)) result.add(u);
            }
            return result;
        }

        @Override
        public long count(ISpecification<User> specification) {
            return findRange(specification).size();
        }

        @Override
        public IPageResult<User> page(ISpecification<User> specification, PageQuery pageQuery) {
            List<User> filtered = new ArrayList<>();
            for (User u : store) {
                if (specification.isSatisfiedBy(u)) filtered.add(u);
            }
            int total = filtered.size();
            int fromIndex = (pageQuery.getPage() - 1) * pageQuery.getPageSize();
            int toIndex = Math.min(fromIndex + pageQuery.getPageSize(), total);
            List<User> pageData = fromIndex < total ? filtered.subList(fromIndex, toIndex) : Collections.emptyList();
            return new SimplePageResult<>(pageData, pageQuery.getPage(), pageQuery.getPageSize(), total);
        }

        @Override
        public User save(User entity) {
            store.add(entity);
            return entity;
        }

        @Override
        public void update(User entity) {
            // no-op for test
        }

        @Override
        public void deleteById(String id) {
            store.removeIf(u -> u.getName().equals(id));
        }

        @Override
        public void deleteByIds(Collection<String> ids) {
            store.removeIf(u -> ids.contains(u.getName()));
        }

        @Override
        public void delete(User entity) {
            store.removeIf(u -> u.getName().equals(entity.getName()));
        }
    }

    private TestRepository repo;

    @BeforeEach
    void setUp() {
        repo = new TestRepository(
            new User("Alice", 25),
            new User("Bob", 35),
            new User("Charlie", 30)
        );
    }

    // --- IReadBaseRepository defaults ---

    @Test
    void getAsyncExecutor_default_shouldReturnCommonPool() {
        TestRepository emptyRepo = new TestRepository();
        assertSame(ForkJoinPool.commonPool(), emptyRepo.getAsyncExecutor());
    }

    @Test
    void findByIdAsync_shouldReturnEntity() throws Exception {
        CompletableFuture<User> future = repo.findByIdAsync("Alice");
        User result = future.get();
        assertNotNull(result);
        assertEquals("Alice", result.getName());
    }

    @Test
    void findByIdAsync_shouldReturnNull_whenNotFound() throws Exception {
        CompletableFuture<User> future = repo.findByIdAsync("Unknown");
        assertNull(future.get());
    }

    @Test
    void findOneAsync_shouldReturnEntity() throws Exception {
        ISpecification<User> spec = Specification.where(User::getAge).eq(35).build();
        CompletableFuture<User> future = repo.findOneAsync(spec);
        User result = future.get();
        assertNotNull(result);
        assertEquals("Bob", result.getName());
    }

    @Test
    void findRangeAsync_shouldReturnMatching() throws Exception {
        ISpecification<User> spec = Specification.where(User::getAge).gt(28).build();
        CompletableFuture<Collection<User>> future = repo.findRangeAsync(spec);
        Collection<User> result = future.get();
        assertEquals(2, result.size());
    }

    @Test
    void countAsync_shouldReturnCount() throws Exception {
        ISpecification<User> spec = Specification.where(User::getAge).gt(0).build();
        CompletableFuture<Long> future = repo.countAsync(spec);
        assertEquals(3L, future.get());
    }

    @Test
    void pageAsync_shouldReturnPagedResult() throws Exception {
        ISpecification<User> spec = Specification.where(User::getAge).gt(0).build();
        CompletableFuture<IPageResult<User>> future = repo.pageAsync(spec, new PageQuery(1, 2));
        IPageResult<User> result = future.get();
        assertEquals(Long.valueOf(3), result.getTotal());
        assertEquals(Long.valueOf(1), result.getPage());
        assertEquals(Long.valueOf(2), result.getPageSize());
        assertEquals(2, result.getRecords().size());
        assertTrue(result.hasNext());
        assertFalse(result.hasPrevious());
    }

    @Test
    void findAll_shouldReturnAll() {
        Collection<User> all = repo.findAll();
        assertEquals(3, all.size());
    }

    @Test
    void findAllAsync_shouldReturnAll() throws Exception {
        CompletableFuture<Collection<User>> future = repo.findAllAsync();
        assertEquals(3, future.get().size());
    }

    @Test
    void exists_shouldReturnTrue_whenMatch() {
        ISpecification<User> spec = Specification.where(User::getAge).eq(25).build();
        assertTrue(repo.exists(spec));
    }

    @Test
    void exists_shouldReturnFalse_whenNoMatch() {
        ISpecification<User> spec = Specification.where(User::getAge).eq(99).build();
        assertFalse(repo.exists(spec));
    }

    @Test
    void existsAsync_shouldReturnResult() throws Exception {
        ISpecification<User> spec = Specification.where(User::getAge).gt(0).build();
        assertTrue(repo.existsAsync(spec).get());
    }

    @Test
    void findOptional_shouldReturnOptional_whenFound() {
        ISpecification<User> spec = Specification.where(User::getAge).eq(30).build();
        Optional<User> result = repo.findOptional(spec);
        assertTrue(result.isPresent());
        assertEquals("Charlie", result.get().getName());
    }

    @Test
    void findOptional_shouldReturnEmpty_whenNotFound() {
        ISpecification<User> spec = Specification.where(User::getAge).eq(99).build();
        assertFalse(repo.findOptional(spec).isPresent());
    }

    @Test
    void findOptionalAsync_shouldReturnResult() throws Exception {
        ISpecification<User> spec = Specification.where(User::getAge).eq(25).build();
        Optional<User> result = repo.findOptionalAsync(spec).get();
        assertTrue(result.isPresent());
    }

    // --- IBaseRepository defaults ---

    @Test
    void saveAsync_shouldSaveAndReturn() throws Exception {
        CompletableFuture<User> future = repo.saveAsync(new User("David", 40));
        User saved = future.get();
        assertEquals("David", saved.getName());
        assertEquals(4, repo.store.size());
    }

    @Test
    void updateAsync_shouldComplete() throws Exception {
        User user = new User("Alice", 26);
        CompletableFuture<Void> future = repo.updateAsync(user);
        future.get();
    }

    @Test
    void deleteAsync_shouldDelete() throws Exception {
        CompletableFuture<Void> future = repo.deleteAsync(new User("Bob", 35));
        future.get();
        assertEquals(2, repo.store.size());
    }

    @Test
    void saveRangeAsync_shouldSaveAll() throws Exception {
        List<User> newUsers = Arrays.asList(new User("D1", 40), new User("D2", 41));
        CompletableFuture<Collection<User>> future = repo.saveRangeAsync(newUsers);
        Collection<User> saved = future.get();
        assertEquals(2, saved.size());
        assertEquals(5, repo.store.size());
    }

    @Test
    void updateRangeAsync_shouldComplete() throws Exception {
        List<User> users = Arrays.asList(new User("Alice", 26), new User("Bob", 36));
        CompletableFuture<Void> future = repo.updateRangeAsync(users);
        future.get();
    }

    @Test
    void deleteRangeAsync_collection_shouldDeleteAll() throws Exception {
        List<User> toDelete = Arrays.asList(new User("Alice", 25), new User("Bob", 35));
        CompletableFuture<Void> future = repo.deleteRangeAsync(toDelete);
        future.get();
        assertEquals(1, repo.store.size());
    }

    @Test
    void deleteRangeAsync_specification_shouldDeleteMatching() throws Exception {
        ISpecification<User> spec = Specification.where(User::getAge).gt(28).build();
        CompletableFuture<Void> future = repo.deleteRangeAsync(spec);
        future.get();
        assertEquals(1, repo.store.size());
        assertEquals("Alice", repo.store.get(0).getName());
    }

    @Test
    void customExecutor_shouldBeUsed() throws Exception {
        Thread[] captured = new Thread[1];
        Executor customExecutor = runnable -> {
            Thread t = new Thread(runnable);
            t.setName("test-executor-thread");
            captured[0] = t;
            t.start();
        };
        repo.asyncExecutor = customExecutor;

        repo.findByIdAsync("Alice").get();
        assertNotNull(captured[0]);
        assertEquals("test-executor-thread", captured[0].getName());
    }
}
