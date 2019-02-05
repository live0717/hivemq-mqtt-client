/*
 * Copyright 2018 The MQTT Bee project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.mqttbee.internal.mqtt.advanced;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mqttbee.internal.mqtt.advanced.interceptor.MqttClientInterceptors;
import org.mqttbee.internal.mqtt.advanced.interceptor.MqttClientInterceptorsBuilder;
import org.mqttbee.internal.util.Checks;
import org.mqttbee.mqtt.mqtt5.advanced.Mqtt5ClientAdvancedConfigBuilder;
import org.mqttbee.mqtt.mqtt5.advanced.interceptor.Mqtt5ClientInterceptors;

import java.util.function.Function;

/**
 * @author Silvio Giebl
 */
public abstract class MqttClientAdvancedConfigBuilder<B extends MqttClientAdvancedConfigBuilder<B>> {

    private boolean allowServerReAuth;
    private @Nullable MqttClientInterceptors interceptors;

    abstract @NotNull B self();

    public @NotNull B allowServerReAuth(final boolean allowServerReAuth) {
        this.allowServerReAuth = allowServerReAuth;
        return self();
    }

    public @NotNull B interceptors(final @Nullable Mqtt5ClientInterceptors interceptors) {
        this.interceptors = Checks.notImplementedOrNull(interceptors, MqttClientInterceptors.class, "Interceptors");
        return self();
    }

    public @NotNull MqttClientInterceptorsBuilder.Nested<B> interceptors() {
        return new MqttClientInterceptorsBuilder.Nested<>(this::interceptors);
    }

    public @NotNull MqttClientAdvancedConfig build() {
        return new MqttClientAdvancedConfig(allowServerReAuth, interceptors);
    }

    public static class Default extends MqttClientAdvancedConfigBuilder<Default>
            implements Mqtt5ClientAdvancedConfigBuilder {

        @Override
        @NotNull Default self() {
            return this;
        }
    }

    public static class Nested<P> extends MqttClientAdvancedConfigBuilder<Nested<P>>
            implements Mqtt5ClientAdvancedConfigBuilder.Nested<P> {

        private final @NotNull Function<? super MqttClientAdvancedConfig, P> parentConsumer;

        public Nested(final @NotNull Function<? super MqttClientAdvancedConfig, P> parentConsumer) {
            this.parentConsumer = parentConsumer;
        }

        @Override
        @NotNull Nested<P> self() {
            return this;
        }

        @Override
        public @NotNull P applyAdvancedConfig() {
            return parentConsumer.apply(build());
        }
    }
}