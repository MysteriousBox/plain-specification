package org.plain.specification.core.evaluate;

import org.plain.specification.core.ISpecification;
import org.plain.specification.core.descriptor.IExpressionDescriptor;
import org.plain.specification.core.visitor.PredicateExpressionVisitor;

import java.util.Collection;
import java.util.stream.Collectors;

public class WhereEvaluator implements IEvaluator{

    public static final WhereEvaluator INSTANCE = new WhereEvaluator();

    private final Boolean isCriteriaEvaluator = Boolean.FALSE;

    private WhereEvaluator() {
    }

    @Override
    public Boolean isCriteriaEvaluator() {
        return isCriteriaEvaluator;
    }

    @Override
    public <T> Collection<T> evaluate(Collection<T> entities, ISpecification<T> specification) {
        for (IExpressionDescriptor<T> whereExpressionInfo : specification.getWhereExpressions()) {
            entities = entities.stream().filter(whereExpressionInfo.func(new PredicateExpressionVisitor<>())).collect(Collectors.toList());
        }
        return entities;
    }
}
