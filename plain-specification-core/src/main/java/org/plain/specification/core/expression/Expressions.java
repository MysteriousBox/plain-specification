package org.plain.specification.core.expression;

import lombok.Getter;
import org.plain.specification.core.visitor.IExpressionVisitor;

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Predicate;

/**
 * Expression builder class
 * Used to construct various types of expressions, included logical expressions(AND ,OR, NOT) and comparison expressions(equal, not equal, greater than, less than, greater than or equal, less than or equal, in, not in, between, like, exists, not exists).
 * @param <T>  Expression type
 * @author Jayden.Liang
 */
public class Expressions<T> {

    /**
     * Current left expression
     */
    @Getter
    private IExpression<T> currentLeft;

    private ExpressionOperatorEnum beforeOperator = null;

    /**
     * Current operator, default is AND
     */
    private ExpressionOperatorEnum currentOperator = ExpressionOperatorEnum.AND;

    @Getter
    private IExpressionVisitor<T,Predicate<T>> visitor;

    /**
     * Private constructor, prevent the creation of instances.
     */
    private Expressions() {
    }

    private Expressions(IExpressionVisitor<T, Predicate<T>> visitor){
        this.visitor = visitor;
    }

    /**
     * Static method, used to create an expression builder instance
     * @param <T>  Expression type
     * @return  Expression builder instance
     */
    public static <T> Expressions<T> create() {
        return new Expressions<>();
    }

    public static <T> Expressions<T> create(IExpressionVisitor<T, Predicate<T>> visitor) {
        return new Expressions<>(visitor);
    }

    /**
     * Sets the logical operator to AND
     * @return Expressions instance
     * @throws IllegalArgumentException if the left expression is null
     */
    public Expressions<T> and() {
        if (currentLeft == null){
            throw new IllegalArgumentException("Expression syntax error, left is null");
        }
        currentOperator = ExpressionOperatorEnum.AND;
        return this;
    }

    /**
     * Adds an AND logical operation expression to the left expression
     * @param right  Right expression
     * @return Expressions instance
     * @throws IllegalArgumentException if the left expression is null
     */
    public Expressions<T> and(IExpression<T> right) {
        if (currentLeft == null){
            throw new IllegalArgumentException("Expression syntax error, left is null");
        }
        currentLeft = new AndExpression<>(currentLeft, right);
        return this;
    }

    /**
     * Sets the logical operator to OR
     * @return Expressions instance
     * @throws IllegalArgumentException if the left expression is null
     */
    public Expressions<T> or() {
        if (currentLeft == null){
            throw new IllegalArgumentException("Expression syntax error, left is null");
        }
        currentOperator = ExpressionOperatorEnum.OR;
        return this;
    }

    /**
     * Adds an OR logical operation expression to the left expression
     * @param right Right expression
     * @return Expressions instance
     * @throws IllegalArgumentException if the left expression is null
     */
    public Expressions<T> or(IExpression<T> right) {
        if (currentLeft == null){
            throw new IllegalArgumentException("Expression syntax error, left is null");
        }
        currentLeft = new OrExpression<>(currentLeft, right);
        return this;
    }

    /**
     * Sets the logical operator to NOT
     * @return Expressions instance
     */
    public Expressions<T> not() {
        if (currentOperator == null){
            currentOperator = ExpressionOperatorEnum.NOT;
        }else {
            beforeOperator = currentOperator;
            currentOperator = ExpressionOperatorEnum.NOT;
        }
        return this;
    }

    /**
     * Adds a NOT logical operation expression connected to the left expression
     * @param right Right expression
     * @return Expressions instance
     */
    public Expressions<T> not(IExpression<T> right) {
        if (currentLeft==null){
            currentLeft = new NotExpression<>(right);
        }else {
            currentLeft = new AndExpression<>(currentLeft, new NotExpression<>(right));
        }
        return this;
    }

    /**
     * Adds an equal comparison expression
     * @param left Left expression,  a function that extracts the comparison value
     * @param right Right expression, the comparison value
     * @return Expressions instance
     * @param <V> The type of the value returned by the left expression
     */
    public <V extends Comparable<V>> Expressions<T> equal(SFunction<T, V> left, V right) {
        return composite(new EqualExpression<>(left, right));
    }

    /**
     * Adds a not equal comparison expression
     * @param left Left expression,  a function that extracts the comparison value
     * @param right Right expression, the comparison value
     * @return Expressions instance
     * @param <V> The type of the value returned by the left expression
     */
    public <V extends Comparable<V>> Expressions<T> notEqual(SFunction<T, V> left, V right) {
        return composite(new NotEqualExpression<>(left, right));
    }

    /**
     * Adds a greater than comparison expression
     * @param left Left expression,  a function that extracts the comparison value
     * @param right Right expression, the comparison value
     * @return Expressions instance
     * @param <V> The type of the value returned by the left expression
     */
    public <V extends Comparable<V>> Expressions<T> greaterThan(SFunction<T, V> left, V right) {
        return composite(new GreaterThanExpression<>(left, right));
    }

    /**
     * Adds a greater than or equal to comparison expression
     * @param left Left expression,  a function that extracts the comparison value
     * @param right Right expression, the comparison value
     * @return Expressions instance
     * @param <V> The type of the value returned by the left expression
     */
    public <V extends Comparable<V>> Expressions<T> greaterThanOrEqual(SFunction<T, V> left, V right) {
        return composite(new GreaterThanOrEqualExpression<>(left, right));
    }

