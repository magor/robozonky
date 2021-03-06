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

package com.github.robozonky.app.configuration.daemon;

import java.time.Duration;

import com.github.robozonky.api.Refreshable;
import com.github.robozonky.api.marketplaces.Marketplace;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.investing.Investor;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class InvestmentDaemonTest {

    @Test
    public void standard() {
        final Authenticated a = Mockito.mock(Authenticated.class);
        final Marketplace m = Mockito.mock(Marketplace.class);
        final Refreshable<InvestmentStrategy> r = Refreshable.createImmutable(null);
        InvestingDaemon d = new InvestingDaemon(a, new Investor.Builder(), m, r, Duration.ZERO);
        d.run();
        d.run();
        Mockito.verify(m, Mockito.times(2)).run();
        Mockito.verify(m, Mockito.times(1)).registerListener(ArgumentMatchers.any());
    }
}
