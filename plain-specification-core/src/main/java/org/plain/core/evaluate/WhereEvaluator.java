package org.plain.core.evaluate;

import org.plain.core.ISpecification;
import org.plain.core.expression.WhereExpressionInfo;

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
        for (WhereExpressionInfo<T> whereExpressionInfo : specification.getWhereExpressionInfos()) {
            entities = entities.stream().filter(whereExpressionInfo.predicate()).collect(Collectors.toList());
        }
        return entities;
    }
}
