package org.plain.specification.core.validator;

import org.plain.specification.core.ISpecification;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Class SpecificationValidator.
 *
 * @author Jayden.Liang
 */


public class SpecificationValidator implements ISpecificationValidator{

    public static final SpecificationValidator DEFAULT = new SpecificationValidator();

    private final Collection<IValidator> validators;

    public SpecificationValidator() {
        validators = new ArrayList<>();
        validators.add(WhereValidator.instance());
    }

    public SpecificationValidator(Collection<IValidator> validators) {
        this.validators = validators;
    }

    protected Collection<IValidator> getValidators() {
        return validators;
    }

    @Override
    public <T> Boolean isValid(T entity, ISpecification<T> specification) {
        for (IValidator validator : getValidators()) {
            if (!validator.isValid(entity, specification)) {
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }
}
