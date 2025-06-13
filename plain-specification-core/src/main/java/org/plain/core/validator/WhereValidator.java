package org.plain.core.validator;

import org.plain.core.ISpecification;
import org.plain.core.descriptor.IExpressionDescriptor;
import org.plain.core.expression.WhereExpressionInfo;
import org.plain.core.visitor.PredicateExpressionVisitor;

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
