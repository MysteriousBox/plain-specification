package org.plain.specification.redis.pushdown;

import org.plain.specification.core.expression.LambdaUtil;
import org.plain.specification.core.expression.SFunction;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves a getter reference to the HASH field name the repository stores it under, and
 * classifies the value domain so the narrowing compiler knows which raw-string comparisons can
 * stand in for the Java comparison performed during in-memory filtering.
 * <p>
 * Only {@link Kind#INTEGER} and {@link Kind#STRING} are narrowed in Redis. Everything else —
 * floating point, temporal, nested objects, derived getters with no matching field — is reported
 * as {@link Kind#UNPUSHABLE} and left to the in-memory verifier.
 */
public final class FieldMeta {

    /** Value domain of a stored HASH field, as produced by the repository write path. */
    public enum Kind {
        /** Integral value: stored as a canonical decimal string by {@code String.valueOf(Number)}. */
        INTEGER,
        /** String value: the write path stores it verbatim and the read path returns it verbatim. */
        STRING,
        /** Anything the storage layout cannot decide a comparison for. */
        UNPUSHABLE
    }

    private static final char RETURN_TYPE_SEPARATOR = ')';
    private static final String INTEGER_WRAPPER_PREFIX = "Ljava/lang/";
    private static final String BIG_INTEGER = "Ljava/math/BigInteger;";

    private static final Map<Class<?>, FieldMeta> CACHE = new ConcurrentHashMap<Class<?>, FieldMeta>();
    private static final FieldMeta UNPUSHABLE = new FieldMeta(null, Kind.UNPUSHABLE);

    private final String fieldName;
    private final Kind kind;

    private FieldMeta(String fieldName, Kind kind) {
        this.fieldName = fieldName;
        this.kind = kind;
    }

    /**
     * Resolve a getter reference. Never throws: a getter that cannot be understood is reported as
     * unpushable so the caller keeps using the in-memory path.
     *
     * @param getter field reference, may be {@code null}
     * @return resolution result, never {@code null}
     */
    public static FieldMeta of(SFunction<?, ?> getter) {
        if (getter == null) {
            return UNPUSHABLE;
        }
        final Class<?> lambdaClass = getter.getClass();
        FieldMeta cached = CACHE.get(lambdaClass);
        if (cached != null) {
            return cached;
        }
        FieldMeta resolved;
        try {
            final SerializedLambda lambda = serialize(getter);
            resolved = new FieldMeta(LambdaUtil.getPropertyName(lambda), kindOf(lambda.getImplMethodSignature()));
        } catch (RuntimeException ex) {
            resolved = UNPUSHABLE;
        }
        CACHE.put(lambdaClass, resolved);
        return resolved;
    }

    /**
     * @return the HASH field name, or {@code null} when unresolvable
     */
    public String fieldName() {
        return fieldName;
    }

    /**
     * @return the value domain of this field
     */
    public Kind kind() {
        return kind;
    }

    /**
     * @return true when the reference resolved to a HASH field name, irrespective of value domain
     */
    public boolean resolvable() {
        return fieldName != null && !fieldName.isEmpty();
    }

    /**
     * @return true when comparisons on this field can be decided from the raw stored string
     */
    public boolean pushable() {
        return kind != Kind.UNPUSHABLE && resolvable();
    }

    private static SerializedLambda serialize(Serializable lambda) {
        try {
            final Method writeReplace = lambda.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            final Object result = writeReplace.invoke(lambda);
            if (result instanceof SerializedLambda) {
                return (SerializedLambda) result;
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            throw new IllegalStateException("cannot capture lambda", ex);
        }
        throw new IllegalStateException("not a serializable lambda");
    }

    private static Kind kindOf(String methodSignature) {
        if (methodSignature == null) {
            return Kind.UNPUSHABLE;
        }
        final int close = methodSignature.indexOf(RETURN_TYPE_SEPARATOR);
        if (close < 0 || close == methodSignature.length() - 1) {
            return Kind.UNPUSHABLE;
        }
        final String ret = methodSignature.substring(close + 1);
        if (ret.length() == 1) {
            switch (ret.charAt(0)) {
                case 'I':
                case 'J':
                case 'B':
                case 'S':
                    return Kind.INTEGER;
                default:
                    return Kind.UNPUSHABLE;
            }
        }
        if ("Ljava/lang/String;".equals(ret)) {
            return Kind.STRING;
        }
        if (BIG_INTEGER.equals(ret)) {
            return Kind.INTEGER;
        }
        if (ret.startsWith(INTEGER_WRAPPER_PREFIX) && ret.endsWith(";")) {
            final String simple = ret.substring(INTEGER_WRAPPER_PREFIX.length(), ret.length() - 1);
            if ("Integer".equals(simple) || "Long".equals(simple) || "Short".equals(simple) || "Byte".equals(simple)) {
                return Kind.INTEGER;
            }
        }
        return Kind.UNPUSHABLE;
    }
}
