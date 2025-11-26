package org.plain.specification.mybatisplus;


import com.baomidou.mybatisplus.core.toolkit.support.IdeaProxyLambdaMeta;
import com.baomidou.mybatisplus.core.toolkit.support.LambdaMeta;
import com.baomidou.mybatisplus.core.toolkit.support.ReflectLambdaMeta;
import com.baomidou.mybatisplus.core.toolkit.support.ShadowLambdaMeta;
import org.plain.specification.core.expression.SFunction;

import java.io.Serializable;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class MpFieldNameResolver {

    public static  <T> String resolve(SFunction<T, ?> func) {
        SerializedLambda lambda = serialize(func);
        return resolve(lambda);
    }

    public static String resolve(SerializedLambda lambda){
        String methodName = lambda.getImplMethodName();
        // 转换 getter 方法名 -> 属性名
        if (methodName.startsWith("get")) {
            methodName = methodName.substring(3);
        } else if (methodName.startsWith("is")) {
            methodName = methodName.substring(2);
        }
        return Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getDomainClass(SerializedLambda lambda) {
        String className = lambda.getImplClass().replace('/', '.');
        try {
            return (Class<T>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("无法加载类：" + className, e);
        }
    }

    public static SerializedLambda serialize(Serializable lambda) {
        try {
            Method write = lambda.getClass().getDeclaredMethod("writeReplace");
            write.setAccessible(true);
            return (SerializedLambda) write.invoke(lambda);
        } catch (Exception e) {
            throw new RuntimeException("无法解析 Lambda 表达式", e);
        }
    }

}
