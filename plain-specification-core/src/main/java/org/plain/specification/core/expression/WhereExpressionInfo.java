package org.plain.specification.core.expression;


import org.plain.specification.core.descriptor.IExpressionDescriptor;
import org.plain.specification.core.visitor.IExpressionVisitor;

import java.util.function.Predicate;

/**
 * Class WhereExpressionInfo.
 *
 * @author Jayden.Liang
 */


public class WhereExpressionInfo <T> implements IExpressionDescriptor<T> {

    private Predicate<T> filterFunc;


    private final Expressions<T> expressions;

    private final IExpressionVisitor<T, Predicate<T>> visitor;


    public WhereExpressionInfo(Expressions<T> expression, IExpressionVisitor<T, Predicate<T>> visitor) {
        this.visitor = visitor;
        if (expression == null){
            throw new IllegalArgumentException("Expression syntax error");
        }
        this.expressions = expression;
    }

    public Predicate<T> filterFunc() {
        if (filterFunc == null){
            filterFunc = func(visitor);
        }
        return filterFunc;
    }

    @Override
    public <R> R func(IExpressionVisitor<T, R> visitor) {

        return expressions.compiler(visitor).compile();
    }

    @Override
    public Expressions<T> getExceptions() {
        return this.expressions;
    }


}
