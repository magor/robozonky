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
package com.github.triceo.robozonky;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.Defaults;
import com.github.triceo.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentRequestedEvent;
import com.github.triceo.robozonky.api.notifications.LoanRecommendedEvent;
import com.github.triceo.robozonky.api.notifications.StrategyCompletedEvent;
import com.github.triceo.robozonky.api.notifications.StrategyStartedEvent;
import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.api.remote.entities.BlockedAmount;
import com.github.triceo.robozonky.api.remote.entities.Instalment;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.entities.Statistics;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.api.strategies.PortfolioOverview;
import com.github.triceo.robozonky.api.strategies.Recommendation;
import com.github.triceo.robozonky.notifications.Events;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controls the investments based on the strategy, user portfolio and balance.
 */
public class Investor {

    private static final Logger LOGGER = LoggerFactory.getLogger(Investor.class);
    private static final Comparator<LoanDescriptor> LOAN_DESCRIPTOR_COMPARATOR =
            Comparator.comparing((LoanDescriptor l) -> l.getLoan().getId());
    private static final Function<LoanDescriptor, String> LOAN_ID_RETRIEVER = l -> String.valueOf(l.getLoan().getId());

    /**
     * The core investing call. Receives a particular loan, checks if the user has enough money to invest, and sends the
     * command to the Zonky API.
     *
     * @param recommendation Recommendation to invest.
     * @param api API to invest through.
     * @param balance The current balance.
     * @return Present if input valid and operation succeeded, empty otherwise.
     */
    static Optional<Investment> actuallyInvest(final Recommendation recommendation, final ZonkyProxy api,
                                               final BigDecimal balance) {
        final int amount = recommendation.getRecommendedInvestmentAmount();
        final int loanId = recommendation.getLoanDescriptor().getLoan().getId();
        if (balance.compareTo(BigDecimal.valueOf(amount)) < 0) { // strategy should not allow this
            Investor.LOGGER.info("Not investing into loan #{}, {} CZK to invest is more than {} CZK balance.", loanId,
                    amount, balance);
            return Optional.empty();
        }
        Events.fire(new InvestmentRequestedEvent(recommendation));
        final ZonkyResponse response = api.invest(recommendation);
        switch (response.getType()) {
            case FAILED:
                throw new IllegalStateException("Investment operation failed remotely.");
            case REJECTED:
                Events.fire(new InvestmentRejectedEvent(recommendation, balance.intValue(),
                        api.getConfirmationProvider().getId()));
                return Optional.empty();
            case DELEGATED:
                Events.fire(new InvestmentDelegatedEvent(recommendation, balance.intValue(),
                        api.getConfirmationProvider().getId()));
                return Optional.empty();
            case INVESTED:
                final int confirmedAmount = response.getConfirmedAmount().getAsInt();
                final Investment i = new Investment(recommendation.getLoanDescriptor().getLoan(), confirmedAmount);
                Events.fire(new InvestmentMadeEvent(i, balance.intValue() - confirmedAmount));
                return Optional.of(i);
            default:
                throw new IllegalStateException("Impossible.");
        }
    }

