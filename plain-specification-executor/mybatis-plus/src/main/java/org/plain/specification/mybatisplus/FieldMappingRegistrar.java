package org.plain.specification.mybatisplus;

import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class FieldMappingRegistrar {


    /**
     * 自动从领域模型到数据库模型生成映射
     */
    public static void registerMapping(Class<?> domainClass, Class<?> poClass) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(poClass);
        if (tableInfo == null) throw new IllegalStateException("未找到 PO 的 TableInfo: " + poClass);

        Map<String, String> fieldMapping = new HashMap<>();
        for (Field domainField : domainClass.getDeclaredFields()) {
            for (TableFieldInfo tableFieldInfo : tableInfo.getFieldList()) {
                if (tableFieldInfo.getProperty().equals(domainField.getName())) {
                    fieldMapping.put(domainField.getName(), tableFieldInfo.getColumn());
                    break;
                }
            }
        }
        FieldMappingRegistry.register(domainClass, fieldMapping);
    }
}
