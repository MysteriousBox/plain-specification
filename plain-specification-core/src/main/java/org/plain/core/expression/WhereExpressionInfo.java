package org.plain.core.expression;


import lombok.Getter;
import org.plain.core.descriptor.IExpressionDescriptor;
import org.plain.core.visitor.IExpressionVisitor;

import java.util.function.Predicate;

public class WhereExpressionInfo <T> implements IExpressionDescriptor<T> {

    private Predicate<T> filterFunc;

    @Getter
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
}
