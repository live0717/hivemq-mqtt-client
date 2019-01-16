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

package org.mqttbee.internal.mqtt.handler.publish.incoming;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.converter.SimpleArgumentConverter;
import org.junit.jupiter.params.provider.CsvSource;
import org.mqttbee.internal.mqtt.datatypes.MqttTopicFilterImpl;
import org.mqttbee.internal.mqtt.datatypes.MqttTopicImpl;
import org.mqttbee.internal.util.collections.HandleList;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Silvio Giebl
 */
abstract class MqttSubscriptionFlowsTest {

    public static class CsvToArray extends SimpleArgumentConverter {

        @Override
        protected @NotNull Object convert(final @NotNull Object source, final @NotNull Class<?> targetType)
                throws ArgumentConversionException {
            final String s = (String) source;
            return s.split("\\s*;\\s*");
        }
    }

    private final @NotNull Supplier<MqttSubscriptionFlows> flowsSupplier;
    @SuppressWarnings("NullabilityAnnotations")
    private MqttSubscriptionFlows flows;

    MqttSubscriptionFlowsTest(final @NotNull Supplier<MqttSubscriptionFlows> flowsSupplier) {
        this.flowsSupplier = flowsSupplier;
    }

    @BeforeEach
    void setUp() {
        flows = flowsSupplier.get();
    }

    @ParameterizedTest
    @CsvSource({
            "a,    a; +; a/#; +/#; #                                     ",
            "a/b,  a/b; a/+; +/b; +/+; a/b/#; a/+/#; +/b/#; +/+/#; a/#; #",
            "/,    /; +/+; +/; /+; +/#; /#; #                            "
    })
    void subscribe_matchingTopicFilters_doMatch(
            final @NotNull String topic, @ConvertWith(CsvToArray.class) final @NotNull String[] matchingTopicFilters) {

        final MqttSubscriptionFlow[] matchingFlows = new MqttSubscriptionFlow[matchingTopicFilters.length];
        for (int i = 0; i < matchingTopicFilters.length; i++) {
            matchingFlows[i] = mockSubscriptionFlow(matchingTopicFilters[i]);
            flows.subscribe(MqttTopicFilterImpl.of(matchingTopicFilters[i]), matchingFlows[i]);
        }

        final HandleList<MqttIncomingPublishFlow> matching = new HandleList<>();
        flows.findMatching(MqttTopicImpl.of(topic), matching);

        assertFalse(matching.isEmpty());
        assertEquals(ImmutableSet.copyOf(matchingFlows), ImmutableSet.copyOf(matching));
    }

    @ParameterizedTest
    @CsvSource({
            "a,    /a; b; a/b; a/+; +/a; +/+; a/b/#; /#; /               ",
            "a/b,  /a/b; a/c; c/b; a/b/c; +/a/b; a/+/b; a/b/+; a/b/c/#; +",
            "/,    //; a/b; a/; /a; +                                    "
    })
    void subscribe_nonMatchingTopicFilters_doNotMatch(
            final @NotNull String topic,
            @ConvertWith(CsvToArray.class) final @NotNull String[] notMatchingTopicFilters) {

        final MqttSubscriptionFlow[] notMatchingFlows = new MqttSubscriptionFlow[notMatchingTopicFilters.length];
        for (int i = 0; i < notMatchingTopicFilters.length; i++) {
            notMatchingFlows[i] = mockSubscriptionFlow(notMatchingTopicFilters[i]);
            flows.subscribe(MqttTopicFilterImpl.of(notMatchingTopicFilters[i]), notMatchingFlows[i]);
        }

        final HandleList<MqttIncomingPublishFlow> matching = new HandleList<>();
        flows.findMatching(MqttTopicImpl.of(topic), matching);

        assertTrue(matching.isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"a, a", "a, +", "a, #", "a/b, a/b", "a/b, a/+", "a/b, +/b", "a/b, +/+", "a/b, +/#", "a/b, #"})
    void unsubscribe_matchingTopicFilters_doNoLongerMatch(
            final @NotNull String topic, final @NotNull String matchingTopicFilter) {

        final MqttSubscriptionFlow flow1 = mockSubscriptionFlow(matchingTopicFilter);
        final MqttSubscriptionFlow flow2 = mockSubscriptionFlow(matchingTopicFilter);
        flows.subscribe(MqttTopicFilterImpl.of(matchingTopicFilter), flow1);
        flows.subscribe(MqttTopicFilterImpl.of(matchingTopicFilter), flow2);

        final HandleList<MqttSubscriptionFlow> unsubscribed = new HandleList<>();
        flows.unsubscribe(MqttTopicFilterImpl.of(matchingTopicFilter), unsubscribed::add);
        final HandleList<MqttIncomingPublishFlow> matching = new HandleList<>();
        flows.findMatching(MqttTopicImpl.of(topic), matching);

        assertTrue(matching.isEmpty());
        assertEquals(ImmutableSet.of(flow1, flow2), ImmutableSet.copyOf(unsubscribed));
    }

