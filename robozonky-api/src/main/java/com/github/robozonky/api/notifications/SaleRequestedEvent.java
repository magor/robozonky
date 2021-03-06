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

package com.github.robozonky.api.notifications;

import com.github.robozonky.api.remote.ControlApi;
import com.github.robozonky.api.remote.entities.SellRequest;
import com.github.robozonky.api.strategies.RecommendedInvestment;

/**
 * Fired immediately before {@link ControlApi#offer(SellRequest)} call is made or, in case of dry run,
 * immediately before such a call would otherwise be made. Will be followed by {@link SaleOfferedEvent}.
 */
public final class SaleRequestedEvent extends Event {

    private final RecommendedInvestment recommendation;

    public SaleRequestedEvent(final RecommendedInvestment recommendation) {
        this.recommendation = recommendation;
    }

    public RecommendedInvestment getRecommendation() {
        return recommendation;
    }
}
