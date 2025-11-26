package org.plain.specification.core.expression;

import lombok.Getter;

/**
 * 表达式操作符枚举
 * @author Hugh
 */
@Getter
public enum ExpressionOperatorEnum {
    AND("&&"),
    OR("||"),
    NOT("!"),
    EQUAL("="),
    GREATER_THAN(">"),
    LESS_THAN("<"),
    GREATER_THAN_OR_EQUAL(">="),
    LESS_THAN_OR_EQUAL("<="),
    NOT_EQUAL("!="),
    IN("in"),
    NOT_IN("not in"),
    BETWEEN("between"),
    LIKE("like"),
    IS_NULL("is null"),
    IS_NOT_NULL("is not null"),
    EXISTS("exists"),
    NOT_EXISTS("not exists");

    private final String operator;

    ExpressionOperatorEnum(String operator) {
        this.operator = operator;
    }

}
