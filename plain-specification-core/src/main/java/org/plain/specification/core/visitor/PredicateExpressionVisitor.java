package org.plain.specification.core.visitor;

import org.plain.specification.core.expression.*;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * 内存 表达式访问者
 * @param <T>
 * @author Jayden.Liang
 */
public class PredicateExpressionVisitor<T> extends AbstractExpressionVisitor<T, Predicate<T>>  {

    @Override
    public  Predicate<T> visitAnd(AndExpression<T> expression) {
        return expression.getLeft().accept(this).and(expression.getRight().accept(this));
    }

    @Override
    public Predicate<T> visitOr(OrExpression<T> expression) {
        return expression.getLeft().accept(this).or(expression.getRight().accept(this));
    }

    @Override
    public Predicate<T> visitNot(NotExpression<T> expression) {
        return expression.getExpression().accept(this).negate();
    }

    @Override
    public <V extends Comparable<V>> Predicate<T> visitEqual(EqualExpression<T, V> expression) {
        return t -> {
            if (expression.getLeft().apply(t) == null&& expression.getRight() == null){
                return Boolean.TRUE;
            }
            if (expression.getLeft().apply(t) != null&&expression.getLeft().apply(t).equals(expression.getRight())) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        };
    }

    @Override
    public <V extends Comparable<V>> Predicate<T> visitGt(GreaterThanExpression<T, V> expression) {

        return t -> {
            if (expression.getLeft().apply(t) == null || expression.getRight() == null){
                throw new IllegalArgumentException("left and right can not be null");
            }
            if (expression.getLeft().apply(t).compareTo(expression.getRight()) > 0){
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        };
    }

    @Override
    public <V extends Comparable<V>> Predicate<T> visitLt(LessThanExpression<T, V> expression) {
        return t -> {
            if (expression.getLeft().apply(t) == null || expression.getRight() == null){
                throw new IllegalArgumentException("left and right can not be null");
            }
            if (expression.getLeft().apply(t).compareTo(expression.getRight()) < 0){
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        };
    }

    @Override
    public <V extends Comparable<V>> Predicate<T> visitGte(GreaterThanOrEqualExpression<T, V> expression) {
        return t -> {
            if (expression.getLeft().apply(t) == null || expression.getRight() == null){
                throw new IllegalArgumentException("left and right can not be null");
            }
            if (expression.getLeft().apply(t).compareTo(expression.getRight()) >= 0){
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        };
    }

    @Override
    public <V extends Comparable<V>> Predicate<T> visitLte(LessThanOrEqualExpression<T, V> expression) {
        return t -> {
            if (expression.getLeft().apply(t) == null || expression.getRight() == null){
                throw new IllegalArgumentException("left and right can not be null");
            }
            if ( expression.getLeft().apply(t).compareTo(expression.getRight()) <= 0){
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        };
    }

    @Override
    public <V extends Comparable<V>> Predicate<T> visitNotEqual(NotEqualExpression<T, V> expression) {
        return t -> {
            V leftVal = expression.getLeft().apply(t);
            V rightVal = expression.getRight();
            if (leftVal == null || rightVal == null) return Boolean.FALSE;
            return leftVal.equals(rightVal) ? Boolean.FALSE : Boolean.TRUE;
        };
    }

    @Override
    public <V extends Comparable<V>> Predicate<T> visitIn(InExpression<T, V> expression) {
        return t -> {
            V value = expression.getLeft().apply(t);
            return value != null && expression.getRight().contains(value);
        };
    }

    @Override
    public <V extends Comparable<V>> Predicate<T> visitNot(NotInExpression<T, V> expression) {
        return t -> {
            V value = expression.getLeft().apply(t);
            return value == null || !expression.getRight().contains(value);
        };
    }

    @Override
    public <V extends Comparable<V>> Predicate<T> visitBetween(BetweenExpression<T, V> expression) {
        return t -> {
            V value = expression.getLeft().apply(t);
            if (value == null || expression.getLowerBound() == null || expression.getUpperBound() == null) {
                throw new IllegalArgumentException("value, lowerBound and upperBound can not be null");
            }
            return value.compareTo(expression.getLowerBound()) >= 0
                    && value.compareTo(expression.getUpperBound()) <= 0;
        };
    }

    @Override
    public Predicate<T> visitLike(LikeExpression<T> expression) {
        String pattern = convertLikePatternToRegex(expression.getRight());
        return t -> {
            String value = expression.getLeft().apply(t);
            if (value == null) return Boolean.FALSE;
            return value.matches(pattern);
        };
    }

    @Override
    public <V> Predicate<T> visitIsNull(IsNullExpression<T,V> expression) {
        return t-> expression.getLeft().apply(t) == null;
    }

    @Override
    public <V> Predicate<T> visitIsNotNull(IsNotNullExpression<T,V> expression) {
        return t -> expression.getLeft().apply(t) != null;
    }

    @Override
    public <E> Predicate<T> visitExists(ExistsExpression<T, E> expression) {

        return t -> {
            Collection<E> subqueryProvider = expression.getLeft().apply(t);
            if (subqueryProvider == null || subqueryProvider.isEmpty()){
                return Boolean.FALSE;
            }
            return subqueryProvider.stream().anyMatch(expression.getRight().accept(new PredicateExpressionVisitor<>()));
        };
    }

    @Override
    public <E> Predicate<T> visitNotExists(NotExistsExpression<T, E> expression) {
        return t -> {
            Collection<E> subqueryProvider = expression.getLeft().apply(t);
            if (subqueryProvider == null || subqueryProvider.isEmpty()){
                return Boolean.TRUE;
            }
            return subqueryProvider.stream().noneMatch(expression.getExpression().accept(new PredicateExpressionVisitor<>()));
        };
    }

    private String convertLikePatternToRegex(String pattern) {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            switch (c) {
                case '%':
                    regex.append(".*");
                    break;
                case '_':
                    regex.append(".");
                    break;
                case '\\':
                    if (i + 1 < pattern.length()) {
                        regex.append("\\").append(pattern.charAt(++i));
                    }
                    break;
                default:
                    if ("[](){}.*+?$^|#\\".indexOf(c) >= 0) {
                        regex.append("\\");
                    }
                    regex.append(c);
                    break;
            }
        }
        return regex.toString();
    }
}
