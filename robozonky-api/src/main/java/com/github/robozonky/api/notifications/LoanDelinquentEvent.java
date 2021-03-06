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

import java.time.LocalDate;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;

/**
 * Fired immediately after an {@link Investment} is identified as delinquent.
 */
public abstract class LoanDelinquentEvent extends Event {

    private final Loan loan;
    private final LocalDate since;
    private final int thresholdInDays;

    public LoanDelinquentEvent(final Loan loan, final LocalDate since, final int thresholdInDays) {
        this.loan = loan;
        this.since = since;
        this.thresholdInDays = thresholdInDays;
    }

    public int getThresholdInDays() {
        return thresholdInDays;
    }

    public Loan getLoan() {
        return loan;
    }

    public LocalDate getDelinquentSince() {
        return since;
    }
}
