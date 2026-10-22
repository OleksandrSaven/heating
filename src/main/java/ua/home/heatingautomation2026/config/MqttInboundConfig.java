package ua.home.heatingautomation2026.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.messaging.MessageChannel;

import java.util.UUID;

@Configuration
@EnableIntegration
public class MqttInboundConfig {

    @Bean
    public MqttPahoClientFactory mqttClientFactory(
            @Value("${mqtt.broker-url}") String brokerUrl,
            @Value("${mqtt.username}") String username,
            @Value("${mqtt.password}") String password) {

        MqttConnectOptions options = new MqttConnectOptions();

        options.setServerURIs(new String[]{brokerUrl});
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);

        DefaultMqttPahoClientFactory factory =
                new DefaultMqttPahoClientFactory();

        factory.setConnectionOptions(options);

        return factory;
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter(
            MqttPahoClientFactory mqttClientFactory,
            MessageChannel mqttInputChannel,
            @Value("${mqtt.topic.telemetry}") String telemetryTopic,
            @Value("${mqtt.topic.status}") String statusTopic) {

        String clientId =
                "heating-automation-" + UUID.randomUUID();

        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(
                        clientId,
                        mqttClientFactory,
                        telemetryTopic,
                        statusTopic
                );

        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel);

        return adapter;
    }
}