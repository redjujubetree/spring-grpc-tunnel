package top.redjujubetree.grpc.tunnel.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Simple JSON utility class
 * Supports serialization and deserialization of primitive types, String, List, Map, arrays, and plain Java objects
 *
 * Notes:
 *
 * 1. This is a simplified implementation that doesn't handle all edge cases
 * 2. Classes for deserialization must have a no-argument constructor
 * 3. Circular references are not supported
 * 4. Performance is not as good as professional JSON libraries (Gson, Jackson)
 * 5. Complex generic types are not handled
 */
public class JsonUtil {
    
    /**
     * Serialize object to JSON string
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        Class<?> clazz = obj.getClass();
        
        // Handle primitive types and wrapper classes
        if (isPrimitive(clazz)) {
            return obj.toString();
        }
        
        // Handle strings
        if (obj instanceof String) {
            return "\"" + escapeString((String) obj) + "\"";
        }
        
        // Handle arrays
        if (clazz.isArray()) {
            return arrayToJson(obj);
        }
        
        // Handle List
        if (obj instanceof List) {
            return listToJson((List<?>) obj);
        }
        
        // Handle Map
        if (obj instanceof Map) {
            return mapToJson((Map<?, ?>) obj);
        }
        
        // Handle plain objects
        return objectToJson(obj);
    }
    
    /**
     * Deserialize JSON string to object of specified type
     */
    @SuppressWarnings("unchecked")
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty() || "null".equals(json.trim())) {
            return null;
        }
        
        json = json.trim();
        
        // Handle primitive types
        if (isPrimitive(clazz)) {
            return (T) parsePrimitive(json, clazz);
        }
        
        // Handle strings
        if (clazz == String.class) {
            if (json.startsWith("\"") && json.endsWith("\"")) {
                return (T) unescapeString(json.substring(1, json.length() - 1));
            }
            throw new IllegalArgumentException("Invalid JSON string: " + json);
        }
        
        // Handle arrays
        if (clazz.isArray()) {
            return (T) parseArray(json, clazz);
        }
        
        // Handle List
        if (List.class.isAssignableFrom(clazz)) {
            return (T) parseList(json);
        }
        
        // Handle Map
        if (Map.class.isAssignableFrom(clazz)) {
            return (T) parseMap(json);
        }
        
        // Handle plain objects
        return parseObject(json, clazz);
    }
    
    // ==================== Serialization Helper Methods ====================
    
    private static String arrayToJson(Object array) {
        StringBuilder sb = new StringBuilder("[");
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(Array.get(array, i)));
        }
        sb.append("]");
        return sb.toString();
    }
    
    private static String listToJson(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }
    
    private static String mapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            sb.append(toJson(entry.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }
    
    private static String objectToJson(Object obj) {
        StringBuilder sb = new StringBuilder("{");
        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();
        boolean first = true;
        
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(field.getName()).append("\":");
                sb.append(toJson(value));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        sb.append("}");
        return sb.toString();
    }
    
    // ==================== Deserialization Helper Methods ====================
    
    private static Object parseArray(String json, Class<?> arrayClass) {
        List<String> elements = parseJsonArray(json);
        Class<?> componentType = arrayClass.getComponentType();
        Object array = Array.newInstance(componentType, elements.size());
        
        for (int i = 0; i < elements.size(); i++) {
            Array.set(array, i, fromJson(elements.get(i), componentType));
        }
        return array;
    }
    
    private static List<Object> parseList(String json) {
        List<String> elements = parseJsonArray(json);
        List<Object> list = new ArrayList<>();
        
        for (String element : elements) {
            list.add(parseValue(element));
        }
        return list;
    }
    
    private static Map<String, Object> parseMap(String json) {
        Map<String, String> pairs = parseJsonObject(json);
        Map<String, Object> map = new HashMap<>();
        
        for (Map.Entry<String, String> entry : pairs.entrySet()) {
            map.put(entry.getKey(), parseValue(entry.getValue()));
        }
        return map;
    }
    
    private static <T> T parseObject(String json, Class<T> clazz) {
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            Map<String, String> pairs = parseJsonObject(json);
            
            for (Map.Entry<String, String> entry : pairs.entrySet()) {
                try {
                    Field field = clazz.getDeclaredField(entry.getKey());
                    field.setAccessible(true);
                    Object value = fromJson(entry.getValue(), field.getType());
                    field.set(instance, value);
                } catch (NoSuchFieldException e) {
                    // Ignore non-existent fields
                }
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse object", e);
        }
    }
    
    private static Object parseValue(String json) {
        json = json.trim();
        
        if ("null".equals(json)) {
            return null;
        } else if ("true".equals(json) || "false".equals(json)) {
            return Boolean.parseBoolean(json);
        } else if (json.startsWith("\"") && json.endsWith("\"")) {
            return unescapeString(json.substring(1, json.length() - 1));
        } else if (json.startsWith("[")) {
            return parseList(json);
        } else if (json.startsWith("{")) {
            return parseMap(json);
        } else {
            // Try to parse as number
            try {
                if (json.contains(".")) {
                    return Double.parseDouble(json);
                } else {
                    return Long.parseLong(json);
                }
            } catch (NumberFormatException e) {
                return json;
            }
        }
    }
    
    // ==================== JSON Parsing Helper Methods ====================
    
    private static List<String> parseJsonArray(String json) {
        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) {
            throw new IllegalArgumentException("Invalid JSON array: " + json);
        }
        
        List<String> elements = new ArrayList<>();
        String content = json.substring(1, json.length() - 1).trim();
        
        if (content.isEmpty()) {
            return elements;
        }
        
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        StringBuilder current = new StringBuilder();
        
        for (char c : content.toCharArray()) {
            if (!escaped && c == '\\') {
                escaped = true;
                current.append(c);
                continue;
            }
            
            if (!escaped && c == '"') {
                inString = !inString;
            }
            
            if (!inString && !escaped) {
                if (c == '[' || c == '{') depth++;
                if (c == ']' || c == '}') depth--;
                if (c == ',' && depth == 0) {
                    elements.add(current.toString().trim());
                    current = new StringBuilder();
                    continue;
                }
            }
            
            current.append(c);
            escaped = false;
        }
        
        if (current.length() > 0) {
            elements.add(current.toString().trim());
        }
        
        return elements;
    }
    
    private static Map<String, String> parseJsonObject(String json) {
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            throw new IllegalArgumentException("Invalid JSON object: " + json);
        }
        
        Map<String, String> pairs = new LinkedHashMap<>();
        String content = json.substring(1, json.length() - 1).trim();
        
        if (content.isEmpty()) {
            return pairs;
        }
        
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        StringBuilder current = new StringBuilder();
        
        for (char c : content.toCharArray()) {
            if (!escaped && c == '\\') {
                escaped = true;
                current.append(c);
                continue;
            }
            
            if (!escaped && c == '"') {
                inString = !inString;
            }
            
            if (!inString && !escaped) {
                if (c == '[' || c == '{') depth++;
                if (c == ']' || c == '}') depth--;
                if (c == ',' && depth == 0) {
                    parsePair(current.toString().trim(), pairs);
                    current = new StringBuilder();
                    continue;
                }
            }
            
            current.append(c);
            escaped = false;
        }
        
        if (current.length() > 0) {
            parsePair(current.toString().trim(), pairs);
        }
        
        return pairs;
    }
    
    private static void parsePair(String pair, Map<String, String> pairs) {
        int colonIndex = pair.indexOf(':');
        if (colonIndex == -1) {
            throw new IllegalArgumentException("Invalid key-value pair: " + pair);
        }
        
        String key = pair.substring(0, colonIndex).trim();
        String value = pair.substring(colonIndex + 1).trim();
        
        if (key.startsWith("\"") && key.endsWith("\"")) {
            key = key.substring(1, key.length() - 1);
        }
        
        pairs.put(key, value);
    }
    
    // ==================== Utility Methods ====================
    
    private static boolean isPrimitive(Class<?> clazz) {
        return clazz.isPrimitive() || 
               clazz == Integer.class || 
               clazz == Long.class ||
               clazz == Double.class || 
               clazz == Float.class ||
               clazz == Boolean.class || 
               clazz == Byte.class ||
               clazz == Short.class || 
               clazz == Character.class;
    }
    
    private static Object parsePrimitive(String value, Class<?> clazz) {
        if (clazz == int.class || clazz == Integer.class) {
            return Integer.parseInt(value);
        } else if (clazz == long.class || clazz == Long.class) {
            return Long.parseLong(value);
        } else if (clazz == double.class || clazz == Double.class) {
            return Double.parseDouble(value);
        } else if (clazz == float.class || clazz == Float.class) {
            return Float.parseFloat(value);
        } else if (clazz == boolean.class || clazz == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (clazz == byte.class || clazz == Byte.class) {
            return Byte.parseByte(value);
        } else if (clazz == short.class || clazz == Short.class) {
            return Short.parseShort(value);
        } else if (clazz == char.class || clazz == Character.class) {
            return value.charAt(0);
        }
        throw new IllegalArgumentException("Unknown primitive type: " + clazz);
    }
    
    private static String escapeString(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    private static String unescapeString(String str) {
        return str.replace("\\\\", "\\")
                  .replace("\\\"", "\"")
                  .replace("\\n", "\n")
                  .replace("\\r", "\r")
                  .replace("\\t", "\t");
    }
}