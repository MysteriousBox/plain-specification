package org.plain.specification.core.expression;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

public class LambdaUtil {

    public static <T,R> String getFileName(Serializable lambda){
        SerializedLambda func = getSerializedLambda(lambda);
        String methodName = func.getImplMethodName();
        if (methodName.startsWith("get")){
            return StringUtils.uncapitalize(methodName.substring(3));
        }else if (methodName.startsWith("is")){
            return StringUtils.uncapitalize(methodName.substring(2));
        }
        throw new IllegalArgumentException("Not a valid getter method(must starts with get/is): " + methodName);
    }


    private static SerializedLambda getSerializedLambda(Serializable lambda) {
        try {
            Method method = lambda.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(true);
            Object result = method.invoke(lambda);
            if (result instanceof SerializedLambda) {
                return (SerializedLambda) result;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get SerializedLambda", e);
        }
        throw new IllegalArgumentException("Not a valid lambda instance");
    }
}
