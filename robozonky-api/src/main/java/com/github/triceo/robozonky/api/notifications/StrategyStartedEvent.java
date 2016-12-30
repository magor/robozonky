/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.api.notifications;

import java.util.Collection;
import java.util.Collections;

import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;

/**
 * Fired before any loans are submitted for evaluation by the strategy and subsequent investment operations. May be
 * followed by {@link LoanRecommendedEvent} and will eventually be followed by {@link StrategyCompletedEvent}.
 */
public final class StrategyStartedEvent implements Event {

    private final InvestmentStrategy strategyToBeUsed;
    private final Collection<LoanDescriptor> loanDescriptors;
    private final int startingBalance;

    public StrategyStartedEvent(final InvestmentStrategy strategyToBeUsed,
                                final Collection<LoanDescriptor> loanDescriptors, final int startingBalance) {
        this.strategyToBeUsed = strategyToBeUsed;
        this.loanDescriptors = Collections.unmodifiableCollection(loanDescriptors);
        this.startingBalance = startingBalance;
    }

    public InvestmentStrategy getStrategyToBeUsed() {
        return strategyToBeUsed;
    }

    public Collection<LoanDescriptor> getLoanDescriptors() {
        return loanDescriptors;
    }

    public int getStartingBalance() {
        return startingBalance;
    }
}
