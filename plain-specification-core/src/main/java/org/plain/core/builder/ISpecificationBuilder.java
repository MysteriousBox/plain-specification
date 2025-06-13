package org.plain.core.builder;

import org.plain.core.ISpecification;
import org.plain.core.expression.Expressions;

public interface ISpecificationBuilder<T> {

    ISpecification<T> getSpecification();

    ISpecificationBuilder<T> where(Expressions<T> expression);

    ISpecificationBuilder<T> where(Expressions<T> expression,Boolean condition);

    IOrderedSpecificationBuilder<T> orderBy(Expressions<T> expression);

    IOrderedSpecificationBuilder<T> orderBy(Expressions<T> expression, Boolean condition);

    IOrderedSpecificationBuilder<T> orderByDescending(Expressions<T> expression);

    IOrderedSpecificationBuilder<T> orderByDescending(Expressions<T> expression, Boolean condition);


}
