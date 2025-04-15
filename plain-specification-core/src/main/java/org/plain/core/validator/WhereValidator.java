package org.plain.core.validator;

import org.plain.core.ISpecification;
import org.plain.core.Specification;
import org.plain.core.expression.WhereExpressionInfo;

public class WhereValidator implements IValidator {

    private WhereValidator() {
    }

    public static WhereValidator instance() {
        return new WhereValidator();
    }

    @Override
    public <T> Boolean isValid(T entity, ISpecification<T> specification) {
        for (WhereExpressionInfo<T> whereExpressionInfo : specification.getWhereExpressionInfos()) {
            if (!whereExpressionInfo.predicate().test(entity)) {
                return false;
            }
        }
        return true;
    }
}
