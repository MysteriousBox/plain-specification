package org.plain.specification.mybatisplus;

import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Class FieldMappingRegistrar.
 *
 * @author Jayden.Liang
 */


public class FieldMappingRegistrar {

    private static final String TABLE_INFO_ERROR = "未找到 PO 的 TableInfo: ";

    /**
     * Automatically register field mappings from domain model to PO model.
     *
     * @param domainClass domain class
     * @param poClass persistence object class
     */
    public static void registerMapping(Class<?> domainClass, Class<?> poClass) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(poClass);
        if (tableInfo == null) {
            throw new IllegalStateException(TABLE_INFO_ERROR + poClass);
        }

        Map<String, String> fieldMapping = new HashMap<>(domainClass.getDeclaredFields().length);
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
