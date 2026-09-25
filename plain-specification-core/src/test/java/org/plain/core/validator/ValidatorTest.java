package org.plain.core.validator;

import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.plain.specification.core.ISpecification;
import org.plain.specification.core.Specification;
import org.plain.specification.core.expression.Expressions;
import org.plain.specification.core.validator.IValidator;
import org.plain.specification.core.validator.SpecificationValidator;
import org.plain.specification.core.validator.WhereValidator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorTest {

    @Getter
    private static class User {
        String name;
        int age;

        User(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    // --- WhereValidator ---

    @Test
    void whereValidator_instance_shouldReturnNewInstance() {
        WhereValidator v1 = WhereValidator.instance();
        WhereValidator v2 = WhereValidator.instance();
        assertNotSame(v1, v2);
    }

    @Test
    void whereValidator_isValid_shouldReturnTrue_whenNoExpressions() {
        Specification<User> spec = new Specification<>();
        assertTrue(WhereValidator.instance().isValid(new User("A", 20), spec));
    }

    @Test
    void whereValidator_isValid_shouldReturnTrue_whenEntityMatches() {
        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create().equal(User::getName, "Alice"));
        spec.query().where(Expressions.<User>create().greaterThan(User::getAge, 18));

        assertTrue(WhereValidator.instance().isValid(new User("Alice", 25), spec));
    }

    @Test
    void whereValidator_isValid_shouldReturnFalse_whenEntityDoesNotMatch() {
        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create().equal(User::getName, "Alice"));

        assertFalse(WhereValidator.instance().isValid(new User("Bob", 25), spec));
    }

    @Test
    void whereValidator_isValid_shouldShortCircuit_onFirstFailure() {
        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create().equal(User::getName, "Alice"));
        spec.query().where(Expressions.<User>create().greaterThan(User::getAge, 100));

        assertFalse(WhereValidator.instance().isValid(new User("Bob", 25), spec));
    }

    // --- SpecificationValidator ---

    @Test
    void specificationValidator_default_shouldIncludeWhereValidator() {
        Specification<User> spec = new Specification<>();
        spec.query().where(Expressions.<User>create().equal(User::getName, "Alice"));

        assertTrue(SpecificationValidator.DEFAULT.isValid(new User("Alice", 25), spec));
        assertFalse(SpecificationValidator.DEFAULT.isValid(new User("Bob", 25), spec));
    }

    @Test
    void specificationValidator_default_isValid_whenNoExpressions() {
        assertTrue(SpecificationValidator.DEFAULT.isValid(new User("A", 20), new Specification<>()));
    }

    @Test
    void specificationValidator_customValidators_shouldUseProvided() {
        IValidator alwaysTrue = new IValidator() {
            @Override
            public <T> Boolean isValid(T entity, ISpecification<T> specification) {
                return true;
            }
        };

        SpecificationValidator validator = new SpecificationValidator(Collections.singletonList(alwaysTrue));
        assertTrue(validator.isValid(new User("A", 20), new Specification<>()));
    }

    @Test
    void specificationValidator_customValidators_shouldReturnFalse_whenAnyFails() {
        IValidator alwaysTrue = new IValidator() {
            @Override
            public <T> Boolean isValid(T entity, ISpecification<T> specification) {
                return true;
            }
        };
        IValidator alwaysFalse = new IValidator() {
            @Override
            public <T> Boolean isValid(T entity, ISpecification<T> specification) {
                return false;
            }
        };

        Collection<IValidator> validators = new ArrayList<>();
        validators.add(alwaysTrue);
        validators.add(alwaysFalse);

        SpecificationValidator validator = new SpecificationValidator(validators);
        assertFalse(validator.isValid(new User("A", 20), new Specification<>()));
    }

    @Test
    void specificationValidator_emptyValidators_shouldReturnTrue() {
        SpecificationValidator validator = new SpecificationValidator(Collections.emptyList());
        assertTrue(validator.isValid(new User("A", 20), new Specification<>()));
    }
}
