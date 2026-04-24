package org.plain.specification.core.expression;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Class LambdaUtil.
 *
 * @author Jayden.Liang
 */
public class LambdaUtil {
    private LambdaUtil() {
        /* This utility class should not be instantiated */
    }


    private static final String GET_PREFIX = "get";
    private static final String IS_PREFIX = "is";

    public static String getFileName(Serializable lambda) {
        SerializedLambda func = getSerializedLambda(lambda);
        String methodName = func.getImplMethodName();
        if (methodName.startsWith(GET_PREFIX)){
            return StringUtils.uncapitalize(methodName.substring(GET_PREFIX.length()));
        }else if (methodName.startsWith(IS_PREFIX)){
            return StringUtils.uncapitalize(methodName.substring(IS_PREFIX.length()));
        }
        throw new IllegalArgumentException("Not a valid getter method (must start with get/is): " + methodName);
    }


    @SuppressWarnings("squid:S3011")
    private static SerializedLambda getSerializedLambda(Serializable lambda) {
        try {
            Method method = lambda.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(true);
            Object result = method.invoke(lambda);
            if (result instanceof SerializedLambda) {
                return (SerializedLambda) result;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to get SerializedLambda", e);
        }
        throw new IllegalArgumentException("Not a valid lambda instance");
    }
}
