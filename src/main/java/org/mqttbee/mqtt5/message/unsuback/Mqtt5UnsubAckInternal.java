package org.mqttbee.mqtt5.message.unsuback;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.mqttbee.annotations.NotNull;
import org.mqttbee.mqtt5.codec.encoder.Mqtt5MessageEncoders;
import org.mqttbee.mqtt5.message.Mqtt5Message;
import org.mqttbee.mqtt5.message.Mqtt5MessageType;

/**
 * @author Silvio Giebl
 */
public class Mqtt5UnsubAckInternal implements Mqtt5Message {

    private final Mqtt5UnsubAckImpl unsubAck;
    private int packetIdentifier;

    public Mqtt5UnsubAckInternal(@NotNull final Mqtt5UnsubAckImpl unsubAck) {
        this.unsubAck = unsubAck;
    }

    @NotNull
    public Mqtt5UnsubAckImpl getUnsubAck() {
        return unsubAck;
    }

    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    public void setPacketIdentifier(final int packetIdentifier) {
        this.packetIdentifier = packetIdentifier;
    }

    @NotNull
    @Override
    public Mqtt5MessageType getType() {
        return Mqtt5MessageType.UNSUBACK;
    }

    @Override
    public void encode(
            @NotNull final Mqtt5MessageEncoders encoders, @NotNull final Channel channel, @NotNull final ByteBuf out) {
        encoders.getUnsubAckEncoder().encode(this, channel, out);
    }

}