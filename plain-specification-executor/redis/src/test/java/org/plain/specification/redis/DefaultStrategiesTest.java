package org.plain.specification.redis;

import lombok.var;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DefaultStrategiesTest {

    static class TestEntity {
        private Long id;
        private String uuid;

        public Long getId() {
            return id;
        }

        @SuppressWarnings("unused")
        public String getUuid() {
            return uuid;
        }

        public TestEntity(Long id, String uuid) {
            this.id = id;
            this.uuid = uuid;
        }
    }

    @Test
    void testEnterpriseKeyPrefixProvider_withTenant() {
        TenantProvider tenantProvider = () -> "tenantA";
        var provider = DefaultStrategies.enterpriseKeyPrefixProvider("prod", tenantProvider);
        String prefix = provider.provide(String.class, "plain");
        assertEquals("prod:tenantA:plain:String:", prefix);
    }

    @Test
    void testEnterpriseKeyPrefixProvider_noTenant() {
        TenantProvider tenantProvider = () -> null;
        var provider = DefaultStrategies.enterpriseKeyPrefixProvider("prod", tenantProvider);
        String prefix = provider.provide(String.class, "plain");
        assertEquals("prod:plain:String:", prefix);
    }

    @Test
    void testEnterpriseKeyPrefixProvider_noEnv() {
        TenantProvider tenantProvider = () -> "t2";
        var provider = DefaultStrategies.enterpriseKeyPrefixProvider(null, tenantProvider);
        String prefix = provider.provide(String.class, "plain");
        assertEquals("t2:plain:String:", prefix);
    }

    @Test
    void testReflectionIdExtractor_withNullEntity() {
        var extractor = DefaultStrategies.reflectionIdExtractor(TestEntity.class, null);
        assertNull(extractor.getId(null));
    }

    @Test
    void testReflectionIdExtractor_withGetter() {
        var extractor = DefaultStrategies.reflectionIdExtractor(TestEntity.class, "getId");
        TestEntity entity = new TestEntity(123L, "uuid123");
        assertEquals(123L, extractor.getId(entity));
    }

    @Test
    void testReflectionIdExtractor_withField() {
        var extractor = DefaultStrategies.reflectionIdExtractor(TestEntity.class, "id");
        TestEntity entity = new TestEntity(456L, "uuid456");
        assertEquals(456L, extractor.getId(entity));
    }

    @Test
    void testReflectionIdExtractor_withInvalidGetter_shouldThrow() {
        var extractor = DefaultStrategies.reflectionIdExtractor(TestEntity.class, "getNonExistent");
        TestEntity entity = new TestEntity(123L, "uuid123");
        Exception ex = assertThrows(IllegalStateException.class, () -> extractor.getId(entity));
        assertTrue(ex.getMessage().contains("getNonExistent"));
    }

    @Test
    void testReflectionIdExtractor_withInvalidField_shouldThrow() {
        var extractor = DefaultStrategies.reflectionIdExtractor(TestEntity.class, "nonExistentField");
        TestEntity entity = new TestEntity(123L, "uuid123");
        Exception ex = assertThrows(IllegalStateException.class, () -> extractor.getId(entity));
        assertTrue(ex.getMessage().contains("nonExistentField"));
    }

    @Test
    void testReflectionIdExtractor_defaultGetId() {
        var extractor = DefaultStrategies.reflectionIdExtractor(TestEntity.class, null);
        TestEntity entity = new TestEntity(789L, "uuid789");
        assertEquals(789L, extractor.getId(entity));
    }

    @Test
    void testReflectionIdExtractor_defaultGetUuid() {
        // Create a class without getId method
        @SuppressWarnings("unused")
        class NoGetIdEntity {
            private String uuid;

            public String getUuid() {
                return uuid;
            }

            public NoGetIdEntity(String uuid) {
                this.uuid = uuid;
            }
        }
        var extractor = DefaultStrategies.reflectionIdExtractor(NoGetIdEntity.class, null);
        NoGetIdEntity entity = new NoGetIdEntity("uuid999");
        assertEquals("uuid999", extractor.getId(entity));
    }

    @Test
    void testReflectionIdExtractor_defaultIdField() {
        // Create a class without getId or getUuid methods
        @SuppressWarnings("unused")
        class OnlyIdFieldEntity {
            private final Integer id;

            public OnlyIdFieldEntity(Integer id) {
                this.id = id;
            }
        }
        var extractor = DefaultStrategies.reflectionIdExtractor(OnlyIdFieldEntity.class, null);
        OnlyIdFieldEntity entity = new OnlyIdFieldEntity(111);
        assertEquals(111, extractor.getId(entity));
    }

    @Test
    void testReflectionIdExtractor_noIdFound() {
        // Create a class with no id-related fields or methods
        @SuppressWarnings("unused")
        class NoIdEntity {
            private final String name;

            public NoIdEntity(String name) {
                this.name = name;
            }
        }
        var extractor = DefaultStrategies.reflectionIdExtractor(NoIdEntity.class, null);
        NoIdEntity entity = new NoIdEntity("test");
        assertNull(extractor.getId(entity));
    }
}
