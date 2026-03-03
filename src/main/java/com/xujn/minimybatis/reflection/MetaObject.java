package com.xujn.minimybatis.reflection;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Reflection facade for reading and writing bean properties.
 *
 * <p>Responsibility: hide setter/field lookup details from parameter and
 * result handlers.
 *
 * <p>Thread-safety: not thread-safe; one instance wraps one target object.
 */
public class MetaObject {

    private final Object originalObject;
    private final Map<String, PropertyDescriptor> propertyDescriptors;
    private final Map<String, Field> fields;

    public MetaObject(Object originalObject) {
        this.originalObject = originalObject;
        this.propertyDescriptors = propertyDescriptorMap(originalObject.getClass());
        this.fields = fieldMap(originalObject.getClass());
    }

    public Object getValue(String name) throws ReflectiveOperationException {
        PropertyDescriptor descriptor = propertyDescriptors.get(normalize(name));
        if (descriptor != null && descriptor.getReadMethod() != null) {
            return descriptor.getReadMethod().invoke(originalObject);
        }
        Field field = fields.get(normalize(name));
        if (field != null) {
            field.setAccessible(true);
            return field.get(originalObject);
        }
        throw new NoSuchFieldException(name);
    }

    public void setValue(String name, Object value) throws ReflectiveOperationException {
        PropertyDescriptor descriptor = propertyDescriptors.get(normalize(name));
        if (descriptor != null && descriptor.getWriteMethod() != null) {
            descriptor.getWriteMethod().invoke(originalObject, value);
            return;
        }
        Field field = fields.get(normalize(name));
        if (field != null) {
            field.setAccessible(true);
            field.set(originalObject, value);
            return;
        }
        throw new NoSuchFieldException(name);
    }

    public boolean hasGetter(String name) {
        PropertyDescriptor descriptor = propertyDescriptors.get(normalize(name));
        if (descriptor != null && descriptor.getReadMethod() != null) {
            return true;
        }
        return fields.containsKey(normalize(name));
    }

    public boolean hasSetter(String name) {
        PropertyDescriptor descriptor = propertyDescriptors.get(normalize(name));
        if (descriptor != null && descriptor.getWriteMethod() != null) {
            return true;
        }
        return fields.containsKey(normalize(name));
    }

    public Class<?> getSetterType(String name) throws NoSuchFieldException {
        PropertyDescriptor descriptor = propertyDescriptors.get(normalize(name));
        if (descriptor != null && descriptor.getWriteMethod() != null) {
            return descriptor.getPropertyType();
        }
        Field field = fields.get(normalize(name));
        if (field != null) {
            return field.getType();
        }
        throw new NoSuchFieldException(name);
    }

    private Map<String, PropertyDescriptor> propertyDescriptorMap(Class<?> type) {
        try {
            Map<String, PropertyDescriptor> descriptors = new HashMap<>();
            for (PropertyDescriptor descriptor : Introspector.getBeanInfo(type).getPropertyDescriptors()) {
                descriptors.put(normalize(descriptor.getName()), descriptor);
            }
            return descriptors;
        } catch (IntrospectionException ex) {
            throw new IllegalStateException("Failed to inspect bean properties for " + type.getName(), ex);
        }
    }

    private Map<String, Field> fieldMap(Class<?> type) {
        Map<String, Field> fieldMap = new HashMap<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                fieldMap.putIfAbsent(normalize(field.getName()), field);
            }
            current = current.getSuperclass();
        }
        return fieldMap;
    }

    private String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