    /**
     * Blocked amounts represent loans in various stages. Either the user has invested and the loan has not yet been
     * funded to 100 % ("na tržišti"), or the user invested and the loan has been funded ("na cestě"). In the latter
     * case, the loan has already disappeared from the marketplace, which means that it will not be available for
     * investing any more. As far as I know, the next stage is "v pořádku", the blocked amount is cleared and the loan
     * becomes an active investment.
     *
     * Based on that, this method deals with the first case - when the loan is still available for investing, but we've
     * already invested as evidenced by the blocked amount. It also unnecessarily deals with the second case, since
     * that is represented by a blocked amount as well. But that does no harm.
     *
     * In case user has made repeated investments into a particular loan, this will show up as multiple blocked amounts.
     * The method needs to handle this as well.
     *
     * @param api Authenticated API that will be used to retrieve the user's blocked amounts from the wallet.
     * @return Every blocked amount represents a future investment. This method returns such investments.
     */
    static List<Investment> retrieveInvestmentsRepresentedByBlockedAmounts(final ZonkyProxy api) {
        // first group all blocked amounts by the loan ID and sum them
        final Map<Integer, Integer> amountsBlockedByLoans =
                api.execute(zonky -> zonky.getBlockedAmounts(Integer.MAX_VALUE, 0))
                        .stream()
                        .filter(blocked -> blocked.getLoanId() > 0) // 0 == Zonky investors' fee
                        .collect(Collectors.groupingBy(BlockedAmount::getLoanId,
                                Collectors.summingInt(BlockedAmount::getAmount)));
        // and then fetch all the loans in parallel, converting them into investments
        return Collections.unmodifiableList(amountsBlockedByLoans.entrySet().parallelStream()
                .map(entry -> LoanRetriever.getLoan(api, entry.getKey())
                        .map(l -> new Investment(l, entry.getValue()))
                        .orElseThrow(() -> new RuntimeException("Loan retrieval failed."))
                ).sorted(Comparator.comparingInt(l -> l.getId()))
                .collect(Collectors.toList()));
    }

    /**
     * Zonky API may return {@link ZonkyApi#getStatistics()} as null if the account has no previous investments.
     *
     * @param api API to execute the operation.
     * @return Either what the API returns, or an empty object.
     */
    private static Statistics retrieveStatistics(final ZonkyProxy api) {
        final Statistics returned = api.execute(ZonkyApi::getStatistics);
        return returned == null ? new Statistics() : returned;
    }

    private static <T> String collectionToString(final Collection<T> collection, final Comparator<T> comparator,
                                                 final Function<T, String> reduction) {
        return collection.stream()
                .sorted(comparator)
                .map(reduction)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private final ZonkyProxy api;
    private BigDecimal balance;

    /**
     * Standard constructor.
     * @param api Authenticated API ready to communicate with the server.
     * @param initialBalance How much available cash the user has in their wallet.
     */
    public Investor(final ZonkyProxy api, final BigDecimal initialBalance) {
        this.api = api;
        this.balance = initialBalance;
        Investor.LOGGER.info("Starting account balance: {} CZK.", this.balance);
    }

    /**
     * Prepares a list of loans that are suitable for investment by asking the strategy. Then goes over that list one
     * by one, in the order prescribed by the strategy, and attempts to invest into these loans. The first such
     * investment operation that succeeds will return.
     *
     * @param strategy The investment strategy to pick loans from the list.
     * @param tracker Current status of the investment algorithm.
     * @param portfolio User's investment portfolio overview.
     * @return The first {@link #actuallyInvest(Recommendation, ZonkyProxy, BigDecimal)} which succeeds, or empty if
     * none have.
     */
    Optional<Investment> investOnce(final InvestmentStrategy strategy, final InvestmentTracker tracker,
                                    final PortfolioOverview portfolio) {
        Investor.LOGGER.debug("Current share of unpaid loans with a given rating: {}.",
                portfolio.getSharesOnInvestment());
        final Optional<Investment> investment = strategy.recommend(tracker.getAvailableLoans(), portfolio).stream()
                .peek(r -> Events.fire(new LoanRecommendedEvent(r)))
                .map(r -> Investor.actuallyInvest(r, this.api, this.balance))
                .flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty())
                .findFirst();
        investment.ifPresent(i -> this.balance = this.balance.subtract(BigDecimal.valueOf(i.getAmount())));
        return investment;
    }

