package org.plain.specification.core.expression;

import java.io.Serializable;
import java.util.function.Function;

@FunctionalInterface
/**
 * Interface SFunction.
 *
 * @author Jayden.Liang
 */

public interface SFunction<T,R> extends Function<T,R>, Serializable {

}
