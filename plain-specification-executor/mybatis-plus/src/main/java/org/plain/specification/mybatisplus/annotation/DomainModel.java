package org.plain.specification.mybatisplus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as associated with a domain model type for MyBatis-Plus integration.
 *
 * <p>Use this annotation on types that act as specification executors or mappers
 * to indicate the corresponding domain model class they operate on.</p>
 *
 * @author Jayden.Liang
 * @since 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DomainModel {

    /**
     * Returns the domain model class associated with the annotated type.
     *
     * <p>Example: {@code @DomainModel(User.class)}</p>
     *
     * @return the domain model {@link Class} this type is bound to
     */
    Class<?> value();
}