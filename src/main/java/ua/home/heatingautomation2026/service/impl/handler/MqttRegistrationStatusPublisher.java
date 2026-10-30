package ua.home.heatingautomation2026.service.impl.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import ua.home.heatingautomation2026.entity.Device;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttRegistrationStatusPublisher {

    private static final String TOPIC_PREFIX = "heating/";

    private final ObjectMapper objectMapper;
    private final MessageChannel mqttOutboundChannel;

    public void publish(Device device) {
        String deviceId = device.getDeviceId();

        String topic = TOPIC_PREFIX
                + deviceId
                + "/registration-status";

        Map<String, Object> payload = Map.of(
                "deviceId", deviceId,
                "deviceRegistrationStatus",
                device.getDeviceRegistrationStatus().name()
        );

        try {
            String json = objectMapper.writeValueAsString(payload);

            boolean sent = mqttOutboundChannel.send(
                    MessageBuilder.withPayload(json)
                            .setHeader(MqttHeaders.TOPIC, topic)
                            .setHeader(MqttHeaders.QOS, 1)
                            .setHeader(MqttHeaders.RETAINED, true)
                            .build()
            );

            if (!sent) {
                log.error(
                        "Failed to enqueue registration status for device {}",
                        deviceId
                );
            } else {
                log.info(
                        "Published registration status for {}: {}",
                        deviceId,
                        device.getDeviceRegistrationStatus()
                );
            }
        } catch (JacksonException ex) {
            throw new IllegalStateException(
                    "Cannot serialize registration status for " + deviceId,
                    ex
            );
        }
    }
}