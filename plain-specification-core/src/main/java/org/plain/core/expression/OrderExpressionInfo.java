package org.plain.core.expression;

import lombok.Getter;
import org.plain.core.OrderTypeEnum;
import org.plain.core.visitor.IExpressionVisitor;
import org.plain.core.visitor.OrderExpressionVisitor;

import java.util.Comparator;


public class OrderExpressionInfo<T> {

    private Comparator<T> keySelectorFunc;


    @Getter
    private final Expressions<T> keySelector;

    @Getter
    private final OrderTypeEnum orderType;

    private final IExpressionVisitor<T, Comparator<T>> visitor;


    public OrderExpressionInfo(Expressions<T> keySelector, OrderTypeEnum orderType, IExpressionVisitor<T, Comparator<T>> visitor) {
        this.visitor = visitor;
        if (keySelector == null){
            throw new IllegalArgumentException("Expression syntax error");
        }
        this.keySelector = keySelector;
        this.orderType = orderType;
    }

    public Comparator<T> getKeySelectorFunc() {
        if (keySelectorFunc == null){
            keySelectorFunc = keySelector.compiler(visitor).compile();
        }
        return keySelectorFunc;
    }
}
