package org.plain.specification.core.expression;

import lombok.Getter;

/**
 * 表达式操作符枚举
 * @author Jayden.Liang
 */
/**
 * Enum ExpressionOperatorEnum.
 *
 * @author Jayden.Liang
 */
@Getter
public enum ExpressionOperatorEnum {
    /** Logical AND operator. */
    AND("&&"),

    /** Logical OR operator. */
    OR("||"),

    /** Logical NOT operator. */
    NOT("!"),

    /** Equality operator. */
    EQUAL("="),

    /** Greater-than operator. */
    GREATER_THAN(">"),

    /** Less-than operator. */
    LESS_THAN("<"),

    /** Greater-than-or-equal operator. */
    GREATER_THAN_OR_EQUAL(">="),

    /** Less-than-or-equal operator. */
    LESS_THAN_OR_EQUAL("<="),

    /** Not-equal operator. */
    NOT_EQUAL("!="),

    /** In operator. */
    IN("in"),

    /** Not-in operator. */
    NOT_IN("not in"),

    /** Between operator. */
    BETWEEN("between"),

    /** Like operator. */
    LIKE("like"),

    /** Is null operator. */
    IS_NULL("is null"),

    /** Is not null operator. */
    IS_NOT_NULL("is not null"),

    /** Exists operator. */
    EXISTS("exists"),

    /** Not exists operator. */
    NOT_EXISTS("not exists");

    private final String operator;

    ExpressionOperatorEnum(String operator) {
        this.operator = operator;
    }

}
