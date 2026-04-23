package org.plain.specification.mybatisplus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class FieldMappingRegistry.
 *
 * @author Jayden.Liang
 */


public class FieldMappingRegistry {

    /**
     * Registry of domain-to-PO field mappings keyed by domain class name.
     */
    private static final Map<String, Map<String, String>> REGISTRY = new ConcurrentHashMap<>();

    /**
     * Registers a field mapping for the given domain class.
     *
     * @param domainClass domain entity class
     * @param fieldMapping mapping from domain field to PO column
     */
    public static void register(Class<?> domainClass, Map<String, String> fieldMapping) {
        REGISTRY.put(domainClass.getName(), fieldMapping);
    }

    /**
     * Gets the registered field mapping for the specified domain class.
     *
     * @param domainClass domain entity class
     * @return field mapping or null if none registered
     */
    public static Map<String, String> getFieldMapping(Class<?> domainClass) {
        return REGISTRY.get(domainClass.getName());
    }

    /**
     * Returns the database column name for the given domain field.
     *
     * @param domainClass domain entity class
     * @param domainField domain field name
     * @return database column name or null when not mapped
     */
    public static String getColumn(Class<?> domainClass, String domainField) {
        Map<String, String> mapping = getFieldMapping(domainClass);
        if (mapping == null) {
            throw new IllegalStateException("未注册映射: " + domainClass);
        }
        return mapping.get(domainField);
    }

}
