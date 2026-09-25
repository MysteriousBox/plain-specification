package org.plain.specification.autoconfigure;

import org.plain.specification.mybatisplus.autoconfig.MybatisPlusAutoConfiguration;
import org.plain.specification.redis.autoconfig.PlainRedisAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 显式启用 plain-specification 自动配置。
 * <p>
 * 通常不需要手动添加此注解——Spring Boot 会自动发现并加载配置。
 * 仅在不使用 Spring Boot 自动发现机制时（如自定义配置类）才需要。
 * </p>
 *
 * <pre>
 * &#064;SpringBootApplication
 * &#064;EnableSpecification
 * public class Application {
 *     public static void main(String[] args) {
 *         SpringApplication.run(Application.class, args);
 *     }
 * }
 * </pre>
 *
 * @author Jayden.Liang
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({MybatisPlusAutoConfiguration.class, PlainRedisAutoConfiguration.class})
public @interface EnableSpecification {
}
