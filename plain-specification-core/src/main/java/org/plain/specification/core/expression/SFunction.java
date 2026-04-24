package org.plain.specification.core.expression;

import java.io.Serializable;
import java.util.function.Function;


/**
 * Interface SFunction.
 *
 * @author Jayden.Liang
 */
@FunctionalInterface
@SuppressWarnings("PMD.ClassNamingShouldBeCamelRule")
public interface SFunction<T,R> extends Function<T,R>, Serializable {

}
