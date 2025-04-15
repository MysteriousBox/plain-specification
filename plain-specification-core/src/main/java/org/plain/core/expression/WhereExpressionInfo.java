package org.plain.core.expression;


import lombok.Getter;
import org.plain.core.visitor.IExpressionVisitor;
import org.plain.core.visitor.PredicateExpressionVisitor;

import java.util.function.Function;
import java.util.function.Predicate;

public class WhereExpressionInfo <T>{

    private Predicate<T> predicate;


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

    public Predicate<T> predicate() {
        if (predicate == null){
            predicate = expressions.compiler(visitor).compile();
        }
        return predicate;
    }

}
