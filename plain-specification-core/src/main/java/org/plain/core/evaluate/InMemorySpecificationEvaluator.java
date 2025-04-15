package org.plain.core.evaluate;

import lombok.Getter;
import org.plain.core.ISpecification;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class InMemorySpecificationEvaluator implements ISpecificationEvaluator {

    public final static InMemorySpecificationEvaluator DEFAULT = new InMemorySpecificationEvaluator();

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
