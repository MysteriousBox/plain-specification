package org.plain.specification.core;


import org.plain.specification.core.builder.ISpecificationBuilder;
import org.plain.specification.core.builder.SpecificationBuilder;
import org.plain.specification.core.descriptor.IExpressionDescriptor;
import org.plain.specification.core.evaluate.ISpecificationEvaluator;
import org.plain.specification.core.evaluate.InMemorySpecificationEvaluator;
import org.plain.specification.core.expression.OrderExpressionInfo;
import org.plain.specification.core.validator.ISpecificationValidator;
import org.plain.specification.core.validator.SpecificationValidator;
import org.plain.specification.core.visitor.IExpressionVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Class Specification.
 *
 * @author Jayden.Liang
 */
public class Specification<T> implements ISpecification<T>{

    private static final int DEFAULT_CAPACITY_WHERE = 2;

    private final ISpecificationBuilder<T> query= new SpecificationBuilder<>(this);

    private static final ISpecificationValidator VALIDATOR = SpecificationValidator.DEFAULT;

    private UnaryOperator<Collection<T>> postProcessingAction;

    private ISpecificationEvaluator evaluator = InMemorySpecificationEvaluator.DEFAULT;

    protected void setEvaluator(ISpecificationEvaluator evaluator) {
        this.evaluator = evaluator;
    }
    protected ISpecificationEvaluator getEvaluator() {
        return evaluator;
    }

    protected ISpecificationValidator getValidator() {
        return VALIDATOR;
    }

    private List<IExpressionDescriptor<T>> whereDescriptors;

    private List<OrderExpressionInfo<T>> orderExpressions;

    public void add(IExpressionDescriptor<T> whereDescriptor) {
        if (whereDescriptors == null){
            whereDescriptors = new ArrayList<>();
        }
        whereDescriptors.add(whereDescriptor);
    }

    public void add(OrderExpressionInfo<T> orderExpressionInfo) {
        if (orderExpressions == null){
            orderExpressions = new ArrayList<>(DEFAULT_CAPACITY_WHERE);
        }
        orderExpressions.add(orderExpressionInfo);
    }

    @Override
    public <R> List<R> selectCompiler(IExpressionVisitor<T,R> visitor){
        if (whereDescriptors == null) {
            return new ArrayList<>();
        }
        return whereDescriptors.stream().map(descriptor -> descriptor.func(visitor)).collect(Collectors.toList());
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
        return VALIDATOR.isValid(entity, this);
    }

    protected void setPostProcessingAction(UnaryOperator<Collection<T>> postProcessingAction) {
        this.postProcessingAction = postProcessingAction;
    }

    @Override
    public UnaryOperator<Collection<T>> postProcessingAction() {
        return this.postProcessingAction;
    }

    @Override
    public Iterable<IExpressionDescriptor<T>> getWhereExpressions() {
        return whereDescriptors==null? new ArrayList<>(DEFAULT_CAPACITY_WHERE):whereDescriptors;
    }

    @Override
    public Iterable<OrderExpressionInfo<T>> getOrderExpressions() {
        return orderExpressions==null? new ArrayList<>(DEFAULT_CAPACITY_WHERE):orderExpressions;
    }


}