    @ParameterizedTest
    @CsvSource({"a, a, b", "a, a, a/b", "a/b, a/b, a/c"})
    void unsubscribe_nonMatchingTopicFilters_othersStillMatch(
            final @NotNull String topic, final @NotNull String matchingTopicFilter,
            final @NotNull String notMatchingTopicFilter) {

        final MqttSubscriptionFlow flow1 = mockSubscriptionFlow(matchingTopicFilter);
        final MqttSubscriptionFlow flow2 = mockSubscriptionFlow(notMatchingTopicFilter);
        flows.subscribe(MqttTopicFilterImpl.of(matchingTopicFilter), flow1);
        flows.subscribe(MqttTopicFilterImpl.of(notMatchingTopicFilter), flow2);

        final HandleList<MqttSubscriptionFlow> unsubscribed = new HandleList<>();
        flows.unsubscribe(MqttTopicFilterImpl.of(notMatchingTopicFilter), unsubscribed::add);
        final HandleList<MqttIncomingPublishFlow> matching = new HandleList<>();
        flows.findMatching(MqttTopicImpl.of(topic), matching);

        assertFalse(matching.isEmpty());
        assertEquals(ImmutableSet.of(flow1), ImmutableSet.copyOf(matching));
        assertEquals(ImmutableSet.of(flow2), ImmutableSet.copyOf(unsubscribed));
    }

    @ParameterizedTest
    @CsvSource({"a, a", "a, +", "a, #", "a/b, a/b", "a/b, a/+", "a/b, +/b", "a/b, +/+", "a/b, +/#", "a/b, #"})
    void cancel_doNoLongerMatch(final @NotNull String topic, final @NotNull String matchingTopicFilter) {
        final MqttSubscriptionFlow flow1 = mockSubscriptionFlow(matchingTopicFilter);
        final MqttSubscriptionFlow flow2 = mockSubscriptionFlow(matchingTopicFilter);
        flows.subscribe(MqttTopicFilterImpl.of(matchingTopicFilter), flow1);
        flows.subscribe(MqttTopicFilterImpl.of(matchingTopicFilter), flow2);

        flows.cancel(flow1);
        HandleList<MqttIncomingPublishFlow> matching = new HandleList<>();
        flows.findMatching(MqttTopicImpl.of(topic), matching);

        assertFalse(matching.isEmpty());
        assertEquals(ImmutableSet.of(flow2), ImmutableSet.copyOf(matching));

        flows.cancel(flow2);
        matching = new HandleList<>();
        flows.findMatching(MqttTopicImpl.of(topic), matching);

        assertTrue(matching.isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"a, a", "a, +", "a, #", "a/b, a/b", "a/b, a/+", "a/b, +/b", "a/b, +/+", "a/b, +/#", "a/b, #"})
    void cancel_notPresentFlows_areIgnored(final @NotNull String topic, final @NotNull String matchingTopicFilter) {
        final MqttSubscriptionFlow flow1 = mockSubscriptionFlow(matchingTopicFilter);
        final MqttSubscriptionFlow flow2 = mockSubscriptionFlow(matchingTopicFilter);
        flows.subscribe(MqttTopicFilterImpl.of(matchingTopicFilter), flow1);

        flows.cancel(flow2);
        final HandleList<MqttIncomingPublishFlow> matching = new HandleList<>();
        flows.findMatching(MqttTopicImpl.of(topic), matching);

        assertFalse(matching.isEmpty());
        assertEquals(ImmutableSet.of(flow1), ImmutableSet.copyOf(matching));
    }

    @NotNull
    private MqttSubscriptionFlow mockSubscriptionFlow(final @NotNull String name) {
        final MqttSubscriptionFlow flow = mock(MqttSubscriptionFlow.class);
        when(flow.getTopicFilters()).thenReturn(new HandleList<>());
        when(flow.toString()).thenReturn(name);
        return flow;
    }

}