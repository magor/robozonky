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

package com.github.robozonky.strategy.natural;

import java.util.Optional;

import com.github.robozonky.api.remote.enums.Rating;

class LoanRatingWorseCondition extends MarketplaceFilterConditionImpl {

    private final Rating bestPossibleRating;

    public LoanRatingWorseCondition(final Rating r) {
        if (r == null) {
            throw new IllegalArgumentException("Rating must be provided.");
        }
        this.bestPossibleRating = r;
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.of("Threshold: " + bestPossibleRating + ".");
    }

    @Override
    public boolean test(final Wrapper item) {
        return item.getRating().compareTo(bestPossibleRating) > 0;
    }
}
