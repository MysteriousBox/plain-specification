package org.plain.specification.core.expression;

import lombok.Getter;
import org.plain.specification.core.OrderTypeEnum;
import org.plain.specification.core.descriptor.IExpressionDescriptor;
import org.plain.specification.core.visitor.IExpressionVisitor;

import java.util.Comparator;

/**
 * 排序表达式信息封装，持有字段引用和比较器。
 *
 * @author Jayden.Liang
 */
public class OrderExpressionInfo<T> implements IExpressionDescriptor<T> {

    private Comparator<T> keySelectorFunc;



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

    @Override
    public <R> R func(IExpressionVisitor<T, R> visitor) {
        return keySelector.compiler(visitor).compile();
    }

    @Override
    public Expressions<T> getExpressions() {
        return this.keySelector;
    }

}
