package org.plain.specification.core.evaluate;

import org.plain.specification.core.ISpecification;

import java.util.Arrays;
import java.util.Collection;

/**
 * Class InMemorySpecificationEvaluator.
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
        this.evaluators = Arrays.asList(WhereEvaluator.INSTANCE);
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
