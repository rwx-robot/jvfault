/**
 * jvfault transport-mqtt —— MQTT 传输（Eclipse Paho mqttv3）。
 *
 * <p>多版本 JAR 的 Java 9 描述符（主代码 --release 8）。
 *
 * @since v1.0.3 (2026)
 */
module com.jvfault.transport.mqtt {

    requires transitive com.jvfault.microservices;
    requires org.eclipse.paho.client.mqttv3;

    exports com.jvfault.transport.mqtt;
}