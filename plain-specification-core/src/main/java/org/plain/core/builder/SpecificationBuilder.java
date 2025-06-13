package org.plain.core.builder;

import org.plain.core.ISpecification;
import org.plain.core.OrderTypeEnum;
import org.plain.core.Specification;
import org.plain.core.expression.Expressions;
import org.plain.core.expression.OrderExpressionInfo;
import org.plain.core.expression.WhereExpressionInfo;
import org.plain.core.visitor.OrderExpressionVisitor;
import org.plain.core.visitor.PredicateExpressionVisitor;

public class SpecificationBuilder<T> implements ISpecificationBuilder<T>, IOrderedSpecificationBuilder<T>{

    protected final Specification<T> specification;

    public SpecificationBuilder(Specification<T> specification) {
        this.specification = specification;
    }

    @Override
    public ISpecification<T> getSpecification() {
        return this.specification;
    }

    @Override
    public ISpecificationBuilder<T> where(Expressions<T> expression) {
        return where(expression,true);
    }

    @Override
    public ISpecificationBuilder<T> where(Expressions<T> expression, Boolean condition) {
        if (condition){
            WhereExpressionInfo<T> whereExpressionInfo = new WhereExpressionInfo<>(expression,new PredicateExpressionVisitor<>());
            specification.add(whereExpressionInfo);
        }
        return this;
    }

    @Override
    public IOrderedSpecificationBuilder<T> orderBy(Expressions<T> expression) {
        return orderBy(expression,true);
    }

    @Override
    public IOrderedSpecificationBuilder<T> orderBy(Expressions<T> expression, Boolean condition) {
        if (condition){
            OrderExpressionInfo<T> orderExpressionInfo = new OrderExpressionInfo<>(expression, OrderTypeEnum.OrderBy, new OrderExpressionVisitor<>());
            specification.add(orderExpressionInfo);
        }
        Specification.setIsChainDiscarded(!condition);
        return this;
    }

    @Override
    public IOrderedSpecificationBuilder<T> orderByDescending(Expressions<T> expression) {
        return orderByDescending(expression,true);
    }

    @Override
    public IOrderedSpecificationBuilder<T> orderByDescending(Expressions<T> expression, Boolean condition) {
        if (condition){
            OrderExpressionInfo<T> orderExpressionInfo = new OrderExpressionInfo<>(expression, OrderTypeEnum.OrderByDescending, new OrderExpressionVisitor<>());
            specification.add(orderExpressionInfo);
        }
        Specification.setIsChainDiscarded(!condition);
        return this;
    }

    @Override
    public IOrderedSpecificationBuilder<T> thenBy(Expressions<T> expression) {
        return thenBy(expression,true);
    }

    @Override
    public IOrderedSpecificationBuilder<T> thenBy(Expressions<T> expression, Boolean condition) {
        if (condition&& !Specification.getIsChainDiscarded()){
            OrderExpressionInfo<T> orderExpressionInfo = new OrderExpressionInfo<>(expression, OrderTypeEnum.ThenBy, new OrderExpressionVisitor<>());
            specification.add(orderExpressionInfo);
        }else if (condition&& Specification.getIsChainDiscarded()){
            Specification.setIsChainDiscarded(true);
        }
        return this;
    }

    @Override
    public IOrderedSpecificationBuilder<T> thenByDescending(Expressions<T> expression) {
        return thenByDescending(expression,true);
    }

    @Override
    public IOrderedSpecificationBuilder<T> thenByDescending(Expressions<T> expression, Boolean condition) {
        if (condition&& !Specification.getIsChainDiscarded()){
            OrderExpressionInfo<T> orderExpressionInfo = new OrderExpressionInfo<>(expression, OrderTypeEnum.ThenByDescending, new OrderExpressionVisitor<>());
            specification.add(orderExpressionInfo);
        }else {
            Specification.setIsChainDiscarded(true);
        }
        return this;
    }
}
