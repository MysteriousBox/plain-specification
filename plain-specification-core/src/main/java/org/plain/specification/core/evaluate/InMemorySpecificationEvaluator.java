package org.plain.specification.core.evaluate;

import org.plain.specification.core.ISpecification;

import java.util.Arrays;
import java.util.Collection;

/**
 * 内存规格求值器，通过流式过滤和排序在内存中执行查询。
 *
 * @author Jayden.Liang
 */


public class InMemorySpecificationEvaluator implements ISpecificationEvaluator {

    public static final  InMemorySpecificationEvaluator DEFAULT = new InMemorySpecificationEvaluator();

    private final Collection<IEvaluator> evaluators;

    protected Collection<IEvaluator> getEvaluators() {
        return evaluators;
    }


    private InMemorySpecificationEvaluator() {
        this.evaluators = Arrays.asList(WhereEvaluator.INSTANCE, OrderEvaluator.INSTANCE);
    }

    public InMemorySpecificationEvaluator(Collection<IEvaluator> evaluators) {
        this.evaluators = evaluators;
    }

    @Override
    public <T> Collection<T> evaluate(Collection<T> entities, ISpecification<T> specification) {
        for (IEvaluator evaluator : getEvaluators()) {
            entities = evaluator.evaluate(entities, specification);
        }
        return specification.postProcessingAction() == null ? entities : specification.postProcessingAction().apply(entities);
    }
}
