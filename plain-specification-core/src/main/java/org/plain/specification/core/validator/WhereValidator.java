package org.plain.specification.core.validator;

import org.plain.specification.core.ISpecification;
import org.plain.specification.core.descriptor.IExpressionDescriptor;
import org.plain.specification.core.visitor.PredicateExpressionVisitor;

public class WhereValidator implements IValidator {

    private WhereValidator() {
    }

    public static WhereValidator instance() {
        return new WhereValidator();
    }

    @Override
    public <T> Boolean isValid(T entity, ISpecification<T> specification) {
        for (IExpressionDescriptor<T> whereExpressionInfo : specification.getWhereExpressions()) {
            if (!whereExpressionInfo.func(new PredicateExpressionVisitor<>()).test(entity)) {
                return false;
            }
        }
        return true;
    }
}
