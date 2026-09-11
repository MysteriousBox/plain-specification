package org.plain.specification.core.evaluate;

import org.plain.specification.core.IPageResult;
import org.plain.specification.core.ISpecification;
import org.plain.specification.core.PageQuery;
import org.plain.specification.core.descriptor.IExpressionDescriptor;
import org.plain.specification.core.expression.IExpression;
import org.plain.specification.core.expression.OrderExpressionInfo;
import org.plain.specification.core.spi.ISpecificationExecutor;
import org.plain.specification.core.visitor.AbstractExpressionVisitor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 混合执行策略：优先将查询下推到存储后端，不支持时降级为内存过滤。
 * <p>
 * 工作流程：
 * <ol>
 *   <li>遍历 Specification 中所有 Where 表达式，收集表达式类型</li>
 *   <li>与 {@link ISpecificationExecutor#supportedExpressions()} 对比</li>
 *   <li>全部支持 → 委托执行器完成查询（含分页、排序）</li>
 *   <li>存在不支持 → 从执行器获取全量数据，使用 {@link org.plain.specification.core.visitor.PredicateExpressionVisitor} 内存过滤</li>
 * </ol>
 * </p>
 * <p>
 * 各存储后端 Repository 可直接使用本类作为默认执行策略，
 * 也可调用 {@link #canPushDown(ISpecificationExecutor, ISpecification)} 自行决策。
 * </p>
 *
 * @param <T> 实体类型
 * @author Jayden.Liang
 */
public class HybridSpecificationEvaluator<T> implements ISpecificationEvaluator {

    private final ISpecificationExecutor<T> executor;

    public HybridSpecificationEvaluator(ISpecificationExecutor<T> executor) {
        if (executor == null) {
            throw new IllegalArgumentException("executor must not be null");
        }
        this.executor = executor;
    }

    @Override
    public <E> Collection<E> evaluate(Collection<E> entities, ISpecification<E> specification) {
        @SuppressWarnings("unchecked")
        ISpecificationExecutor<E> exec = (ISpecificationExecutor<E>) executor;

        if (canPushDown(exec, specification)) {
            return exec.execute((ISpecification<E>) specification);
        }
        return WhereEvaluator.INSTANCE.evaluate(entities, specification);
    }

    /**
     * 通过执行器执行查询（分页）。仅在可以下推时使用。
     *
     * @param specification 查询规范
     * @param pageQuery     分页参数
     * @return 分页结果
     * @throws UnsupportedOperationException 当表达式无法完全下推时
     */
    public IPageResult<T> executePaged(ISpecification<T> specification, PageQuery pageQuery) {
        if (canPushDown(executor, specification)) {
            return executor.execute(specification, pageQuery);
        }
        throw new UnsupportedOperationException(
                "Cannot push down query to backend; paged execution requires full backend support");
    }

    /**
     * 判断执行器是否能完全处理该 Specification 的所有表达式。
     *
     * @param executor      存储后端执行器
     * @param specification 查询规范
     * @param <T>           实体类型
     * @return true 表示可以完全下推
     */
    public static <T> boolean canPushDown(ISpecificationExecutor<T> executor,
                                          ISpecification<T> specification) {
        Set<Class<? extends IExpression<T>>> supported = executor.supportedExpressions();
        if (supported == null || supported.isEmpty()) {
            return false;
        }
        Set<Class<? extends IExpression<T>>> used = collectExpressionTypes(specification);
        return supported.containsAll(used);
    }

    /**
     * 收集 Specification 中使用到的所有表达式类型（递归遍历表达式树）。
     */
    private static <T> Set<Class<? extends IExpression<T>>> collectExpressionTypes(
            ISpecification<T> specification) {
        Set<Class<? extends IExpression<T>>> types = new HashSet<>();
        ExpressionTypeCollector<T> collector = new ExpressionTypeCollector<>(types);

        for (IExpressionDescriptor<T> descriptor : specification.getWhereExpressions()) {
            descriptor.func(collector);
        }
        for (OrderExpressionInfo<T> orderInfo : specification.getOrderExpressions()) {
            orderInfo.func(collector);
        }
        return types;
    }

    /**
     * 表达式类型收集器。遍历表达式树，将每个节点的实际类型记录到集合中。
     */
    private static class ExpressionTypeCollector<T> extends AbstractExpressionVisitor<T, Boolean> {

        private final Set<Class<? extends IExpression<T>>> types;

        ExpressionTypeCollector(Set<Class<? extends IExpression<T>>> types) {
            this.types = types;
        }

        @SuppressWarnings("unchecked")
        private Boolean record(IExpression<T> expr) {
            types.add((Class<? extends IExpression<T>>) expr.getClass());
            return Boolean.TRUE;
        }

        @Override
        public Boolean visitAnd(org.plain.specification.core.expression.AndExpression<T> expression) {
            record(expression);
            expression.getLeft().accept(this);
            expression.getRight().accept(this);
            return Boolean.TRUE;
        }

        @Override
        public Boolean visitOr(org.plain.specification.core.expression.OrExpression<T> expression) {
            record(expression);
            expression.getLeft().accept(this);
            expression.getRight().accept(this);
            return Boolean.TRUE;
        }

        @Override
        public Boolean visitNot(org.plain.specification.core.expression.NotExpression<T> expression) {
            record(expression);
            expression.getExpression().accept(this);
            return Boolean.TRUE;
        }

        @Override
        public <V extends Comparable<V>> Boolean visitEqual(org.plain.specification.core.expression.EqualExpression<T, V> expression) {
            return record(expression);
        }

        @Override
        public <V extends Comparable<V>> Boolean visitNotEqual(org.plain.specification.core.expression.NotEqualExpression<T, V> expression) {
            return record(expression);
        }

        @Override
        public <V extends Comparable<V>> Boolean visitGt(org.plain.specification.core.expression.GreaterThanExpression<T, V> expression) {
            return record(expression);
        }

        @Override
        public <V extends Comparable<V>> Boolean visitLt(org.plain.specification.core.expression.LessThanExpression<T, V> expression) {
            return record(expression);
        }

        @Override
        public <V extends Comparable<V>> Boolean visitGte(org.plain.specification.core.expression.GreaterThanOrEqualExpression<T, V> expression) {
            return record(expression);
        }

        @Override
        public <V extends Comparable<V>> Boolean visitLte(org.plain.specification.core.expression.LessThanOrEqualExpression<T, V> expression) {
            return record(expression);
        }

        @Override
        public <V extends Comparable<V>> Boolean visitIn(org.plain.specification.core.expression.InExpression<T, V> expression) {
            return record(expression);
        }

        @Override
        public <V extends Comparable<V>> Boolean visitNot(org.plain.specification.core.expression.NotInExpression<T, V> expression) {
            return record(expression);
        }

        @Override
        public <V extends Comparable<V>> Boolean visitBetween(org.plain.specification.core.expression.BetweenExpression<T, V> expression) {
            return record(expression);
        }

        @Override
        public Boolean visitLike(org.plain.specification.core.expression.LikeExpression<T> expression) {
            return record(expression);
        }

        @Override
        public <V> Boolean visitIsNull(org.plain.specification.core.expression.IsNullExpression<T, V> expression) {
            return record(expression);
        }

        @Override
        public <V> Boolean visitIsNotNull(org.plain.specification.core.expression.IsNotNullExpression<T, V> expression) {
            return record(expression);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <E> Boolean visitExists(org.plain.specification.core.expression.ExistsExpression<T, E> expression) {
            record(expression);
            expression.getRight().accept(new ExpressionTypeCollector<>((Set<Class<? extends IExpression<E>>>) (Set<?>) types));
            return Boolean.TRUE;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <E> Boolean visitNotExists(org.plain.specification.core.expression.NotExistsExpression<T, E> expression) {
            record(expression);
            expression.getExpression().accept(new ExpressionTypeCollector<>((Set<Class<? extends IExpression<E>>>) (Set<?>) types));
            return Boolean.TRUE;
        }

        @Override
        public <V extends Comparable<V>> Boolean visitOrder(org.plain.specification.core.expression.OrderExpression<T, V> expression) {
            return record(expression);
        }
    }
}