    /**
     * Adds a less than comparison expression
     * @param left Left expression,  a function that extracts the comparison value
     * @param right Right expression, the comparison value
     * @return Expressions instance
     * @param <V> The type of the value returned by the left expression
     */
    public <V extends Comparable<V>> Expressions<T> lessThan(SFunction<T, V> left, V right) {
        return composite(new LessThanExpression<>(left, right));
    }

    /**
     * Adds a less than or equal to comparison expression
     * @param left Left expression,  a function that extracts the comparison value
     * @param right Right expression, the comparison value
     * @return Expressions instance
     * @param <V> The type of the value returned by the left expression
     */
    public <V extends Comparable<V>> Expressions<T> lessThanOrEqual(SFunction<T, V> left, V right) {
        return composite(new LessThanOrEqualExpression<>(left, right));
    }

    /**
     * Adds an in comparison expression
     * @param left Left expression,  a function that extracts the comparison value
     * @param right Right expression, the comparison value
     * @return Expressions instance
     * @param <V> The type of the value returned by the left expression
     */
    public <V extends Comparable<V>> Expressions<T> in(SFunction<T, V> left, Collection<V> right) {
        return composite(new InExpression<>(left, right));
    }

    /**
     * Adds a notIn comparison expression
     * @param left Left expression,  a function that extracts the comparison value
     * @param right Right expression, the comparison value
     * @return Expressions instance
     * @param <V> The type of the value returned by the left expression
     */
    public <V extends Comparable<V>> Expressions<T> notIn(SFunction<T, V> left, Collection<V> right) {
        return composite(new NotInExpression<>(left, right));
    }


    /**
     * Adds a between comparison expression
     * @param left Left expression,  a function that extracts the comparison value
     * @param leftValue Left value
     * @param rightValue Right value
     * @return Expressions instance
     * @param <V> The type of the value returned by the left expression
     */
    public <V extends Comparable<V>> Expressions<T> between(SFunction<T, V> left, V leftValue, V rightValue) {
        return composite(new BetweenExpression<>(left, leftValue, rightValue));
    }

    /**
     * Adds a like comparison expression
     * @param left Left expression,  a function that extracts the comparison value
     * @param right Right expression, the comparison value
     * @return Expressions instance
     */
    public Expressions<T> like(SFunction<T, String> left, String right) {
        return composite(new LikeExpression<>(left, right));
    }

    /**
     * Adds an exists comparison expression
     * @param left Left expression,  a function that extracts the comparison value
     * @param right Right expression, the comparison value
     * @return Expressions instance
     * @param <E> The type of the value returned by the left expression
     */
    public <E> Expressions<T> exists(SFunction<T, Collection<E>> left, IExpression<E> right) {
        return composite(new ExistsExpression<>(left, right));
    }

    /**
     * Adds a not exists comparison expression
     * @param left Left expression,  a function that extracts the comparison value
     * @param right Right expression, the comparison value
     * @return Expressions instance
     * @param <E> The type of the value returned by the left expression
     */
    public <E> Expressions<T> notExists(SFunction<T, Collection<E>> left, IExpression<E> right) {
        return composite(new NotExistsExpression<>(left, right));
    }

    public <V extends Comparable<V>> Expressions<T> orderBy(SFunction<T, V> left) {
        return composite(new OrderExpression<>(left, Comparator.nullsLast(Comparator.naturalOrder()), true));
    }

    public <V extends Comparable<V>> Expressions<T> orderByDescending(SFunction<T, V> left) {
        return composite(new OrderExpression<>(left, Comparator.nullsLast(Comparator.reverseOrder()), false));
    }

    public <V extends Comparable<V>> Expressions<T> notNull(SFunction<T,V> left){
        return composite(new IsNotNullExpression<>(left));
    }

    public <V extends Comparable<V>>  Expressions<T> isNull(SFunction<T,V> left){
        return composite(new IsNullExpression<>(left));
    }

    /**
     * Builds the expression
     * @return IExpression instance
     */
    public IExpression<T> build(){
        if (currentLeft == null){
            throw new IllegalArgumentException("Expression syntax error");
        }
        return currentLeft;
    }

    public <R> ExpressionCompiler<R> compiler(IExpressionVisitor<T,R> visitor){
        return new ExpressionCompiler<>(visitor);
    }

    /**
     * Combines expressions according to the current logical operator
     * @param expression Expression to combine
     * @return Expressions instance
     * @throws IllegalArgumentException if expression is null or the operator is invalid
     */
    private Expressions<T> composite(IExpression<T> expression){
        if (expression == null){
            throw new IllegalArgumentException("Expression syntax error");
        }
        switch (currentOperator){
            case AND:
                if (currentLeft == null){
                    currentLeft = expression;
                }else {
                    currentLeft = new AndExpression<>(currentLeft, expression);
                }
                break;
            case OR:
                currentLeft = new OrExpression<>(currentLeft, expression);
                break;
            case NOT:
                if (currentLeft == null){
                    currentLeft = new NotExpression<>(expression);
                }else {
                    if (beforeOperator == ExpressionOperatorEnum.AND){
                        currentLeft = new AndExpression<>(currentLeft, new NotExpression<>(expression));
                    }else if (beforeOperator == ExpressionOperatorEnum.OR){
                        currentLeft = new OrExpression<>(currentLeft, new NotExpression<>(expression));
                    }else {
                        throw new IllegalArgumentException("Expression syntax error");
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Expression syntax error");
        }
        currentOperator = ExpressionOperatorEnum.AND;
        return this;
    }

    public class ExpressionCompiler<R>  {
        private final IExpressionVisitor<T, R> visitor;

        public ExpressionCompiler(IExpressionVisitor<T, R> visitor) {
            this.visitor = visitor;
        }

        public R compile(){
            return currentLeft.accept(visitor);
        }
    }
}
