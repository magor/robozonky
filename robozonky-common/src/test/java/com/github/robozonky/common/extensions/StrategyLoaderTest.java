/*
 * Copyright 2017 The RoboZonky Project
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
 */

package com.github.robozonky.common.extensions;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.api.strategies.StrategyService;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class StrategyLoaderTest {

    @Test
    public void unknown() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(StrategyLoader.toInvest(UUID.randomUUID().toString())).isEmpty();
            softly.assertThat(StrategyLoader.toSell(UUID.randomUUID().toString())).isEmpty();
            softly.assertThat(StrategyLoader.toPurchase(UUID.randomUUID().toString())).isEmpty();
        });
    }

    @Test
    public void failedProcessing() {
        final StrategyService iss = Mockito.mock(StrategyService.class);
        Mockito.doThrow(new IllegalStateException("Testing")).when(iss).toInvest(ArgumentMatchers.any());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(StrategyLoader.processStrategyService(iss, "", StrategyService::toInvest))
                    .isEmpty();
            softly.assertThat(StrategyLoader.processStrategyService(iss, "", StrategyService::toSell))
                    .isEmpty();
            softly.assertThat(StrategyLoader.processStrategyService(iss, "", StrategyService::toPurchase))
                    .isEmpty();
        });
    }

    @Test
    public void standardProcessing() {
        final StrategyService iss = Mockito.mock(StrategyService.class);
        Mockito.when(iss.toInvest(ArgumentMatchers.any())).thenReturn(
                Optional.of(Mockito.mock(InvestmentStrategy.class)));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(StrategyLoader.processStrategyService(iss, "", StrategyService::toInvest))
                    .isPresent();
            softly.assertThat(StrategyLoader.processStrategyService(iss, "", StrategyService::toSell))
                    .isEmpty();
            softly.assertThat(StrategyLoader.processStrategyService(iss, "", StrategyService::toPurchase))
                    .isEmpty();
        });
    }

    @Test
    public void loading() {
        final InvestmentStrategy is = (availableLoans, portfolio) -> Stream.empty();
        final StrategyService iss = new StrategyService() {
            @Override
            public Optional<InvestmentStrategy> toInvest(final String strategy) {
                return Optional.of(is);
            }

            @Override
            public Optional<SellStrategy> toSell(final String strategy) {
                return Optional.empty();
            }

            @Override
            public Optional<PurchaseStrategy> toPurchase(final String strategy) {
                return Optional.empty();
            }
        };
        Assertions.assertThat(StrategyLoader.load("", Collections.singleton(iss), StrategyService::toInvest))
                .contains(is);
    }
}
