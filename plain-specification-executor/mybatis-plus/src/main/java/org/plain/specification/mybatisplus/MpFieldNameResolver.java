package org.plain.specification.mybatisplus;


import org.plain.specification.core.expression.SFunction;

import java.io.Serializable;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Class MpFieldNameResolver.
 *
 * @author Jayden.Liang
 */
public class MpFieldNameResolver {
    private MpFieldNameResolver() {
        /* This utility class should not be instantiated */
    }


    private static final String GET_PREFIX = "get";
    private static final String IS_PREFIX = "is";

    public static <T> String resolve(SFunction<T, ?> func) {
        SerializedLambda lambda = serialize(func);
        return resolve(lambda);
    }

    public static String resolve(SerializedLambda lambda) {
        String methodName = lambda.getImplMethodName();
        // 转换 getter 方法名 -> 属性名
        if (methodName.startsWith(GET_PREFIX)) {
            methodName = methodName.substring(GET_PREFIX.length());
        } else if (methodName.startsWith(IS_PREFIX)) {
            methodName = methodName.substring(IS_PREFIX.length());
        }
        return Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);
    }

    public static Class<?> getDomainClass(SerializedLambda lambda) {
        String className = lambda.getImplClass().replace('/', '.');
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("无法加载类：" + className, e);
        }
    }

    @SuppressWarnings("squid:S3011")
    public static SerializedLambda serialize(Serializable lambda) {
        try {
            Method write = lambda.getClass().getDeclaredMethod("writeReplace");
            // 反射访问私有方法是必要的
            // NOSONAR
            write.setAccessible(true);
            return (SerializedLambda) write.invoke(lambda);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("无法解析 Lambda 表达式", e);
        }
    }

}
