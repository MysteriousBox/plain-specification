package org.plain.specification.mybatisplus;

import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FieldMappingRegistry {

    // 存储：领域模型 -> 数据库模型 -> 字段映射
    private static final Map<String, Map<String, String>> registry = new ConcurrentHashMap<>();

    // 注册映射：领域类、字段名 -> PO 字段名
    public static void register(Class<?> domainClass, Map<String, String> fieldMapping) {
        registry.put(domainClass.getName(), fieldMapping);
    }

    // 获取映射
    public static Map<String, String> getFieldMapping(Class<?> domainClass) {
        return registry.get(domainClass.getName());
    }

    // 获取数据库字段名
    public static String getColumn(Class<?> domainClass, String domainField) {
        Map<String, String> mapping = getFieldMapping(domainClass);
        if (mapping == null) throw new IllegalStateException("未注册映射: " + domainClass);
        return mapping.get(domainField);
    }

}
