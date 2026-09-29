package org.plain.specification.core.builder;

import org.plain.specification.core.ISpecification;
import org.plain.specification.core.OrderTypeEnum;
import org.plain.specification.core.Specification;
import org.plain.specification.core.expression.Expressions;
import org.plain.specification.core.expression.OrderExpressionInfo;
import org.plain.specification.core.expression.WhereExpressionInfo;
import org.plain.specification.core.visitor.OrderExpressionVisitor;
import org.plain.specification.core.visitor.PredicateExpressionVisitor;

/**
 * Default implementation of specification builder.
 *
 * @param <T> entity type
 * @author Jayden.Liang
 */
public class SpecificationBuilder<T> implements IOrderedSpecificationBuilder<T> {

    protected final Specification<T> specification;

    private boolean chainDiscarded = false;

    public SpecificationBuilder(Specification<T> specification) {
        this.specification = specification;
    }

    @Override
    public ISpecification<T> getSpecification() {
        return this.specification;
    }

    @Override
    public ISpecificationBuilder<T> where(Expressions<T> expression) {
        return where(expression, true);
    }

    @Override
    public ISpecificationBuilder<T> where(Expressions<T> expression, boolean condition) {
        if (condition && (expression == null || !expression.isEmpty())) {
            WhereExpressionInfo<T> whereExpressionInfo = new WhereExpressionInfo<>(expression,
                    new PredicateExpressionVisitor<>());
            specification.add(whereExpressionInfo);
        }
        return this;
    }

    @Override
    public IOrderedSpecificationBuilder<T> orderBy(Expressions<T> expression) {
        return orderBy(expression, true);
    }

    @Override
    public IOrderedSpecificationBuilder<T> orderBy(Expressions<T> expression, boolean condition) {
        if (condition) {
            OrderExpressionInfo<T> orderExpressionInfo = new OrderExpressionInfo<>(expression, OrderTypeEnum.ORDER_BY,
                    new OrderExpressionVisitor<>());
            specification.add(orderExpressionInfo);
        }
        this.chainDiscarded = !condition;
        return this;
    }

    @Override
    public IOrderedSpecificationBuilder<T> orderByDescending(Expressions<T> expression) {
        return orderByDescending(expression, true);
    }

    @Override
    public IOrderedSpecificationBuilder<T> orderByDescending(Expressions<T> expression, boolean condition) {
        if (condition) {
            OrderExpressionInfo<T> orderExpressionInfo = new OrderExpressionInfo<>(expression,
                    OrderTypeEnum.ORDER_BY_DESCENDING, new OrderExpressionVisitor<>());
            specification.add(orderExpressionInfo);
        }
        this.chainDiscarded = !condition;
        return this;
    }

    @Override
    public IOrderedSpecificationBuilder<T> thenBy(Expressions<T> expression) {
        return thenBy(expression, true);
    }

    @Override
    public IOrderedSpecificationBuilder<T> thenBy(Expressions<T> expression, boolean condition) {
        if (condition && !this.chainDiscarded) {
            OrderExpressionInfo<T> orderExpressionInfo = new OrderExpressionInfo<>(expression, OrderTypeEnum.THEN_BY,
                    new OrderExpressionVisitor<>());
            specification.add(orderExpressionInfo);
        } else {
            this.chainDiscarded = true;
        }
        return this;
    }

    @Override
    public IOrderedSpecificationBuilder<T> thenByDescending(Expressions<T> expression) {
        return thenByDescending(expression, true);
    }

    @Override
    public IOrderedSpecificationBuilder<T> thenByDescending(Expressions<T> expression, boolean condition) {
        if (condition && !this.chainDiscarded) {
            OrderExpressionInfo<T> orderExpressionInfo = new OrderExpressionInfo<>(expression,
                    OrderTypeEnum.THEN_BY_DESCENDING, new OrderExpressionVisitor<>());
            specification.add(orderExpressionInfo);
        } else {
            this.chainDiscarded = true;
        }
        return this;
    }
}
