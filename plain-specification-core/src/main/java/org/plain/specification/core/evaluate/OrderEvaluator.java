package org.plain.specification.core.evaluate;

import org.plain.specification.core.ISpecification;
import org.plain.specification.core.expression.OrderExpressionInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * 内存排序评估器，处理 Specification 中的排序表达式。
 * <p>
 * 排序方向已内嵌于 {@link OrderExpressionInfo#getKeySelectorFunc()} 返回的 Comparator 中，
 * 本评估器只需按顺序链式组合多个排序条件。
 * </p>
 *
 * @author Jayden.Liang
 */
public class OrderEvaluator implements IEvaluator {

    public static final OrderEvaluator INSTANCE = new OrderEvaluator();

    private static final Boolean IS_CRITERIA_EVALUATOR = Boolean.FALSE;

    private OrderEvaluator() {
    }

    @Override
    public Boolean isCriteriaEvaluator() {
        return IS_CRITERIA_EVALUATOR;
    }

    @Override
    public <T> Collection<T> evaluate(Collection<T> entities, ISpecification<T> specification) {
        Iterable<OrderExpressionInfo<T>> orderExpressions = specification.getOrderExpressions();
        if (orderExpressions == null) {
            return entities;
        }

        Comparator<T> compositeComparator = null;

        for (OrderExpressionInfo<T> orderExpr : orderExpressions) {
            Comparator<T> comparator = orderExpr.getKeySelectorFunc();
            if (compositeComparator == null) {
                compositeComparator = comparator;
            } else {
                compositeComparator = compositeComparator.thenComparing(comparator);
            }
        }

        if (compositeComparator == null) {
            return entities;
        }

        List<T> sorted = new ArrayList<>(entities);
        sorted.sort(compositeComparator);
        return sorted;
    }
}
