package org.plain.specification.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enum OrderTypeEnum.
 *
 * @author Jayden.Liang
 */
/**
 * Enum OrderTypeEnum.
 *
 * @author Jayden.Liang
 */
@Getter
@AllArgsConstructor
public enum OrderTypeEnum {

    /** Order by ascending. */
    OrderBy(1),

    /** Order by descending. */
    OrderByDescending(2),

    /** Secondary order ascending. */
    ThenBy(3),

    /** Secondary order descending. */
    ThenByDescending(4);

    private final Integer value;
}
