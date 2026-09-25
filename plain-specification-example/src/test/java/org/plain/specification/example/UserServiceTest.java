package org.plain.specification.example;

import org.junit.jupiter.api.Test;
import org.plain.specification.core.IPageResult;
import org.plain.specification.example.entity.User;
import org.plain.specification.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    // ========== 快速 API ==========

    @Test
    void findAdults_shouldReturnUsersOver18() {
        Collection<User> adults = userService.findAdults();
        assertFalse(adults.isEmpty());
        adults.forEach(u -> assertTrue(u.getAge() > 18));
    }

    @Test
    void findAdultsWithName_shouldFilterByNamePrefix() {
        Collection<User> result = userService.findAdultsWithName("张");
        assertFalse(result.isEmpty());
        result.forEach(u -> {
            assertTrue(u.getAge() > 18);
            assertTrue(u.getName().startsWith("张"));
        });
    }

    @Test
    void findSpecialAgeGroups_shouldReturnUnder18OrOver60() {
        Collection<User> result = userService.findSpecialAgeGroups();
        assertFalse(result.isEmpty());
        result.forEach(u -> assertTrue(u.getAge() < 18 || u.getAge() > 60));
    }

    @Test
    void findBySpecificAges_shouldReturnMatchingUsers() {
        Collection<User> result = userService.findBySpecificAges();
        assertFalse(result.isEmpty());
        result.forEach(u -> assertTrue(u.getAge() == 18 || u.getAge() == 25 || u.getAge() == 30));
    }

    @Test
    void findAgeBetween_shouldReturnUsersInRange() {
        Collection<User> result = userService.findAgeBetween(20, 35);
        assertFalse(result.isEmpty());
        result.forEach(u -> assertTrue(u.getAge() >= 20 && u.getAge() <= 35));
    }

    @Test
    void findWithEmail_shouldReturnUsersHavingEmail() {
        Collection<User> result = userService.findWithEmail();
        assertFalse(result.isEmpty());
        result.forEach(u -> assertNotNull(u.getEmail()));
    }

    // ========== 排序 ==========

    @Test
    void findAllOrderByAgeDesc_shouldSortCorrectly() {
        Collection<User> result = userService.findAllOrderByAgeDesc();
        assertEquals(6, result.size());
        User prev = null;
        for (User u : result) {
            if (prev != null) {
                assertTrue(prev.getAge() >= u.getAge());
            }
            prev = u;
        }
    }

    @Test
    void findAdultsOrderByAgeDesc_shouldFilterAndSort() {
        Collection<User> result = userService.findAdultsOrderByAgeDesc();
        assertFalse(result.isEmpty());
        User prev = null;
        for (User u : result) {
            assertTrue(u.getAge() > 18);
            if (prev != null) {
                assertTrue(prev.getAge() >= u.getAge());
            }
            prev = u;
        }
    }

    // ========== 分页 ==========

    @Test
    void findAdultsByPage_shouldReturnPagedResult() {
        IPageResult<User> page1 = userService.findAdultsByPage(1, 2);
        assertNotNull(page1);
        assertTrue(page1.getTotal() > 0);
        assertEquals(1L, page1.getPage());
        assertEquals(2L, page1.getPageSize());
        assertTrue(page1.getRecords().size() <= 2);
        assertTrue(page1.hasNext());
        assertFalse(page1.hasPrevious());
    }

    @Test
    void findAdultsByPage_secondPage_shouldHavePrevious() {
        IPageResult<User> page2 = userService.findAdultsByPage(2, 2);
        assertNotNull(page2);
        assertTrue(page2.hasPrevious());
    }

    // ========== 便捷方法 ==========

    @Test
    void hasAdults_shouldReturnTrue() {
        assertTrue(userService.hasAdults());
    }

    @Test
    void findFirstAdult_shouldReturnMatchingUser() {
        Optional<User> result = userService.findFirstAdult("张三");
        assertTrue(result.isPresent());
        assertEquals("张三", result.get().getName());
        assertTrue(result.get().getAge() > 18);
    }

    @Test
    void findFirstAdult_nonExistent_shouldReturnEmpty() {
        Optional<User> result = userService.findFirstAdult("不存在");
        assertFalse(result.isPresent());
    }

    @Test
    void countAdults_shouldReturnCorrectCount() {
        long count = userService.countAdults();
        assertTrue(count > 0);
    }

    // ========== 异步 ==========

    @Test
    void findUserByIdAsync_shouldReturnUser() throws Exception {
        CompletableFuture<User> future = userService.findUserByIdAsync(1L);
        User user = future.get();
        assertNotNull(user);
        assertEquals(1L, user.getId());
    }

    @Test
    void findAdultsAsync_shouldReturnResults() throws Exception {
        CompletableFuture<Collection<User>> future = userService.findAdultsAsync();
        Collection<User> result = future.get();
        assertFalse(result.isEmpty());
    }

    @Test
    void findAdultsByPageAsync_shouldReturnPagedResult() throws Exception {
        CompletableFuture<IPageResult<User>> future = userService.findAdultsByPageAsync(1, 3);
        IPageResult<User> result = future.get();
        assertNotNull(result);
        assertTrue(result.getTotal() > 0);
    }

    // ========== 查询全部 ==========

    @Test
    void findAll_shouldReturnAllUsers() {
        Collection<User> all = userService.findAll();
        assertEquals(6, all.size());
    }
}
