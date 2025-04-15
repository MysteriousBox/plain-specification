package org.plain.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderTypeEnum {
    OrderBy(1),
    OrderByDescending(2),
    ThenBy(3),
    ThenByDescending(4);

    private final Integer value;
}
