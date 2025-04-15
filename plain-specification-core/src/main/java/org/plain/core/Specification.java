package org.plain.core;


import org.plain.core.evaluate.ISpecificationEvaluator;
import org.plain.core.evaluate.InMemorySpecificationEvaluator;
import org.plain.core.expression.Expressions;
import org.plain.core.expression.OrderExpressionInfo;
import org.plain.core.expression.WhereExpressionInfo;
import org.plain.core.validator.ISpecificationValidator;
import org.plain.core.validator.SpecificationValidator;
import org.plain.core.visitor.IExpressionVisitor;
import org.plain.core.visitor.OrderExpressionVisitor;
import org.plain.core.visitor.PredicateExpressionVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class Specification<T> implements ISpecification<T>{

    private final static int DEFAULT_CAPACITY_WHERE = 2;

    private final ISpecificationBuilder<T> query= new Builder<>(this);

    private final ISpecificationValidator validator = SpecificationValidator.DEFAULT;

    private Function<Collection<T>,Collection<T>> postProcessingAction;

    private ISpecificationEvaluator evaluator = InMemorySpecificationEvaluator.DEFAULT;

    private static final ThreadLocal<Boolean>  IsChainDiscarded = ThreadLocal.withInitial(() -> false);

    protected void setEvaluator(ISpecificationEvaluator evaluator) {
        this.evaluator = evaluator;
    }
    protected ISpecificationEvaluator getEvaluator() {
        return evaluator;
    }

    protected ISpecificationValidator getValidator() {
        return validator;
    }

    private List<WhereExpressionInfo<T>> whereExpressions;

    private List<OrderExpressionInfo<T>> orderExpressions;

    protected void add(WhereExpressionInfo<T> whereExpressionInfo) {
        if (whereExpressions == null){
            whereExpressions = new ArrayList<>(DEFAULT_CAPACITY_WHERE);
        }
        whereExpressions.add(whereExpressionInfo);
    }

    protected void add(OrderExpressionInfo<T> orderExpressionInfo) {
        if (orderExpressions == null){
            orderExpressions = new ArrayList<>(DEFAULT_CAPACITY_WHERE);
        }
        orderExpressions.add(orderExpressionInfo);
    }


    @Override
    public ISpecificationBuilder<T> query() {
        return query;
    }

    @Override
    public Collection<T> evaluate(Collection<T> entities) {
        return evaluator.evaluate(entities, this);
    }

    @Override
    public Boolean isSatisfiedBy(T entity) {
        return validator.isValid(entity, this);
    }

    protected void setPostProcessingAction(Function<Collection<T>, Collection<T>> postProcessingAction) {
        this.postProcessingAction = postProcessingAction;
    }

    @Override
    public Function<Collection<T>, Collection<T>> postProcessingAction() {
        return this.postProcessingAction;
    }

    @Override
    public Iterable<WhereExpressionInfo<T>> getWhereExpressionInfos() {
        return whereExpressions==null? new ArrayList<>(DEFAULT_CAPACITY_WHERE):whereExpressions;
    }

    @Override
    public Iterable<OrderExpressionInfo<T>> getOrderExpressionInfos() {
        return orderExpressions==null? new ArrayList<>(DEFAULT_CAPACITY_WHERE):orderExpressions;
    }

    public static void setIsChainDiscarded(Boolean isChainDiscarded) {
        IsChainDiscarded.set(isChainDiscarded);
    }

    public static Boolean getIsChainDiscarded() {
        return IsChainDiscarded.get();
    }

    public static void clearIsChainDiscarded() {
        IsChainDiscarded.remove();
    }

    private static class Builder<T> implements ISpecificationBuilder<T>, IOrderedSpecificationBuilder<T> {
        private final Specification<T> specification;


        private Builder(Specification<T> specification) {
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
}
