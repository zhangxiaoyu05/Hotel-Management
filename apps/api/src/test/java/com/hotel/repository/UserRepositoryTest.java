package com.hotel.repository;

import com.hotel.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User createTestUser(String username, String email, String status) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPhoneNumber("1380013800" + username.charAt(username.length() - 1));
        user.setPassword("password");
        user.setStatus(status);
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLoginAt(LocalDateTime.now());
        return user;
    }

    @Test
    void testCountByStatus() {
        // 创建测试数据
        User activeUser1 = createTestUser("activeuser1", "active1@test.com", "ACTIVE");
        User activeUser2 = createTestUser("activeuser2", "active2@test.com", "ACTIVE");
        User inactiveUser = createTestUser("inactiveuser", "inactive@test.com", "INACTIVE");

        entityManager.persist(activeUser1);
        entityManager.persist(activeUser2);
        entityManager.persist(inactiveUser);
        entityManager.flush();

        // 测试
        long activeCount = userRepository.countByStatus("ACTIVE");
        long inactiveCount = userRepository.countByStatus("INACTIVE");

        assertEquals(2, activeCount);
        assertEquals(1, inactiveCount);
    }

    @Test
    void testCountNewUsersThisMonth() {
        // 创建本月新用户
        LocalDateTime thisMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        User newUser1 = createTestUser("newuser1", "new1@test.com", "ACTIVE");
        newUser1.setCreatedAt(thisMonth.plusDays(1));

        User newUser2 = createTestUser("newuser2", "new2@test.com", "ACTIVE");
        newUser2.setCreatedAt(thisMonth.plusDays(5));

        // 创建上月用户
        LocalDateTime lastMonth = thisMonth.minusMonths(1);
        User oldUser = createTestUser("olduser", "old@test.com", "ACTIVE");
        oldUser.setCreatedAt(lastMonth);

        entityManager.persist(newUser1);
        entityManager.persist(newUser2);
        entityManager.persist(oldUser);
        entityManager.flush();

        // 测试
        long newUsersCount = userRepository.countNewUsersThisMonth();

        assertEquals(2, newUsersCount);
    }

    @Test
    void testFindAllWithSpecification() {
        // 创建测试数据
        User user1 = createTestUser("testuser1", "test1@example.com", "ACTIVE");
        User user2 = createTestUser("testuser2", "test2@example.com", "INACTIVE");
        User user3 = createTestUser("otheruser", "other@example.com", "ACTIVE");

        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.persist(user3);
        entityManager.flush();

        // 创建查询规范
        Specification<User> spec = Specification.where((root, query, criteriaBuilder) ->
            criteriaBuilder.like(root.get("username"), "%test%")
        );

        Pageable pageable = PageRequest.of(0, 10);
        Page<User> result = userRepository.findAll(spec, pageable);

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream()
            .allMatch(user -> user.getUsername().contains("test")));
    }

    @Test
    void testFindAllByIdIn() {
        // 创建测试数据
        User user1 = createTestUser("user1", "user1@test.com", "ACTIVE");
        User user2 = createTestUser("user2", "user2@test.com", "ACTIVE");
        User user3 = createTestUser("user3", "user3@test.com", "ACTIVE");

        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.persist(user3);
        entityManager.flush();

        // 测试
        List<Long> userIds = List.of(user1.getId(), user3.getId());
        List<User> result = userRepository.findAllById(userIds);

        assertEquals(2, result.size());
        assertTrue(result.stream()
            .anyMatch(user -> user.getUsername().equals("user1")));
        assertTrue(result.stream()
            .anyMatch(user -> user.getUsername().equals("user3")));
        assertTrue(result.stream()
            .noneMatch(user -> user.getUsername().equals("user2")));
    }

    @Test
    void testSaveAll() {
        // 创建测试数据
        List<User> users = List.of(
            createTestUser("batchuser1", "batch1@test.com", "ACTIVE"),
            createTestUser("batchuser2", "batch2@test.com", "ACTIVE"),
            createTestUser("batchuser3", "batch3@test.com", "ACTIVE")
        );

        // 保存
        List<User> savedUsers = userRepository.saveAll(users);

        // 验证
        assertEquals(3, savedUsers.size());
        savedUsers.forEach(user -> assertNotNull(user.getId()));

        // 从数据库查询验证
        List<User> dbUsers = userRepository.findAll();
        assertEquals(3, dbUsers.size());
    }

    @Test
    void testFindById() {
        // 创建测试数据
        User user = createTestUser("finduser", "find@test.com", "ACTIVE");
        entityManager.persist(user);
        entityManager.flush();

        // 测试
        java.util.Optional<User> found = userRepository.findById(user.getId());

        assertTrue(found.isPresent());
        assertEquals("finduser", found.get().getUsername());
        assertEquals("find@test.com", found.get().getEmail());
    }

    @Test
    void testCount() {
        // 创建测试数据
        entityManager.persist(createTestUser("countuser1", "count1@test.com", "ACTIVE"));
        entityManager.persist(createTestUser("countuser2", "count2@test.com", "ACTIVE"));
        entityManager.flush();

        // 测试
        long count = userRepository.count();

        assertEquals(2, count);
    }

    @Test
    void testExistsById() {
        // 创建测试数据
        User user = createTestUser("existsuser", "exists@test.com", "ACTIVE");
        entityManager.persist(user);
        entityManager.flush();

        // 测试
        assertTrue(userRepository.existsById(user.getId()));
        assertFalse(userRepository.existsById(999L));
    }

    @Test
    void testDeleteById() {
        // 创建测试数据
        User user = createTestUser("deleteuser", "delete@test.com", "ACTIVE");
        entityManager.persist(user);
        entityManager.flush();

        Long userId = user.getId();
        assertTrue(userRepository.existsById(userId));

        // 删除
        userRepository.deleteById(userId);

        // 验证删除
        assertFalse(userRepository.existsById(userId));
    }
}