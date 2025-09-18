package top.redjujubetree.grpc.tunnel.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import top.redjujubetree.grpc.tunnel.proto.RequestPayload;

/**
 * Utility class for handling tunnel messages, including serialization and deserialization.
 */
@Slf4j
public class TunnelMessagesUtil {

    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    private static final ObjectMapper NON_NULL_OBJECT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);


    public static String serializeObj(Object obj) {
        return serializeObjExcludeNulls(obj);
    }

    public static String serializeObjWithNulls(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            log.debug("Serializing object of type: {}", obj.getClass().getSimpleName());
            return NON_NULL_OBJECT_MAPPER.writeValueAsString(obj);
        }catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object: " + e.getMessage(), e);
        }
    }

    public static String serializeObjExcludeNulls(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            log.debug("Serializing object of type: {}", obj.getClass().getName());
            return NON_NULL_OBJECT_MAPPER.writeValueAsString(obj);
        }catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object: " + e.getMessage(), e);
        }
    }

    public static <T> T deserializeRequest(RequestPayload request, Class<T> clazz) {
        try {
            String data = request.getData().toStringUtf8();
            return DEFAULT_OBJECT_MAPPER.readValue(data, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize request data: " + e.getMessage(), e);
        }
    }
    
    public static <T> T deserializeRequest(RequestPayload request, TypeReference<T> typeReference) {
        try {
            String data = request.getData().toStringUtf8();
            return DEFAULT_OBJECT_MAPPER.readValue(data, typeReference);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize request data: " + e.getMessage(), e);
        }
    }
}