package ua.home.heatingautomation2026.service.impl.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.integration.annotation.ServiceActivator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import ua.home.heatingautomation2026.dto.TelemetryRequest;
import ua.home.heatingautomation2026.service.DeviceConnectivityService;
import ua.home.heatingautomation2026.service.DeviceService;
import ua.home.heatingautomation2026.service.TelemetryService;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttMessageHandler {

    private static final String TOPIC_PREFIX = "heating/";

    private final ObjectMapper objectMapper;
    private final DeviceService deviceService;
    private final DeviceConnectivityService connectivityService;
    private final TelemetryService telemetryService;

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<?> message) {
        Object payload = message.getPayload();

        String topic = String.valueOf(
                message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC)
        );

        try {
            String json = payloadToString(payload);
            String deviceId = extractDeviceId(topic);

            if (topic.endsWith("/status")) {
                handleStatus(deviceId, json);
            } else if (topic.endsWith("/telemetry")) {
                handleTelemetry(deviceId, json);
            } else {
                log.debug("Ignoring MQTT topic: {}", topic);
            }
        } catch (Exception ex) {
            log.error(
                    "Failed to process MQTT message from topic {}: {}",
                    topic,
                    ex.getMessage(),
                    ex
            );
        }
    }

    private void handleStatus(String deviceId, String json)
            throws Exception {

        JsonNode root = objectMapper.readTree(json);

        if (root == null
                || !root.has("online")
                || !root.get("online").isBoolean()) {

            throw new IllegalArgumentException(
                    "Status message must contain boolean field 'online'"
            );
        }

        // Не дозволяємо невідомим пристроям потрапляти в кеш.
        deviceService.findDevice(deviceId);

        boolean online = root.get("online").booleanValue();

        if (online) {
            connectivityService.markOnline(deviceId);
            log.info("MQTT device ONLINE: {}", deviceId);
        } else {
            connectivityService.markOffline(deviceId);
            log.info("MQTT device OFFLINE: {}", deviceId);
        }
    }

    private void handleTelemetry(String topicDeviceId, String json)
            throws Exception {

        JsonNode root = objectMapper.readTree(json);

        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException(
                    "Telemetry payload must be a JSON object"
            );
        }

        JsonNode payloadDeviceId = root.get("deviceId");

        if (payloadDeviceId != null
                && payloadDeviceId.isTextual()
                && !topicDeviceId.equals(payloadDeviceId.textValue())) {

            throw new IllegalArgumentException(
                    "Device ID in topic does not match payload"
            );
        }

        /*
         * Підтримуємо обидва формати:
         * {"values": {...}}
         * {"temperatures": {...}}
         */
        JsonNode valuesNode = root.has("values")
                ? root.get("values")
                : root.get("temperatures");

        if (valuesNode == null || !valuesNode.isObject()) {
            throw new IllegalArgumentException(
                    "Telemetry must contain object 'values' or 'temperatures'"
            );
        }

        Map<String, JsonNode> values = new LinkedHashMap<>();

        Iterator<Map.Entry<String, JsonNode>> fields =
                valuesNode.properties().iterator();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            values.put(field.getKey(), field.getValue());
        }

        Instant measuredAt = Instant.now();

        JsonNode measuredAtNode = root.get("measuredAt");

        if (measuredAtNode != null && measuredAtNode.isTextual()) {
            measuredAt = Instant.parse(measuredAtNode.textValue());
        }

        TelemetryRequest request =
                new TelemetryRequest(measuredAt, values);

        int count = telemetryService.ingest(topicDeviceId, request);

        // Телеметрія також підтверджує активність пристрою.
        connectivityService.markOnline(topicDeviceId);

        log.debug(
                "Received {} telemetry readings from device {}",
                count,
                topicDeviceId
        );
    }

    private String extractDeviceId(String topic) {
        if (topic == null || !topic.startsWith(TOPIC_PREFIX)) {
            throw new IllegalArgumentException(
                    "Unexpected MQTT topic: " + topic
            );
        }

        String[] parts = topic.split("/");

        if (parts.length != 3
                || parts[1].isBlank()
                || !(parts[2].equals("status")
                || parts[2].equals("telemetry"))) {

            throw new IllegalArgumentException(
                    "Invalid MQTT topic structure: " + topic
            );
        }

        return parts[1];
    }

    private String payloadToString(Object payload) {
        if (payload instanceof String stringPayload) {
            return stringPayload;
        }

        if (payload instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }

        throw new IllegalArgumentException(
                "Unsupported MQTT payload type: "
                        + payload.getClass().getName()
        );
    }
}