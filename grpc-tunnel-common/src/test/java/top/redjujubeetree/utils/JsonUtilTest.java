package top.redjujubeetree.utils;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import top.redjujubetree.grpc.tunnel.utils.JsonUtil;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 JSON 序列化和反序列化功能
 */
@DisplayName("JsonUtil 测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JsonUtilTest {

    // 测试用的简单对象类
    static class Person {
        private String name;
        private int age;
        private boolean active;
        
        public Person() {}
        
        public Person(String name, int age, boolean active) {
            this.name = name;
            this.age = age;
            this.active = active;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Person person = (Person) obj;
            return age == person.age && 
                   active == person.active && 
                   Objects.equals(name, person.name);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(name, age, active);
        }
        
        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age + ", active=" + active + "}";
        }
    }

    @BeforeAll
    static void setUp() {
        System.out.println("开始执行 JsonUtil 测试");
    }

    @AfterAll
    static void tearDown() {
        System.out.println("完成 JsonUtil 测试");
    }

    // ==================== 基本类型测试 ====================
    
    @Test
    @Order(1)
    @DisplayName("测试 null 值序列化和反序列化")
    void testNullValue() {
        // 序列化 null
        String json = JsonUtil.toJson(null);
        assertEquals("null", json);
        
        // 反序列化 null
        String result = JsonUtil.fromJson("null", String.class);
        assertNull(result);
        
        Integer intResult = JsonUtil.fromJson("null", Integer.class);
        assertNull(intResult);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, -1, 100, -999})
    @DisplayName("测试整数序列化和反序列化")
    void testIntegerSerialization(int value) {
        String json = JsonUtil.toJson(value);
        assertEquals(String.valueOf(value), json);
        
        Integer result = JsonUtil.fromJson(json, Integer.class);
        assertEquals(value, result);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 1.5, -2.7, 100.999})
    @DisplayName("测试浮点数序列化和反序列化")
    void testDoubleSerialization(double value) {
        String json = JsonUtil.toJson(value);
        assertEquals(String.valueOf(value), json);
        
        Double result = JsonUtil.fromJson(json, Double.class);
        assertEquals(value, result, 0.001);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("测试布尔值序列化和反序列化")
    void testBooleanSerialization(boolean value) {
        String json = JsonUtil.toJson(value);
        assertEquals(String.valueOf(value), json);
        
        Boolean result = JsonUtil.fromJson(json, Boolean.class);
        assertEquals(value, result);
    }

    // ==================== 字符串测试 ====================
    
    @Test
    @Order(2)
    @DisplayName("测试字符串序列化和反序列化")
    void testStringSerialization() {
        String original = "Hello World";
        String json = JsonUtil.toJson(original);
        assertEquals("\"Hello World\"", json);
        
        String result = JsonUtil.fromJson(json, String.class);
        assertEquals(original, result);
    }

    @Test
    @DisplayName("测试特殊字符串序列化")
    void testSpecialStringsSerialization() {
        String original = "Hello\n\"World\"\t\\Test";
        String json = JsonUtil.toJson(original);
        assertTrue(json.contains("\\n"));
        assertTrue(json.contains("\\\""));
        assertTrue(json.contains("\\t"));
        assertTrue(json.contains("\\\\"));
        
        String result = JsonUtil.fromJson(json, String.class);
        assertEquals(original, result);
    }

    // ==================== 数组测试 ====================
    
    @Test
    @Order(3)
    @DisplayName("测试整数数组序列化和反序列化")
    void testIntArraySerialization() {
        int[] original = {1, 2, 3, 4, 5};
        String json = JsonUtil.toJson(original);
        assertEquals("[1,2,3,4,5]", json);
        
        int[] result = JsonUtil.fromJson(json, int[].class);
        assertArrayEquals(original, result);
    }

    @Test
    @DisplayName("测试字符串数组序列化和反序列化")
    void testStringArraySerialization() {
        String[] original = {"apple", "banana", "cherry"};
        String json = JsonUtil.toJson(original);
        assertEquals("[\"apple\",\"banana\",\"cherry\"]", json);
        
        String[] result = JsonUtil.fromJson(json, String[].class);
        assertArrayEquals(original, result);
    }

    @Test
    @DisplayName("测试空数组序列化")
    void testEmptyArraySerialization() {
        int[] original = {};
        String json = JsonUtil.toJson(original);
        assertEquals("[]", json);
        
        int[] result = JsonUtil.fromJson(json, int[].class);
        assertArrayEquals(original, result);
    }

    // ==================== List 测试 ====================
    
    @Test
    @Order(4)
    @DisplayName("测试 List 序列化和反序列化")
    void testListSerialization() {
        List<String> original = Arrays.asList("one", "two", "three");
        String json = JsonUtil.toJson(original);
        assertEquals("[\"one\",\"two\",\"three\"]", json);
        
        List<Object> result = JsonUtil.fromJson(json, List.class);
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("one", result.get(0));
        assertEquals("two", result.get(1));
        assertEquals("three", result.get(2));
    }

    @Test
    @DisplayName("测试空 List 序列化")
    void testEmptyListSerialization() {
        List<String> original = new ArrayList<>();
        String json = JsonUtil.toJson(original);
        assertEquals("[]", json);
        
        List<Object> result = JsonUtil.fromJson(json, List.class);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== Map 测试 ====================
    
    @Test
    @Order(5)
    @DisplayName("测试 Map 序列化和反序列化")
    void testMapSerialization() {
        Map<String, Object> original = new HashMap<>();
        original.put("name", "张三");
        original.put("age", 25);
        original.put("active", true);
        
        String json = JsonUtil.toJson(original);
        assertNotNull(json);
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
        assertTrue(json.contains("\"name\":\"张三\""));
        assertTrue(json.contains("\"age\":25"));
        assertTrue(json.contains("\"active\":true"));
        
        Map<String, Object> result = JsonUtil.fromJson(json, Map.class);
        assertNotNull(result);
        assertEquals("张三", result.get("name"));
        assertEquals(25L, result.get("age")); // 注意：数字会被解析为 Long
        assertEquals(true, result.get("active"));
    }

    @Test
    @DisplayName("测试空 Map 序列化")
    void testEmptyMapSerialization() {
        Map<String, Object> original = new HashMap<>();
        String json = JsonUtil.toJson(original);
        assertEquals("{}", json);
        
        Map<String, Object> result = JsonUtil.fromJson(json, Map.class);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== 对象测试 ====================
    
    @Test
    @Order(6)
    @DisplayName("测试对象序列化和反序列化")
    void testObjectSerialization() {
        Person original = new Person("李四", 30, true);
        String json = JsonUtil.toJson(original);
        
        assertNotNull(json);
        assertTrue(json.contains("\"name\":\"李四\""));
        assertTrue(json.contains("\"age\":30"));
        assertTrue(json.contains("\"active\":true"));
        
        Person result = JsonUtil.fromJson(json, Person.class);
        assertNotNull(result);
        assertEquals(original, result);
    }

    @Test
    @DisplayName("测试带有 null 字段的对象")
    void testObjectWithNullFields() {
        Person original = new Person(null, 25, false);
        String json = JsonUtil.toJson(original);
        assertTrue(json.contains("\"name\":null"));
        
        Person result = JsonUtil.fromJson(json, Person.class);
        assertNotNull(result);
        assertNull(result.name);
        assertEquals(25, result.age);
        assertEquals(false, result.active);
    }

    // ==================== 复杂场景测试 ====================
    
    @Test
    @Order(7)
    @DisplayName("测试嵌套对象")
    void testNestedStructures() {
        Map<String, Object> nested = new HashMap<>();
        nested.put("numbers", Arrays.asList(1, 2, 3));
        nested.put("person", new Person("王五", 28, true));
        
        String json = JsonUtil.toJson(nested);
        assertNotNull(json);
        
        Map<String, Object> result = JsonUtil.fromJson(json, Map.class);
        assertNotNull(result);
        assertTrue(result.containsKey("numbers"));
        assertTrue(result.containsKey("person"));
    }

    // ==================== 异常测试 ====================
    
    @Test
    @Order(8)
    @DisplayName("测试无效 JSON 格式异常")
    void testInvalidJsonException() {
        assertThrows(IllegalArgumentException.class, () -> {
            JsonUtil.fromJson("{invalid json", Map.class);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            JsonUtil.fromJson("[1,2,3", List.class);
        });
    }

    @Test
    @DisplayName("测试空字符串和空白字符串")
    void testEmptyStrings() {
        assertNull(JsonUtil.fromJson("", String.class));
        assertNull(JsonUtil.fromJson("   ", String.class));
        assertNull(JsonUtil.fromJson(null, String.class));
    }

    // ==================== 性能测试（简单） ====================
    
    @Test
    @Order(9)
    @DisplayName("简单性能测试")
    void testSimplePerformance() {
        Person person = new Person("性能测试", 100, true);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 1000; i++) {
            String json = JsonUtil.toJson(person);
            Person result = JsonUtil.fromJson(json, Person.class);
            assertNotNull(result);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("1000 次序列化/反序列化耗时: " + duration + "ms");
        assertTrue(duration < 5000, "性能测试：1000次操作应该在5秒内完成");
    }
}