    /**
     * One of the two entry points to the investment API. This takes the strategy, determines suitable loans, and tries
     * to invest in as many of them as the balance allows.
     *
     * @param strategy The investment strategy to use.
     * @param loans Loans that are available on the marketplace.
     * @return Investments made in the session.
     */
    public Collection<Investment> invest(final InvestmentStrategy strategy, final Collection<LoanDescriptor> loans) {
        // make sure we have enough money to invest
        final BigDecimal minimumInvestmentAmount = BigDecimal.valueOf(Defaults.MINIMUM_INVESTMENT_IN_CZK);
        if (balance.compareTo(minimumInvestmentAmount) < 0) {
            return Collections.emptyList(); // no need to do anything else
        }
        // read our investment statistics
        Investor.LOGGER.debug("The following loans are available for robotic investing: {}.",
                Investor.collectionToString(loans, Investor.LOAN_DESCRIPTOR_COMPARATOR, Investor.LOAN_ID_RETRIEVER));
        final Statistics stats = Investor.retrieveStatistics(this.api);
        Investor.LOGGER.debug("The sum total of principal remaining on active loans: {} CZK.",
                stats.getCurrentOverview().getPrincipalLeft());
        // figure out which loans we can still put money into
        final InvestmentTracker tracker = new InvestmentTracker(loans);
        tracker.registerExistingInvestments(Investor.retrieveInvestmentsRepresentedByBlockedAmounts(this.api));
        // invest the money
        this.runInvestmentLoop(strategy, tracker, stats, minimumInvestmentAmount);
        // report
        this.reportOnPortfolioStructure(stats, tracker.getAllInvestments());
        return tracker.getInvestmentsMade();
    }

    private void runInvestmentLoop(final InvestmentStrategy strategy, final InvestmentTracker tracker,
                                   final Statistics stats, final BigDecimal minimumInvestmentAmount) {
        Investor.LOGGER.debug("The following available loans have not yet been invested into: {}.",
                Investor.collectionToString(tracker.getAvailableLoans(), Investor.LOAN_DESCRIPTOR_COMPARATOR,
                        Investor.LOAN_ID_RETRIEVER));
        Events.fire(new StrategyStartedEvent(strategy, tracker.getAvailableLoans(), balance.intValue()));
        do {
            final PortfolioOverview portfolio =
                    PortfolioOverview.calculate(balance, stats, tracker.getAllInvestments());
            final Optional<Investment> investment = this.investOnce(strategy, tracker, portfolio);
            if (!investment.isPresent()) { // there is nothing to invest into; RoboZonky is finished now
                break;
            }
            final Investment i = investment.get();
            tracker.makeInvestment(i);
        } while (balance.compareTo(minimumInvestmentAmount) >= 0);
        Events.fire(new StrategyCompletedEvent(strategy, tracker.getInvestmentsMade(), balance.intValue()));
    }

    private void reportOnPortfolioStructure(final Statistics stats, final Collection<Investment> allInvestments) {
        final PortfolioOverview portfolio = PortfolioOverview.calculate(balance, stats, allInvestments);
        Investor.LOGGER.info("Expected annual yield of portfolio: {} % ({} CZK).",
                portfolio.getRelativeExpectedYield().scaleByPowerOfTen(2).setScale(2, RoundingMode.HALF_EVEN),
                portfolio.getCzkExpectedYield());
        final List<Instalment> instalments = stats.getCashFlow();
        final int currentMonthInstalmentId = instalments.size() - 1 - 3; // contains 3 future months
        if (currentMonthInstalmentId >= 0) { // maybe the history has not been built yet
            Investor.LOGGER.info("Expected instalments: {} CZK this month, {} CZK the next.",
                    instalments.get(currentMonthInstalmentId).getInstalmentAmount(),
                    instalments.get(currentMonthInstalmentId + 1).getInstalmentAmount());
        }
    }

    /**
     * One of the two entry points to the investment API. Takes a particular loan and invests a given amount into it.
     *
     * @param loanId ID of the loan that should be invested into.
     * @param loanAmount Amount in CZK to be invested.
     * @return Present if investment succeeded, empty otherwise.
     */
    public Optional<Investment> invest(final int loanId, final int loanAmount, final TemporalAmount captchaDuration) {
        final Loan l = api.execute(zonky -> zonky.getLoan(loanId));
        final Optional<Recommendation> r = new LoanDescriptor(l, captchaDuration).recommend(loanAmount);
        final Optional<Investment> result = r.map(r2 -> Investor.actuallyInvest(r2, this.api, this.balance))
                .orElse(Optional.empty());
        result.ifPresent(i -> this.balance = balance.subtract(BigDecimal.valueOf(i.getAmount())));
        return result;
    }

    public BigDecimal getBalance() {
        return balance;
    }

}
