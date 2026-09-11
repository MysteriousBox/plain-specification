package org.plain.specification.core.spi;

import org.plain.specification.core.IPageResult;
import org.plain.specification.core.ISpecification;
import org.plain.specification.core.PageQuery;
import org.plain.specification.core.expression.IExpression;
import org.plain.specification.core.visitor.IExpressionVisitor;

import java.util.List;
import java.util.Set;

/**
 * 存储后端执行器 SPI 接口。
 * <p>
 * 每个存储后端（MyBatis-Plus、Redis、Elasticsearch 等）实现此接口，
 * 声明自己能处理哪些表达式类型，并提供将表达式树编译为目标查询格式的 Visitor。
 * </p>
 * <p>
 * 新增存储后端只需：
 * <ol>
 *   <li>实现 {@link IExpressionVisitor} 将表达式编译为目标查询格式</li>
 *   <li>实现本接口执行查询</li>
 *   <li>实现 Repository 基类对外暴露</li>
 * </ol>
 * </p>
 *
 * @param <T> 实体类型
 * @author Jayden.Liang
 */
public interface ISpecificationExecutor<T> {

    /**
     * 返回此执行器的表达式 Visitor，用于将表达式树编译为目标查询格式。
     *
     * @return 表达式 Visitor
     */
    IExpressionVisitor<T, ?> getVisitor();

    /**
     * 执行查询，返回所有匹配结果。
     *
     * @param specification 查询规范
     * @return 匹配结果列表
     */
    List<T> execute(ISpecification<T> specification);

    /**
     * 执行分页查询。
     *
     * @param specification 查询规范
     * @param pageQuery     分页参数
     * @return 分页结果
     */
    IPageResult<T> execute(ISpecification<T> specification, PageQuery pageQuery);

    /**
     * 声明此执行器能下推处理的表达式类型集合。
     * <p>
     * 不在集合中的表达式将自动降级为内存过滤。
     * 例如 Redis 执行器可能不支持 {@code LikeExpression}，
     * 则 like 查询会在内存中完成过滤。
     * </p>
     *
     * @return 支持的表达式类型集合
     */
    Set<Class<? extends IExpression<T>>> supportedExpressions();
}
