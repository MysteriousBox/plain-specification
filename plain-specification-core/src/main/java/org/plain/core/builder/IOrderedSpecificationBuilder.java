package org.plain.core.builder;

import org.plain.core.expression.Expressions;

public interface IOrderedSpecificationBuilder<T> extends ISpecificationBuilder<T> {

    IOrderedSpecificationBuilder<T> thenBy(Expressions<T> expression);
    IOrderedSpecificationBuilder<T> thenBy(Expressions<T> expression, Boolean condition);

    IOrderedSpecificationBuilder<T> thenByDescending(Expressions<T> expression);

    IOrderedSpecificationBuilder<T> thenByDescending(Expressions<T> expression, Boolean condition);
}
