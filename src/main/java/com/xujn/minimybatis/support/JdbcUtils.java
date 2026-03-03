package com.xujn.minimybatis.support;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * JDBC helper methods used by the phase 1 executor.
 *
 * <p>Responsibility: keep type checks and scalar conversions out of the query
 * pipeline so the executor stays focused on control flow.
 *
 * <p>Thread-safety: stateless and thread-safe.
 */
public final class JdbcUtils {

    private JdbcUtils() {
    }

    public static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || String.class.equals(type)
                || Number.class.isAssignableFrom(type)
                || Boolean.class.equals(type)
                || Character.class.equals(type)
                || Date.class.isAssignableFrom(type)
                || LocalDate.class.equals(type)
                || LocalDateTime.class.equals(type)
                || BigDecimal.class.equals(type)
                || BigInteger.class.equals(type);
    }

    public static Object getColumnValue(ResultSet resultSet, int columnIndex, Class<?> targetType) throws SQLException {
        return convertValue(resultSet.getObject(columnIndex), targetType);
    }

    public static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }
        if (targetType == Long.class || targetType == long.class) {
            return ((Number) value).longValue();
        }
        if (targetType == Integer.class || targetType == int.class) {
            return ((Number) value).intValue();
        }
        if (targetType == Short.class || targetType == short.class) {
            return ((Number) value).shortValue();
        }
        if (targetType == Byte.class || targetType == byte.class) {
            return ((Number) value).byteValue();
        }
        if (targetType == Double.class || targetType == double.class) {
            return ((Number) value).doubleValue();
        }
        if (targetType == Float.class || targetType == float.class) {
            return ((Number) value).floatValue();
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return value instanceof Boolean bool ? bool : Boolean.parseBoolean(String.valueOf(value));
        }
        if (targetType == String.class) {
            return String.valueOf(value);
        }
        if (targetType == BigDecimal.class) {
            return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value));
        }
        if (targetType == BigInteger.class) {
            return value instanceof BigInteger integer ? integer : new BigInteger(String.valueOf(value));
        }
        if (targetType == Character.class || targetType == char.class) {
            String string = String.valueOf(value);
            return string.isEmpty() ? null : string.charAt(0);
        }
        return value;
    }
}
