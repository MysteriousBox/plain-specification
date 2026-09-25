package org.plain.specification.mybatisplus;


import org.plain.specification.core.expression.LambdaUtil;
import org.plain.specification.core.expression.SFunction;

import java.io.Serializable;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * MyBatis-Plus 字段名解析器，从 Lambda 表达式提取数据库列名。
 *
 * @author Jayden.Liang
 */
public class MpFieldNameResolver {
    private MpFieldNameResolver() {
        /* This utility class should not be instantiated */
    }

    public static <T> String resolve(SFunction<T, ?> func) {
        return LambdaUtil.getPropertyName(func);
    }

    public static String resolve(SerializedLambda lambda) {
        return LambdaUtil.getPropertyName(lambda);
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
            write.setAccessible(true);
            return (SerializedLambda) write.invoke(lambda);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("无法解析 Lambda 表达式", e);
        }
    }

}
