package org.plain.specification;


import org.plain.core.expression.SFunction;

import java.io.Serializable;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

public class MpFieldNameResolver {

    public static  <T> String resolve(SFunction<T, ?> func) {
        SerializedLambda lambda = serialize(func);
        String methodName = lambda.getImplMethodName();
        // 转换 getter 方法名 -> 属性名
        if (methodName.startsWith("get")) {
            methodName = methodName.substring(3);
        } else if (methodName.startsWith("is")) {
            methodName = methodName.substring(2);
        }
        return Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);
    }

    private static SerializedLambda serialize(Serializable lambda) {
        try {
            Method write = lambda.getClass().getDeclaredMethod("writeReplace");
            write.setAccessible(true);
            return (SerializedLambda) write.invoke(lambda);
        } catch (Exception e) {
            throw new RuntimeException("无法解析 Lambda 表达式", e);
        }
    }
}
