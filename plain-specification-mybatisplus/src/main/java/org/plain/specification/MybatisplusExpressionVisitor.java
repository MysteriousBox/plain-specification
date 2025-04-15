package org.plain.specification;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.plain.core.expression.EqualExpression;
import org.plain.core.visitor.AbstractExpressionVisitor;

import java.util.function.Predicate;

public class MybatisplusExpressionVisitor<T> extends AbstractExpressionVisitor<T, LambdaQueryWrapper<T>> {


    @Override
    public <R extends Comparable<R>> LambdaQueryWrapper<T> visitEqual(EqualExpression<T, R> expression) {

        Predicate<T> predicate = t -> {
            if (expression.getLeft().apply(t).equals(expression.getRight())){
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        };

        return Wrappers.<T>lambdaQuery().eq(t->expression.getLeft().apply(t), expression.getRight());
    }
}
