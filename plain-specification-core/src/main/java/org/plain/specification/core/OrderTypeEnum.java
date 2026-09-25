package org.plain.specification.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enum OrderTypeEnum.
 *
 * @author Jayden.Liang
 */
@Getter
@AllArgsConstructor
public enum OrderTypeEnum {

    /** Order by ascending. */
    ORDER_BY(1),

    /** Order by descending. */
    ORDER_BY_DESCENDING(2),

    /** Secondary order ascending. */
    THEN_BY(3),

    /** Secondary order descending. */
    THEN_BY_DESCENDING(4);

    private final Integer value;
}
